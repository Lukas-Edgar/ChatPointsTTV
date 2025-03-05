package me.gosdev.chatpointsttv;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.ConfigurationException;
import me.gosdev.chatpointsttv.utils.*;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.gosdev.chatpointsttv.rewards.Rewards;
import me.gosdev.chatpointsttv.twitch.auth.ImplicitGrantFlow;
import me.gosdev.chatpointsttv.twitch.TwitchClient;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ChatPointsTTV extends JavaPlugin {
    private static ChatPointsTTV plugin;
    private TwitchClient twitch;

    private static final Map<String, ChatColor> colors = new HashMap<>();
    private static final Map<String, String> titleStrings = new HashMap<>();
    private boolean shouldMobsGlow;
    private boolean nameSpawnedMobs;
    private boolean configOk = true;
    private AlertMode alertMode;
    private boolean logEvents;


    public final Logger log = getLogger();

    private FileConfiguration config;
    private Metrics metrics;

    public static Map<String, ChatColor> getChatColors() {
        return colors;
    }

    public static Map<String, String> getRedemptionStrings() {
        return titleStrings;
    }

    private static Utils utils;


    public static ChatPointsTTV getInstance() {
        if (plugin == null) {
            plugin = new ChatPointsTTV();
        }
        return plugin;
    }

    public TwitchClient getTwitch() {
        return twitch;
    }


    public static Utils getUtils() {
        if (utils != null) {
            return utils;
        }
        int version = getVersion();
        try {
            if (version >= 12) {
                utils = (Utils) Class.forName(ChatPointsTTV.class.getPackage().getName() + ".Utils.Utils_1_12_R1").getDeclaredConstructor().newInstance();
            } else {
                utils = (Utils) Class.forName(ChatPointsTTV.class.getPackage().getName() + ".Utils.Utils_1_9_R1").getDeclaredConstructor().newInstance();
            }
            return utils;
        } catch (Exception exception) {
            throw new NotFoundException("Error trying to get utils", exception);
        }
    }

    private static int getVersion() {
        return Integer.parseInt(Bukkit.getVersion().split("-")[0].split("\\.")[1]);
    }

    @Override
    public void onLoad() {
        LibraryLoader.loadLibraries(this);
    }

    @Override
    public void onEnable() {
        PluginManager pm = Bukkit.getServer().getPluginManager();

        metrics = new Metrics(this, 22873);

        this.saveDefaultConfig();
        reloadConfig();
        config = getConfig();

        setConfigValues();

        boolean rewardBold = config.getBoolean("REWARD_NAME_BOLD");
        String clientId = config.getString("CUSTOM_CLIENT_ID");
        String accessToken = config.getString("CUSTOM_ACCESS_TOKEN");
        boolean ignoreOfflineStreamers = config.getBoolean("IGNORE_OFFLINE_STREAMERS", false);

        if (clientId != null && accessToken != null) {
            twitch = new TwitchClient(clientId, accessToken, ignoreOfflineStreamers, rewardBold);
        } else {
            twitch = new TwitchClient(ignoreOfflineStreamers, rewardBold);
        }

        CommandController cmdController = new CommandController();
        this.getCommand("twitch").setExecutor(cmdController);
        this.getCommand("twitch").setTabCompleter(cmdController);

        utils.sendMessage(Bukkit.getConsoleSender(), "ChatPointsTTV enabled!");
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission(Permissions.MANAGE.permissionId)) {
                utils.sendMessage(p, new TextComponent("ChatPointsTTV reloaded!"));
            }
        }
        VersionCheck.check();

        pm.registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent player) {
                if (twitch != null && !twitch.isAccountConnected() && player.getPlayer().hasPermission(Permissions.MANAGE.permissionId)) {
                    String msg = ChatColor.LIGHT_PURPLE + "Welcome! Remember to link your Twitch account to enable ChatPointsTTV and start listening to events!\n";
                    BaseComponent btn = new ComponentBuilder(ChatColor.DARK_PURPLE + "" + ChatColor.UNDERLINE + "[Click here to login]").create()[0];

                    btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to run command").create()));
                    btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/twitch link"));

                    utils.sendMessage(player.getPlayer(), new BaseComponent[]{new ComponentBuilder(msg).create()[0], btn});
                }
            }
        }, this);
    }

    private void setConfigValues() {
        config.getConfigurationSection("COLORS").getKeys(false).forEach(key -> colors.put(key, ChatColor.valueOf(config.getConfigurationSection("COLORS").getString(key))));

        config.getConfigurationSection("STRINGS").getKeys(true).forEach(key -> titleStrings.put(key, config.getConfigurationSection("STRINGS").getString(key)));


        shouldMobsGlow = config.getBoolean("MOB_GLOW", false);
        alertMode = AlertMode.valueOf(config.getString("INGAME_ALERTS").toUpperCase());
        nameSpawnedMobs = config.getBoolean("DISPLAY_NAME_ON_MOB", true);
        logEvents = plugin.config.getBoolean("LOG_EVENTS");
    }

    @Override
    public void onDisable() {
        if (twitch != null && twitch.isAccountConnected()) {
            twitch.unlink(Bukkit.getConsoleSender());
        }

        ImplicitGrantFlow.server.stop();

        Rewards.resetRewards();

        HandlerList.unregisterAll(this);
    }

    public Object getRewardConfig(RewardType type) {
        return config.get(type.toString().toUpperCase() + "_REWARDS");
    }

    public void addMetricChart(CustomChart chart) {
        this.metrics.addCustomChart(chart);
    }


    public boolean isShouldMobsGlow() {
        return shouldMobsGlow;
    }


    public boolean isNameSpawnedMobs() {
        return nameSpawnedMobs;
    }


    public boolean isConfigOk() {
        return configOk;
    }


    public AlertMode getAlertMode() {
        return alertMode;
    }


    public boolean isLogEvents() {
        return logEvents;
    }
}
