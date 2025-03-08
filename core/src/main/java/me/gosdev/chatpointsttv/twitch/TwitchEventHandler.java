package me.gosdev.chatpointsttv.twitch;

import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.eventsub.domain.chat.CommunitySubGift;
import com.github.twitch4j.eventsub.domain.chat.Resubscription;
import com.github.twitch4j.eventsub.domain.chat.Subscription;
import com.github.twitch4j.eventsub.events.ChannelChatMessageEvent;
import com.github.twitch4j.eventsub.events.ChannelChatNotificationEvent;
import com.github.twitch4j.eventsub.events.ChannelFollowEvent;
import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.github.twitch4j.pubsub.domain.ChannelPointsRedemption;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import me.gosdev.chatpointsttv.ChatPointsTTV;
import me.gosdev.chatpointsttv.Events;
import me.gosdev.chatpointsttv.rewards.Reward;
import me.gosdev.chatpointsttv.rewards.Rewards;
import me.gosdev.chatpointsttv.utils.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bukkit.Bukkit.getLogger;

public class TwitchEventHandler {

    private final Logger log = getLogger();

    public static final String AMOUNT = "{AMOUNT}";
    ChatPointsTTV plugin = ChatPointsTTV.getInstance();
    Utils utils = ChatPointsTTV.getUtils();

    private final boolean rewardBold;
    ChatColor actionColor = ChatPointsTTV.getChatColors().get("ACTION_COLOR");
    ChatColor userCo = ChatPointsTTV.getChatColors().get("USER_COLOR");

    public TwitchEventHandler(boolean rewardBold) {
        this.rewardBold = rewardBold;
    }

    public void onChannelPointsRedemption(RewardRedeemedEvent event) {
        if (plugin.isLogEvents()) {
            utils.sendMessage(Bukkit.getConsoleSender(), event.getRedemption().getUser().getDisplayName() + " has redeemed " + event.getRedemption().getReward().getTitle() + " in " + TwitchUtils.getUsername(event.getRedemption().getChannelId()));
        }
        if (plugin.getTwitch().isIgnoreOfflineStreamers()) {
            for (Channel channel : plugin.getTwitch().getListenedChannels().values()) {
                if (channel.getChannelId().equals(event.getRedemption().getChannelId()) && !channel.isLive()) return; // Return if channel matches and it's offline.
            }
        }
        ChannelPointsRedemption redemption = event.getRedemption();

        String customString = ChatPointsTTV.getRedemptionStrings().get("REDEEMED_STRING");
        Events.showIngameAlert(redemption.getUser().getDisplayName(), customString, redemption.getReward().getTitle(), actionColor, userCo, rewardBold);

        for (Reward reward : Rewards.getRewards(RewardType.CHANNEL_POINTS)) {
            boolean wrongEventTitle = !reward.getEvent().equalsIgnoreCase(redemption.getReward().getTitle());
            if (wrongEventTitle || isWrongTargetId(redemption.getChannelId(), reward)) {
                continue;
            }
            String userInput = redemption.getUserInput();
            if (userInput == null) {
                userInput = "";
            }
            runCommands(reward.getCommands(), "{TEXT}", userInput, redemption.getUser().getDisplayName());
            return;
        }
    }

    public void onFollow(ChannelFollowEvent event) {
        if (plugin.isLogEvents()) {
            utils.sendMessage(Bukkit.getConsoleSender(), event.getUserName() + " started following " + event.getBroadcasterUserName());
        }
        if (shouldbeIgnored(event.getBroadcasterUserId())){
            return;
        }
        showIngameAlert(event.getUserName(), "FOLLOWED_STRING", "");
        for (Reward reward : Rewards.getRewards(RewardType.FOLLOW)) {
            if (isWrongTargetId(event.getBroadcasterUserId(), reward)) {
                continue;
            }

            runCommands(reward.getCommands(), "", "", event.getUserName());
            return;
        }
    }

    public void onCheer(ChannelChatMessageEvent event) {
        if (event.getCheer() == null) {
            return;
        }
        if (plugin.isLogEvents()) {
            utils.sendMessage(Bukkit.getConsoleSender(), event.getChatterUserName() + " cheered " + event.getCheer().getBits() + " bits to " + event.getBroadcasterUserName() + "!");
        }
        if (shouldbeIgnored(event.getBroadcasterUserId())){
            return;
        }
        int amount = event.getCheer().getBits();
        showIngameAlert(event.getChatterUserName(), "CHEERED_STRING", amount + " bits");

        List<Reward> rewards = Rewards.getRewards(RewardType.CHEER);
        for (Reward reward : rewards) {
            if (isWrongTargetId(event.getBroadcasterUserId(), reward)) {
                continue;
            }
            try {
                if (amount >= Integer.parseInt(reward.getEvent())) {
                    runCommands(reward.getCommands(), AMOUNT, String.valueOf(amount), event.getChatterUserName());
                    return;
                }

            } catch (NumberFormatException e) {
                String message = String.format("Invalid cheer amount: %s", reward.getEvent());
                plugin.log.log(Level.WARNING, message);
                return;
            }
        }
    }

