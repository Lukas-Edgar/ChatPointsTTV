package me.gosdev.chatpointsttv;

import com.github.twitch4j.common.enums.SubscriptionPlan;
import me.gosdev.chatpointsttv.tests.TestCommand;
import me.gosdev.chatpointsttv.twitch.TwitchClient;
import me.gosdev.chatpointsttv.twitch.auth.ImplicitGrantFlow;
import me.gosdev.chatpointsttv.utils.Channel;
import me.gosdev.chatpointsttv.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bukkit.Bukkit.getLogger;

public class CommandController implements TabExecutor {

    private static final Logger log = getLogger();
    public static final String CHATTER_NAME = "<Chatter Name>";
    public static final String STREAMER_CHANNEL = "<Streamer Channel>";
    public static final String RAIDER_NAME = "<Raider Name>";
    public static final String REWARD_NAME = "<Reward Name>";
    Utils utils = ChatPointsTTV.getUtils();
    private final BaseComponent helpMsg = new ComponentBuilder("---------- " + ChatColor.DARK_PURPLE + ChatColor.BOLD + "ChatPointsTTV help" + ChatColor.RESET + " ----------\n" +
            ChatColor.GRAY + "Usage: " + Bukkit.getPluginCommand("twitch").getUsage() + ChatColor.RESET + "\n" +
            ChatColor.LIGHT_PURPLE + "/twitch link [method]: " + ChatColor.RESET + "Use this command to link your Twitch account and enable the plugin.\n" +
            ChatColor.LIGHT_PURPLE + "/twitch unlink: " + ChatColor.RESET + "Use this command to unlink your account and disable the plugin.\n" +
            ChatColor.LIGHT_PURPLE + "/twitch status: " + ChatColor.RESET + "Displays information about the plugin and the Twitch connection.\n" +
            ChatColor.LIGHT_PURPLE + "/twitch reload: " + ChatColor.RESET + "Restarts the plugin and reloads configuration files. You will need to link again your Twitch account.\n" +
            ChatColor.LIGHT_PURPLE + "/twitch test <type> <...>: " + ChatColor.RESET + "Summons a test event.\n" +
            ChatColor.LIGHT_PURPLE + "/twitch help: " + ChatColor.RESET + "Displays this help message.").create()[0];

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ChatPointsTTV plugin = ChatPointsTTV.getInstance();

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;

        } else {
            switch (args[0]) {
                case "link":
                    linkAccount(sender, args, plugin);
                    break;
                case "reload":
                    reload(plugin);
                    break;
                case "help":
                    sendHelpMessage(sender);
                    break;
                case "unlink":
                    unlinkAccount(sender, plugin);
                    break;
                case "status":
                    status(sender, plugin);
                    break;
                case "test":
                    testCommand(sender, args, plugin);
                    break;
                default:
                    sendUnknownCommandMessage(sender, args);
                    break;
            }
        }
        return true;
    }

    private void sendUnknownCommandMessage(CommandSender sender, String[] args) {
        utils.sendMessage(sender, ChatColor.RED + "Unknown command: /twitch " + args[0]);
        sendHelpMessage(sender);
    }

    private void testCommand(CommandSender sender, String[] args, ChatPointsTTV plugin) {
        if (!plugin.getTwitch().isAccountConnected()) {
            utils.sendMessage(sender, ChatColor.RED + "You need to link your account first.");
            return;
        }
        TestCommand.test(sender, args);
    }

    private void unlinkAccount(CommandSender sender, ChatPointsTTV plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getTwitch().joinThread();
            plugin.getTwitch().unlink(sender);
        });
    }

    private void linkAccount(CommandSender sender, String[] args, ChatPointsTTV plugin) {
        if (plugin.getTwitch().isAccountConnected()) {
            utils.sendMessage(sender, "There is an account connected already!\nUnlink it before using another one.");
        }
        if (plugin.isConfigOk()) {
            if (!link(plugin, sender, args.length == 2 ? args[1] : "default")) {
                utils.sendMessage(sender, "Could not connect to Twitch");
            }
        } else {
            utils.sendMessage(sender, "Invalid configuration. Please check your config file.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String arg, String[] args) {
        List<String> availableCommands = new ArrayList<>();

        boolean accountConnected = ChatPointsTTV.getInstance().getTwitch().isAccountConnected();
        if (args.length == 1) {
            addGeneralCommands(accountConnected, availableCommands);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("link")) {
            addLinkCommands(availableCommands);
        } else if (accountConnected && args.length >= 2 && args[0].equalsIgnoreCase("test")) {
            if (args.length == 2) {
                addRewardCommands(availableCommands);
            } else {
                List<String> result = getRewards(args);
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return getCommands(args, availableCommands);
    }

    private List<String> getCommands(String[] args, List<String> availableCommands) {
        List<String> result = new ArrayList<>();
        for (String command : availableCommands) {
            if (command.startsWith(args[args.length - 1])) {
                result.add(command);
            }
        }
        return result;
    }

    private List<String> getRewards(String[] args) {
        String type = args[1].toLowerCase();
        return switch (type) {
            case "channelpoints" -> getChannelPointRewards(args);
            case "cheer" -> getCheerRewards(args);
            case "sub" -> getSubRewards(args, "<Months>");
            case "follow" -> getFollowRewards(args);
            case "subgift" -> getSubRewards(args, "<Amount>");
            case "raid" -> getRaidRewards(args);
            default -> Collections.emptyList();
        };
    }

    private void addGeneralCommands(boolean accountConnected, List<String> available) {
        if (accountConnected) {
            available.add("unlink");
        } else {
            available.add("link");
        }
        available.add("reload");
        available.add("status");
        if (accountConnected) {
            available.add("test");
        }
        available.add("help");
    }

    private List<String> getRaidRewards(String[] args) {
        List<String> result = new ArrayList<>();
        switch (args.length) {
            case 3 -> result.add(RAIDER_NAME);
            case 4 -> result.add(STREAMER_CHANNEL);
            case 5 -> result.add("<Viewers>");
            default -> log.log(Level.INFO, "invalid argument length for raid");
        }
        return result;
    }

    private List<String> getFollowRewards(String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 3) {
            result.add(CHATTER_NAME);
        } else if (args.length == 4) {
            result.add(STREAMER_CHANNEL);
        }
        return result;
    }

    private List<String> getSubRewards(String[] args, String amountText) {
        List<String> result = new ArrayList<>();
        switch (args.length) {
            case 3 -> result.add(CHATTER_NAME);
            case 4 -> result.add(STREAMER_CHANNEL);
            case 5 -> addSubscription(result);
            case 6 -> result.add(amountText);
            default -> log.log(Level.INFO, "invalid argument length for sub");
        }
        return result;
    }

    private List<String> getCheerRewards(String[] args) {
        List<String> result = new ArrayList<>();
        switch (args.length) {
            case 3 -> result.add(CHATTER_NAME);
            case 4 -> result.add(STREAMER_CHANNEL);
            case 5 -> result.add("<Amount>");
            default -> log.log(Level.INFO, "invalid argument length for cheer");

        }
        return result;
    }

    private void addSubscription(List<String> result) {
        for (SubscriptionPlan plan : EnumSet.allOf(SubscriptionPlan.class)) {
            if (plan.equals(SubscriptionPlan.NONE)) {
                continue;
            }
            result.add(plan.name());
        }
    }

    private List<String> getChannelPointRewards(String[] args) {
        List<String> result = new ArrayList<>();
        switch (args.length) {
            case 3 -> result.add("<Redeemer Name>");
            case 4 -> result.add(STREAMER_CHANNEL);
            case 5 -> result.add(REWARD_NAME);
            default -> {
                if (args.length > getRewardNameEnd(args) + 1) {
                    result.add("[User Input]");
                } else {
                    result.add(REWARD_NAME);
                }
            }
        }
        return result;
    }

    private int getRewardNameEnd(String[] args) {
        if (args.length >= 5 && args[4].startsWith("\"")) {
            return findClosingQuotationMarks(args, args.length - 1);
        } else {
            return 4;
        }
    }

    private int findClosingQuotationMarks(String[] args, int rewardNameEnd) {
        for (int i = 5; i < args.length; i++) {
            if (args[i].endsWith("\"")) {
                rewardNameEnd = i;
            }
        }
        return rewardNameEnd;
    }

    private void addRewardCommands(List<String> available) {
        available.add("channelpoints");
        available.add("cheer");
        available.add("sub");
        available.add("follow");
        available.add("subgift");
        available.add("raid");
    }

    private void addLinkCommands(List<String> available) {
        available.add("key");
        available.add("browser");
    }

    private boolean link(ChatPointsTTV plugin, CommandSender p, String method) {
        TwitchClient twitch = plugin.getTwitch();
        boolean success = false;
        boolean useAccessToken = !method.equalsIgnoreCase("browser") && twitch.isCustomCredentialsFound();
        if (useAccessToken) {
            success = twitch.linkToTwitch(p);
        } else {
            if (ImplicitGrantFlow.server.isNotRunning()) {
                CompletableFuture<String> future = ImplicitGrantFlow.getAccessToken(plugin, p, twitch.getClientId());
                future.thenAccept(token -> twitch.linkToTwitch(p, token));
                success = true;
            }
        }
        plugin.addMetricChart(new SimplePie("authentication_method", () -> twitch.isCustomCredentialsFound() ? "OAuth Keys" : "Browser Login"));
        return success;
    }

    private void reload(ChatPointsTTV plugin) {
        plugin.log.info("Reloading ChatPointsTTV...");

        plugin.onDisable();
        plugin.onEnable();
    }

    private void sendHelpMessage(CommandSender p) {
        utils.sendMessage(p, helpMsg);
    }

    private void status(CommandSender p, ChatPointsTTV plugin) {
        TwitchClient twitch = plugin.getTwitch();
        StringBuilder strChannels = new StringBuilder();

        for (Channel channel : twitch.getListenedChannels().values()) {
            ChatColor color = channel.isLive() ? ChatColor.DARK_RED : ChatColor.GRAY;
            strChannels.append(color).append(channel.getChannelUsername()).append(ChatColor.RESET).append(", ");
        }

        boolean hasListenedChannel = !twitch.getListenedChannels().isEmpty();
        strChannels = new StringBuilder(hasListenedChannel ? strChannels.substring(0, strChannels.length() - 2) : "None");

        BaseComponent msg = new ComponentBuilder(
                "---------- " + ChatColor.DARK_PURPLE + ChatColor.BOLD + "ChatPointsTTV status" + ChatColor.RESET + " ----------\n" +
                        ChatColor.LIGHT_PURPLE + "Plugin version: " + ChatColor.RESET + "v" + plugin.getDescription().getVersion() + "\n" +
                        ChatColor.LIGHT_PURPLE + "Connected account: " + ChatColor.RESET + twitch.getConnectedUsername() + "\n" +
                        ChatColor.LIGHT_PURPLE + "Listened channels: " + ChatColor.RESET + strChannels + "\n" +
                        "\n"
        ).create()[0];

        BaseComponent status = new ComponentBuilder(ChatColor.LIGHT_PURPLE + "Connection status: " + (twitch.isAccountConnected() ? ChatColor.GREEN + "" + ChatColor.BOLD + "ACTIVE" : ChatColor.RED + "" + ChatColor.BOLD + "DISCONNECTED")).create()[0];
        status.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to toggle connection").create()));
        status.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, twitch.isAccountConnected() ? "/twitch unlink" : "/twitch link"));

        msg.addExtra(status);

        utils.sendMessage(p, msg);
    }
}
