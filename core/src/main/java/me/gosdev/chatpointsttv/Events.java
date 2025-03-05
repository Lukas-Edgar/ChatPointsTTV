package me.gosdev.chatpointsttv;

import me.gosdev.chatpointsttv.utils.AlertMode;
import me.gosdev.chatpointsttv.utils.SpawnRunnable;
import me.gosdev.chatpointsttv.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Events {

    private Events() {}
    static ChatPointsTTV plugin = ChatPointsTTV.getInstance();
    static Logger log = plugin.log;

    static Utils utils = ChatPointsTTV.getUtils();

    public static void showIngameAlert(String user, String action, String rewardName, ChatColor titleColor, ChatColor userColor, Boolean isBold) {

        AlertMode alertMode = ChatPointsTTV.getInstance().getAlertMode();
        if (alertMode == AlertMode.NONE) {
            return;
        }

        ComponentBuilder builder = new ComponentBuilder(user).color(userColor).bold(isBold);
        builder.append(" " + action).color(titleColor);
        builder.append(" " + rewardName).color(userColor);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(Permissions.BROADCAST.permissionId)) {
                continue;
            }
            if (alertMode == AlertMode.CHAT || alertMode == AlertMode.ALL) {
                utils.sendMessage(player, builder.create());
            }
            if (alertMode == AlertMode.TITLE || alertMode == AlertMode.ALL) {
                utils.displayTitle(player.getPlayer(), user, action, rewardName, isBold, userColor, titleColor);
            }
        }

    }

    public static void runAction(String action, String args, String user) {
        args = args.replace("{USER}", user);
        List<String> command = Arrays.asList(args.split(" "));
        switch (action.toUpperCase()) {
            case "SPAWN" -> actionSpawn(user, command);
            case "RUN" -> actionRun(command);
            case "GIVE" -> actionGive(command);
            case "TNT" -> actionTnt(command);
            default -> log.log(Level.WARNING, () -> String.format("No such action: %s", action));
        }
    }

    private static void actionTnt(List<String> command) {
        Bukkit.getOnlinePlayers()
                .stream()
                .filter(player -> player.hasPermission(Permissions.TARGET.permissionId))
                .forEach(player -> spawnTnt(command, player));
    }

    private static void spawnTnt(List<String> command, Player player) {
        SpawnRunnable tntRunnable = new SpawnRunnable();
        tntRunnable.setEntity(EntityType.PRIMED_TNT);
        tntRunnable.setAmount(Integer.parseInt(command.get(0)));
        try {
            tntRunnable.setExplosionTime(Integer.valueOf(command.get(1)));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            log.log(Level.INFO, "ExplosionTime could not be found in command.");
        }

        tntRunnable.setPlayer(player);
        tntRunnable.setId(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, tntRunnable, 0, 2));
    }

    private static void actionGive(List<String> command) {
        try {
            ItemStack item = getItem(command);
            if (targetNotFound(command)) {
                return;
            }
            getTargetPlayers(command.size() >= 3, command.get(2))
                    .forEach(player -> player.getInventory().addItem(item));
        } catch (NumberFormatException e) {
            log.log(Level.WARNING, String.format("Invalid amount of entities: %s", command.get(1)));
        }
    }

    private static boolean targetNotFound(List<String> command) {
        if (command.size() >= 3) {
            String targetName = command.get(2);
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                log.log(Level.WARNING, () -> String.format("Couldn't find player %s.", targetName));
                return true;
            }
        }
        return false;
    }

    private static ItemStack getItem(List<String> command) {
        if (!EnumUtils.isValidEnum(Material.class, command.get(0))) {
            log.warning(() -> String.format("Item %s does not exist.", command.get(0)));
        }
        int amount = getAmount(command);
        return new ItemStack(Material.valueOf(command.get(0).toUpperCase()), amount);
    }

    private static int getAmount(List<String> command) {
        if (command.size() >= 2) {
            return Integer.parseInt(command.get(1));
        } else {
            return 1;
        }
    }

    private static void actionRun(List<String> cmd) {
        String runAs = cmd.remove(0);

        final String command = cmd
                .stream()
                .map(c -> " " + c)
                .collect(Collectors.joining())
                .trim()
                .replace("/", "");

        if (runAs.equalsIgnoreCase("CONSOLE")) {
            runConsoleCommand(Bukkit.getServer().getConsoleSender(), command);
        } else if (runAs.equalsIgnoreCase("TARGET")) {
            runTargetCommand(command);
        } else {
            log.log(Level.WARNING, () -> String.format("Invalid parameter: %s", runAs));
        }
    }

    private static void runTargetCommand(String command) {
        plugin.getServer().getOnlinePlayers()
                .stream()
                .filter(p -> p.hasPermission(Permissions.TARGET.permissionId))
                .findFirst()
                .ifPresentOrElse(
                        player -> runConsoleCommand(player, command),
                        () -> log.log(Level.WARNING, "Couldn't find any target players!")
                );
    }

    private static void runConsoleCommand(CommandSender sender, String command) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(sender, command);
            }
        }.runTask(plugin);
    }

    private static void actionSpawn(String user, List<String> command) {
        try {
            boolean isValidEntity = EnumUtils.isValidEnum(EntityType.class, command.get(0).toUpperCase());
            if (!isValidEntity) {
                log.log(Level.WARNING, () -> String.format("Entity %s does not exist", command.get(0)));
                return;
            }
            if (targetNotFound(command)) {
                return;
            }
            SpawnRunnable entityRunnable = new SpawnRunnable();
            entityRunnable.setAmount(getAmount(command));
            entityRunnable.setEntityName(user);
            getTargetPlayers(command.size() >= 3, command.get(2))
                    .forEach(player -> {
                        entityRunnable.setPlayer(player);
                        entityRunnable.setId(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, entityRunnable, 0, 0));
                    });
        } catch (NumberFormatException e) {
            log.log(Level.WARNING, String.format("Invalid amount of entities: %s", command.get(1)));
        }
    }
    
    private static List<Player> getTargetPlayers(boolean isTargetingAPlayer, String targetName) {
        List<Player> players = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isTargetingAPlayer) {
                if (player.getName().equalsIgnoreCase(targetName)) {
                    players.add(player);
                }
            } else if (player.hasPermission(Permissions.TARGET.permissionId)) {
                players.add(player);
            }
        }
        return players;
    }
}