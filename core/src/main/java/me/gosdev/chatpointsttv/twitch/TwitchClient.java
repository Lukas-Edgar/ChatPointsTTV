package me.gosdev.chatpointsttv.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.eventsub.domain.chat.NoticeType;
import com.github.twitch4j.eventsub.events.ChannelChatMessageEvent;
import com.github.twitch4j.eventsub.events.ChannelChatNotificationEvent;
import com.github.twitch4j.eventsub.events.ChannelFollowEvent;
import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.github.twitch4j.eventsub.socket.IEventSubSocket;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionSuccessEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import lombok.Getter;
import me.gosdev.chatpointsttv.ChatPointsTTV;
import me.gosdev.chatpointsttv.Permissions;
import me.gosdev.chatpointsttv.rewards.Rewards;
import me.gosdev.chatpointsttv.utils.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bukkit.Bukkit.getLogger;

public class TwitchClient {

    private static final Logger LOG = getLogger();
    private static final String CHANNEL_USERNAME = "CHANNEL_USERNAME";

    private Thread linkThread;

    @Getter
    private boolean customCredentialsFound;
    @Getter
    private boolean ignoreOfflineStreamers;
    @Getter
    private boolean accountConnected = false;
    private OAuth2Credential oAuth;

    private User user;
    private String userId;
    private List<String> chatBlacklist;
    @Getter
    private ITwitchClient client;
    private HashMap<String, Channel> channels;
    private TwitchEventHandler eventHandler;
    private IEventSubSocket eventSocket;
    private EventManager eventManager;

    private final Utils utils = ChatPointsTTV.getUtils();
    private final ChatPointsTTV plugin = ChatPointsTTV.getInstance();
    private final FileConfiguration config = plugin.getConfig();

    private Optional<String> clientId;
    private Optional<String> accessToken;
    public static final String SCOPES = Scopes.join(
            Scopes.CHANNEL_READ_REDEMPTIONS,
            Scopes.CHANNEL_READ_SUBSCRIPTIONS,
            Scopes.USER_READ_MODERATED_CHANNELS,
            Scopes.MODERATOR_READ_FOLLOWERS,
            Scopes.BITS_READ,
            Scopes.USER_READ_CHAT,
            Scopes.CHAT_READ,
            Scopes.USER_BOT,
            Scopes.CHANNEL_BOT
    ).replace(":", "%3A");

    public TwitchClient(String clientId, String accessToken, boolean ignoreOfflineStreamers, boolean rewardBold) {
        setup(Optional.of(clientId), Optional.of(accessToken), ignoreOfflineStreamers, rewardBold);

    }

    public TwitchClient(boolean ignoreOfflineStreamers, boolean rewardBold) {
        setup(Optional.empty(), Optional.empty(), ignoreOfflineStreamers, rewardBold);
    }

    public OAuth2Credential getOAuth() {
        return oAuth;
    }

    private void setup(Optional<String> clientId, Optional<String> accessToken, boolean ignoreOfflineStreamers, boolean rewardBold) {
        this.clientId = clientId;
        this.accessToken = accessToken;
        this.customCredentialsFound = clientId.isPresent() && accessToken.isPresent();
        this.ignoreOfflineStreamers = ignoreOfflineStreamers;
        this.eventHandler = new TwitchEventHandler(rewardBold);
        this.channels = new HashMap<>();
        chatBlacklist = config.getStringList("CHAT_BLACKLIST");
        enableTwitch();
    }

    public String getClientId() {
        return clientId.orElseThrow(() -> new AuthenticationException("Client ID not found"));
    }

    public String getConnectedUsername() {
        return accountConnected ? user.getLogin() : "Not Linked";
    }

    public Map<String, Channel> getListenedChannels() {
        return channels;
    }

    public void enableTwitch() {
        String channelAllowedChars = "^[a-zA-Z0-9_]*$";
        Object configChannel = config.get(CHANNEL_USERNAME);

        if (configChannel instanceof String channel) {
            addSingleChannel(channel, channelAllowedChars);
        } else if (configChannel instanceof List<?> channelList) {
            addChannelList(channelList, channelAllowedChars);
        } else {
            LOG.log(Level.WARNING, "Cannot read channel. Config file may be not set up or invalid.");
        }

        if (customCredentialsFound && config.getBoolean("AUTO_LINK_CUSTOM")) {
            plugin.addMetricChart(new SimplePie("authentication_method", () -> "Twitch Auto-Link (Key)"));
            linkToTwitch(Bukkit.getConsoleSender());
        }
    }

    private void addChannelList(List<?> channelList, String channelAllowedChars) {
        if (channelList.isEmpty()) {
            LOG.log(Level.WARNING, "Channel list is blank.");
        } else {
            config.getStringList(CHANNEL_USERNAME).forEach(channel -> addSingleChannel(channel, channelAllowedChars));
        }
    }

