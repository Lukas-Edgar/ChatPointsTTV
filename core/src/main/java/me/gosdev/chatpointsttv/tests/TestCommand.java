package me.gosdev.chatpointsttv.tests;

import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import me.gosdev.chatpointsttv.ChatPointsTTV;
import me.gosdev.chatpointsttv.utils.MalformedArgumentsException;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class TestCommand {

    private TestCommand() {}
    private static final EventManager eventManager = ChatPointsTTV.getInstance().getTwitch().getClient().getEventManager();

    public static void test(CommandSender sender, String[] cmdInput) {
        if (cmdInput.length < 2) {
            ChatPointsTTV.getUtils().sendMessage(sender, ChatColor.RED + "Usage: /twitch test <type> ...");
            return;
        }

        ArrayList<String> args = getArguments(cmdInput);

        try {
            startEvent(args);
        } catch (MalformedArgumentsException e) {
            ChatPointsTTV.getUtils().sendMessage(sender, e.getMessage());
        }


        ChatPointsTTV.getUtils().sendMessage(sender, ChatColor.GREEN + "Test event sent!");
    }

    private static ArrayList<String> getArguments(String[] cmdInput) { //TODO: refactor
        ArrayList<String> args = new ArrayList<>();
        for (int i = 0; i < cmdInput.length; i++) {
            String arg = cmdInput[i];
            // Check if the argument starts with a quote and does not end with an escaped quote
            if (arg.startsWith("\"") && !arg.endsWith("\\\"")) {
                StringBuilder sb = new StringBuilder(arg.substring(1));
                // Continue appending arguments until the closing quote is found
                while (i + 1 < cmdInput.length && !(cmdInput[i + 1].endsWith("\"") && !cmdInput[i + 1].endsWith("\\\""))) {
                    sb.append(" ").append(cmdInput[++i]);
                }
                // Append the last part of the quoted argument
                if (i + 1 < cmdInput.length) {
                    sb.append(" ").append(cmdInput[++i], 0, cmdInput[i].length() - 1);
                }
                // Add the complete quoted argument to the args list
                args.add(sb.toString().replace("\\\"", "\""));
            } else {
                // Add the argument to the args list, replacing escaped quotes
                args.add(arg.replace("\\\"", "\""));
            }
        }
        return args;
    }

    private static void startEvent(ArrayList<String> args) {
        switch (args.get(1).toLowerCase()) {
            case "channelpoints":
                startChannelPointsEvent(args);
                break;
            case "follow":
                startFollowEvent(args);
                break;
            case "cheer":
                startCheerEvent(args);
                break;
            case "sub":
                startSubEvent(args);
                break;
            case "subgift":
                startSubGiftEvent(args);
                break;
            case "raid":
                startRaidEvent(args);
                break;
            default:
                throw new MalformedArgumentsException(ChatColor.RED + "Unknown test type: " + args.get(1));
        }
    }

    private static void startRaidEvent(ArrayList<String> args) {
        if (args.size() < 5) {
            throw new MalformedArgumentsException(ChatColor.RED + "Usage: /twitch test raid <raider> <channel> <viewer count>");
        }

        String raidUser = args.get(2);
        String raidChannel = args.get(3);
        int raidViewers;

        try {
            raidViewers = Integer.parseInt(args.get(4));
        } catch (NumberFormatException e) {
            throw new MalformedArgumentsException(ChatColor.RED + "Invalid viewer amount: " + args.get(4));
        }

        eventManager.publish(EventTest.raidReward(raidChannel, raidUser, raidViewers));
    }

    private static void startSubGiftEvent(ArrayList<String> args) {
        if (args.size() < 6) {
            throw new MalformedArgumentsException(ChatColor.RED + "Usage: /twitch test subgift <user> <channel> <tier> <amount>");
        }

        String giftChatter = args.get(2);
        String giftChannel = args.get(3);
        try {
            int giftAmount = Integer.parseInt(args.get(5));
            SubscriptionPlan giftTier = SubscriptionPlan.valueOf(args.get(4).toUpperCase());
            eventManager.publish(EventTest.subGiftEvent(giftChannel, giftChatter, giftTier, giftAmount));
        } catch (NumberFormatException e) {
            throw new MalformedArgumentsException(ChatColor.RED + "Invalid gifted subs amount: " + args.get(5));
        } catch (IllegalArgumentException e) {
            throw new MalformedArgumentsException(ChatColor.RED + "Invalid subscription tier: " + args.get(4));
        }
    }

    private static void startSubEvent(ArrayList<String> args) {
        if (args.size() < 6) {
            throw new MalformedArgumentsException(ChatColor.RED + "Usage: /twitch test sub <user> <channel> <plan> <months>");
        }
        String subUser = args.get(2);
        String subChannel = args.get(3);

        try {
            SubscriptionPlan subTier = SubscriptionPlan.valueOf(args.get(4).toUpperCase());
            int subMonths = Integer.parseInt(args.get(5));
            eventManager.publish(EventTest.subEvent(subChannel, subUser, subTier, subMonths));
        } catch (NumberFormatException e) {
            throw new MalformedArgumentsException(ChatColor.RED + "Invalid amount of months: " + args.get(5));
        } catch (IllegalArgumentException e) {
            throw new MalformedArgumentsException(ChatColor.RED + "Invalid subscription tier: " + args.get(4));
        }


    }

    private static void startCheerEvent(ArrayList<String> args) {
        if (args.size() < 5) {
            throw new MalformedArgumentsException(ChatColor.RED + "Usage: /twitch test cheer <user> <channel> <amount>");
        }

        String cheerUser = args.get(2);
        String cheerChannel = args.get(3);

        try {
            int cheerAmount = Integer.parseInt(args.get(4));
            eventManager.publish(EventTest.cheerEvent(cheerChannel, cheerUser, cheerAmount));
        } catch (NumberFormatException e) {
            throw new MalformedArgumentsException(ChatColor.RED + "Invalid cheer amount: " + args.get(4));
        }

    }

    private static void startFollowEvent(ArrayList<String> args) {
        if (args.size() < 4) {
            throw new MalformedArgumentsException(ChatColor.RED + "Usage: /twitch test follow <user> <channel>");
        }

        String followUser = args.get(2);
        String followChannel = args.get(3);

        eventManager.publish(EventTest.followEvent(followChannel, followUser));
    }

    private static void startChannelPointsEvent(ArrayList<String> args) {
        if (args.size() < 5) {
            throw new MalformedArgumentsException(ChatColor.RED + "Usage: /twitch test channelpoints <redeemer> <channel> <reward> [userInput]");
        }

        String pointsChatter = args.get(2);
        String pointsChannel = args.get(3);
        String pointsReward = args.get(4);

        String collect = args.stream().skip(5).collect(Collectors.joining(" "));
        Optional<String> userInput = Optional.of(collect).filter(not(String::isEmpty));

        eventManager.publish(EventTest.channelPointsRedemptionEvent(pointsChannel, pointsChatter, pointsReward, userInput));
    }
}