    public void onSub(ChannelChatNotificationEvent event) {
        SubscriptionPlan tier = getSubscriptionTier(event);
        if (tier == null) {
            return;
        }

        if (plugin.isLogEvents()) {
            utils.sendMessage(Bukkit.getConsoleSender(), event.getChatterUserName() + " has subscribed to " + event.getBroadcasterUserName() + " with a " + TwitchUtils.planToString(tier) + " sub!");
        }
        if (shouldbeIgnored(event.getBroadcasterUserId())) {
            return;
        }

        showIngameAlert(event.getChatterUserName(), "SUB_STRING", TwitchUtils.planToString(tier) + " sub");

        for (Reward reward : Rewards.getRewards(RewardType.SUB)) {
            if (isWrongTargetId(event.getBroadcasterUserId(), reward)) {
                continue;
            }

            if (reward.getEvent().equals(TwitchUtils.planToConfig(tier))) {
                runCommands(reward.getCommands(), "", "", event.getChatterUserName());
                return;
            }
        }
    }

    public void onSubGift(ChannelChatNotificationEvent event) {
        CommunitySubGift communitySubGift = event.getCommunitySubGift();
        if (communitySubGift == null) {
            return;
        }
        int amount = communitySubGift.getTotal();
        String tier = TwitchUtils.planToString(communitySubGift.getSubTier());

        if (plugin.isLogEvents()) {
            utils.sendMessage(Bukkit.getConsoleSender(), event.getChatterUserName() + " has gifted " + amount  + " " + tier + " subs in " + event.getBroadcasterUserName() + "'s' channel!");
        }
        if(shouldbeIgnored(event.getBroadcasterUserId())) {
            return;
        }
        
        showIngameAlert(event.getChatterUserName(), "GIFT_STRING", tier);

        for (Reward reward : Rewards.getRewards(RewardType.GIFT)) {
            if (isWrongTargetId(event.getBroadcasterUserId(), reward)) {
                continue;
            }
            if (amount >= Integer.parseInt(reward.getEvent())) {
                runCommands(reward.getCommands(), AMOUNT, String.valueOf(amount), event.getChatterUserName());
            }
        }
    }

    public void onRaid(ChannelRaidEvent event) {
        String raiderName = event.getFromBroadcasterUserName();
        Integer amount = event.getViewers();

        if (plugin.isLogEvents()) {
            utils.sendMessage(Bukkit.getConsoleSender(), raiderName + " has raided " + event.getToBroadcasterUserName()  + " with a viewer count of " + amount + "!");
        }
        if(shouldbeIgnored(event.getToBroadcasterUserId())) {
            return;
        }
        
        String customString = ChatPointsTTV.getRedemptionStrings().get("RAIDED_STRING").replace("{CHANNEL}", event.getToBroadcasterUserName());
        Events.showIngameAlert(raiderName, customString, amount.toString(), actionColor, userCo, rewardBold);

        for (Reward reward : Rewards.getRewards(RewardType.RAID)) {
            if (isWrongTargetId(event.getToBroadcasterUserId(), reward)) {
                continue;
            }
            if (amount >= Integer.parseInt(reward.getEvent())) {
                runCommands(reward.getCommands(), AMOUNT, String.valueOf(amount), raiderName);
                return;
            }
        }
    }

    private void runCommands(List<String> commands, String toReplace, String replacement, String username) {
        for (String command : commands) {
            String[] commandParts = command.split(" ", 2);

            if (commandParts.length <= 1) {
                String message = String.format("Invalid command: %s", commandParts[0]);
                log.log(Level.WARNING, message);
                continue;
            }

            Events.runAction(commandParts[0], commandParts[1].replace(toReplace, replacement), username);
        }
    }

    private boolean isWrongTargetId(String id, Reward reward) {
        return !reward.getChannelId().equals(id) && !reward.getChannelId().equals(Rewards.EVERYONE);
    }

    private SubscriptionPlan getSubscriptionTier(ChannelChatNotificationEvent event) {

        switch (event.getNoticeType()) {
            case SUB:
                Subscription sub = event.getSub();
                if (sub == null) {
                    plugin.log.log(Level.WARNING, "Could not find subscription for event type");
                    return null;
                }
                if (Boolean.TRUE.equals(sub.isPrime())) {
                    return SubscriptionPlan.TWITCH_PRIME;
                } else {
                    return sub.getSubTier();
                }
            case RESUB:
                Resubscription resub = event.getResub();
                if (resub == null) {
                    plugin.log.log(Level.WARNING, "Could not find subscription for event type");
                    return null;
                }
                if (Boolean.TRUE.equals(resub.isPrime())) {
                    return SubscriptionPlan.TWITCH_PRIME;
                }
                else {
                    return resub.getSubTier();
                }
            default:
                plugin.log.log(Level.WARNING, "Couldn't fetch sub type!");
                return null;
        }
    }

    private void showIngameAlert(String userName, String redemptionName, String rewardName) {
        String customString = ChatPointsTTV.getRedemptionStrings().get(redemptionName);
        Events.showIngameAlert(userName, customString, rewardName, actionColor, userCo, rewardBold);
    }

    private boolean shouldbeIgnored(String broadcasterUserId) {
        if (plugin.getTwitch().isIgnoreOfflineStreamers()) {
            for (Channel channel : plugin.getTwitch().getListenedChannels().values()) {
                if (channel.getChannelId().equals(broadcasterUserId) && !channel.isLive()) {
                    return true;
                }
            }
        }
        return false;
    }
}
