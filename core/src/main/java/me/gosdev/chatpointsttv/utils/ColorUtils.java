package me.gosdev.chatpointsttv.utils;

import java.awt.Color;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.md_5.bungee.api.ChatColor;

/**
 * Mapping of Spigot ChatColor to appropriate Java Color
 */
public class ColorUtils {

    private static final Map<ChatColor, Color> COLOR_MAPPINGS = ImmutableMap.<ChatColor, Color>builder()
        .put(ChatColor.BLACK, new Color(0, 0, 0))
        .put(ChatColor.DARK_BLUE, new Color(0, 0, 170))
        .put(ChatColor.DARK_GREEN, new Color(0, 170, 0))
        .put(ChatColor.DARK_AQUA, new Color(0, 170, 170))
        .put(ChatColor.DARK_RED, new Color(170, 0, 0))
        .put(ChatColor.DARK_PURPLE, new Color(170, 0, 170))
        .put(ChatColor.GOLD, new Color(255, 170, 0))
        .put(ChatColor.GRAY, new Color(170, 170, 170))
        .put(ChatColor.DARK_GRAY, new Color(85, 85, 85))
        .put(ChatColor.BLUE, new Color(85, 85, 255))
        .put(ChatColor.GREEN, new Color(85, 255, 85))
        .put(ChatColor.AQUA, new Color(85, 255, 255))
        .put(ChatColor.RED, new Color(255, 85, 85))
        .put(ChatColor.LIGHT_PURPLE, new Color(255, 85, 255))
        .put(ChatColor.YELLOW, new Color(255, 255, 85))
        .put(ChatColor.WHITE, new Color(255, 255, 255))
        .build();

    private ColorUtils() {}

    public static int hexToRgb(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        return Integer.parseInt(hex, 16);
    }

    public static ChatColor getClosestChatColor(Color color) {
        ChatColor closestColor = null;
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<ChatColor, Color> entry : COLOR_MAPPINGS.entrySet()) {
            double distance = getColorDistance(color, entry.getValue());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestColor = entry.getKey();
            }
        }

        return closestColor;
    }

    private static double getColorDistance(Color c1, Color c2) {
        double redDiff = (double) c1.getRed() - (double) c2.getRed();
        double greenDiff = (double) c1.getGreen() - (double) c2.getGreen();
        double blueDiff = (double) c1.getBlue() - (double) c2.getBlue();
        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }
}
