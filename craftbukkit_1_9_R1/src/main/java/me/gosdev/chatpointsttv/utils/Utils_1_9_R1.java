package me.gosdev.chatpointsttv.utils;

import me.gosdev.chatpointsttv.Permissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Utils_1_9_R1 implements Utils {    @SuppressWarnings("deprecation")
    @Override
    public void displayTitle(Player p, String title, String action, String sub, boolean bold, ChatColor titleColor, ChatColor subColor) {
        if (bold) {
            p.sendTitle(titleColor + title, action + " " + subColor + ChatColor.BOLD + sub);
        } else {
            p.sendTitle(titleColor + title, action + " " + subColor + sub);
        }
    }
    
    @Override
    public void sendMessage(CommandSender p, BaseComponent[] message) {
        Bukkit.getPlayer(p.getName()).spigot().sendMessage(message);
    }

    @Override
    public void sendMessage(CommandSender p, BaseComponent message) {
        Bukkit.getPlayer(p.getName()).spigot().sendMessage(message);
    }
    @Override
    public void sendMessage(CommandSender p, String message) {
        p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD +"[ChatPointsTTV] " + ChatColor.RESET + message);
    }

    @Override
    public void sendLogToPlayers(String msg) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(Permissions.MANAGE.permissionId)) {
                p.sendRawMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[ChatPointsTTV] " + ChatColor.RESET + msg);
            }
        }
    }
}