    private void addSingleChannel(String channel, String channelAllowedChars) {
        if (channel.isEmpty()) {
            LOG.log(Level.WARNING, "Channel field is blank.");
        } else if (!channel.matches(channelAllowedChars)) {
            String message = String.format("Invalid channel name: %s", channel);
            LOG.log(Level.WARNING, message);
        } else {
            channels.put(channel, null);
        }
    }

    public boolean linkToTwitch(CommandSender sender, String token) {
        linkThread = new Thread(() -> {
            String id = this.clientId.orElseThrow(() -> new AuthenticationException("Client ID not set."));
            if (token == null || token.isEmpty()) {
                throw new AuthenticationException("Access Token not set");
            }

            utils.sendMessage(sender, "Logging in...");

            oAuth = new OAuth2Credential(id, token);

            client = TwitchClientBuilder.builder()
                    .withDefaultAuthToken(oAuth)
                    .withEnableChat(true)
                    .withEnableHelix(true)
                    .withEnablePubSub(true)
                    .withEnableEventSocket(true)
                    .withDefaultEventHandler(SimpleEventHandler.class)
                    .build();

            user = client.getHelix().getUsers(token, null, null).execute().getUsers().get(0);

            utils.sendMessage(Bukkit.getConsoleSender(), "Logged in as: " + user.getDisplayName());

            channels.replaceAll((channelName, channel) -> new Channel(channelName, TwitchUtils.getUserId(channelName), TwitchUtils.isLive(token, channelName)));

            userId = new TwitchIdentityProvider(null, null, null).getAdditionalCredentialInformation(oAuth).map(OAuth2Credential::getUserId).orElse(null);

            eventSocket = client.getEventSocket();
            eventManager = client.getEventManager();


            CountDownLatch latch = addEvents();


            if (config.getList(CHANNEL_USERNAME) == null) {
                subscribeToEvents(latch, config.getString(CHANNEL_USERNAME));
            } else {
                for (String channel : config.getStringList(CHANNEL_USERNAME)) {
                    subscribeToEvents(latch, channel);
                }
            }

            try {
                client.getEventManager().getEventHandler(SimpleEventHandler.class).registerListener(eventHandler);
                latch.await();
            } catch (InterruptedException e) {
                LOG.warning("Failed to bind events.");
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                }
            }

            utils.sendMessage(sender, "Twitch client has started successfully!");
            accountConnected = true;
        });

