package me.gosdev.chatpointsttv.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;

public interface Utils {
    void displayTitle(Player p, String title, String action, String sub, boolean bold, ChatColor titleColor, ChatColor subColor);

    void sendMessage(CommandSender p, BaseComponent[] message);
    void sendMessage(CommandSender p, BaseComponent message);
    void sendMessage(CommandSender p, String message);

    void sendLogToPlayers(String msg);
}