        linkThread.start();
        linkThread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            linkThread.interrupt();
            LOG.warning(e.getMessage());
            utils.sendMessage(sender, ChatColor.RED + "Account linking failed!");
            accountConnected = false;
            unlink(Bukkit.getConsoleSender());
        });
        return accountConnected;
    }

    private CountDownLatch addEvents() {
        int eventCount = 0;
        eventManager.onEvent(ChannelGoLiveEvent.class, (ChannelGoLiveEvent event) -> updateChannelStatus(event.getChannel().getName(), true));
        eventManager.onEvent(ChannelGoOfflineEvent.class, (ChannelGoOfflineEvent event) -> updateChannelStatus(event.getChannel().getName(), false));

        if (Rewards.getRewards(RewardType.CHANNEL_POINTS) != null) {
            eventManager.onEvent(RewardRedeemedEvent.class, (RewardRedeemedEvent event) -> eventHandler.onChannelPointsRedemption(event));
        }
        if (Rewards.getRewards(RewardType.FOLLOW) != null) {
            eventCount++;
            eventManager.onEvent(ChannelFollowEvent.class, (ChannelFollowEvent event) -> eventHandler.onFollow(event));
        }
        if (Rewards.getRewards(RewardType.CHEER) != null) {
            eventCount++;
            eventManager.onEvent(ChannelChatMessageEvent.class, (ChannelChatMessageEvent event) -> eventHandler.onCheer(event));
        }
        if (Rewards.getRewards(RewardType.SUB) != null || Rewards.getRewards(RewardType.GIFT) != null) {
            eventCount++;
            addSubEvent();
        }
        if (Rewards.getRewards(RewardType.RAID) != null) {
            eventCount++;
            eventManager.onEvent(ChannelRaidEvent.class, (ChannelRaidEvent event) -> eventHandler.onRaid(event));
        }
        if (config.getBoolean("SHOW_CHAT")) {
            addChatEvent();
        }

        CountDownLatch latch = new CountDownLatch(getListenedChannels().size() * eventCount);

        eventManager.onEvent(EventSocketSubscriptionSuccessEvent.class, e -> latch.countDown());
        eventManager.onEvent(EventSocketSubscriptionFailureEvent.class, e -> latch.countDown());
        return latch;
    }

    private void addChatEvent() {
        eventManager.onEvent(ChannelMessageEvent.class, event -> {
            if (ignoreOfflineStreamers && !getListenedChannels().get(event.getChannel().getName().toLowerCase()).isLive()) {
                return;
            }
            if (!chatBlacklist.contains(event.getUser().getName())) {

                Color color = new Color(ColorUtils.hexToRgb(event.getMessageEvent().getUserChatColor().orElse("#FF0000")));
                ChatColor mcColor = ColorUtils.getClosestChatColor(color);
                BaseComponent[] components = new BaseComponent[]{
                        new ComponentBuilder(mcColor + event.getMessageEvent().getUserDisplayName().orElse("unknown user") + ": ").create()[0],
                        new ComponentBuilder(event.getMessage()).create()[0]
                };
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission(Permissions.BROADCAST.permissionId)) {
                        utils.sendMessage(player, components);
                    }
                }
            }
        });
    }

    private void addSubEvent() {
        eventManager.onEvent(ChannelChatNotificationEvent.class, (ChannelChatNotificationEvent event) -> {
            if (event.getNoticeType() == NoticeType.SUB || event.getNoticeType() == NoticeType.RESUB) {
                eventHandler.onSub(event);
            } else if (event.getNoticeType() == NoticeType.COMMUNITY_SUB_GIFT) {
                eventHandler.onSubGift(event);
            }
        });
    }

    private void updateChannelStatus(String channelName, boolean live) {
        for (Channel channel : channels.values()) {
            if (channel.getChannelName().equalsIgnoreCase(channelName))
                channel.setLive(live);
        }
    }

    public boolean linkToTwitch(CommandSender p) {
        return linkToTwitch(p, this.accessToken.orElse(""));
    }

    public void subscribeToEvents(CountDownLatch latch, String channel) {
        String channelId = TwitchUtils.getUserId(channel);
        utils.sendMessage(Bukkit.getConsoleSender(), "Listening to " + channel + "'s events...");

        if (Rewards.getRewards(RewardType.CHANNEL_POINTS) != null) {
            client.getPubSub().listenForChannelPointsRedemptionEvents(null, channelId);
        }

        if (Rewards.getRewards(RewardType.FOLLOW) != null) {
            if (TwitchUtils.getModeratedChannelIDs(oAuth.getAccessToken(), userId).contains(channelId) || userId.equals(channelId)) { // If account is the streamer or a mod (need to have mod permissions on the channel)
                eventSocket.register(SubscriptionTypes.CHANNEL_FOLLOW_V2.prepareSubscription(b -> b.moderatorUserId(userId).broadcasterUserId(channelId).build(), null));
            } else {
                String message = String.format("%s: Follow events cannot be listened to on unauthorised channels.", channel);
                LOG.log(Level.WARNING, message);
                latch.countDown();
            }
        }

        if (Rewards.getRewards(RewardType.CHEER) != null) {
            eventSocket.register(SubscriptionTypes.CHANNEL_CHAT_MESSAGE.prepareSubscription(b -> b.broadcasterUserId(channelId).userId(userId).build(), null));
        }

        if (Rewards.getRewards(RewardType.SUB) != null || Rewards.getRewards(RewardType.GIFT) != null) {
            eventSocket.register(SubscriptionTypes.CHANNEL_CHAT_NOTIFICATION.prepareSubscription(b -> b.broadcasterUserId(channelId).userId(userId).build(), null));
        }

        if (Rewards.getRewards(RewardType.RAID) != null) {
            eventSocket.register(SubscriptionTypes.CHANNEL_RAID.prepareSubscription(b -> b.toBroadcasterUserId(channelId).build(), null));
        }
        client.getChat().joinChannel(channel);
    }

    public void unlink(CommandSender p) {
        if (!accountConnected) {
            utils.sendMessage(p, new TextComponent(ChatColor.RED + "There is no connected account."));
            return;
        }
        try {
            if (!linkThread.isInterrupted()) {
                linkThread.join();
            }
            client.getEventSocket().close();
            client.close();
        } catch (Exception e) {
            if (Thread.interrupted()) {
                linkThread.interrupt();
            }
            LOG.log(Level.WARNING, "Error while disabling ChatPointsTTV.", e);
            return;
        }

        client = null;
        eventHandler = null;
        eventSocket = null;
        eventManager = null;
        accountConnected = false;
        oAuth = null;

        utils.sendMessage(p, ChatColor.GREEN + "Account disconnected!");
    }

    public void joinThread() {
        try {
            this.linkThread.join();
        } catch (InterruptedException e) {
            if (Thread.interrupted()) {
                linkThread.interrupt();
            }
        }
    }

    public void close() {
        if (this.client != null) {
            this.client.close();
        }
    }
}
