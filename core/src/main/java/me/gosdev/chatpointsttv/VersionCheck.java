package me.gosdev.chatpointsttv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.json.JSONArray;

import me.gosdev.chatpointsttv.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VersionCheck {

    private static final String URL = "https://api.modrinth.com/v2/project/nN0gRvoO/version";
    private static final String DOWNLOAD_URL = "https://modrinth.com/plugin/chatpointsttv";

    public static void check() {
        ChatPointsTTV plugin = ChatPointsTTV.getInstance();
        Logger log = plugin.log;

        try {
            String latest = getVersionNumber();
            Utils utils = ChatPointsTTV.getUtils();

            if (!ChatPointsTTV.getInstance().getDescription().getVersion().equals(latest.replaceAll("[^\\d.]", ""))) {
                for (Player player: plugin.getServer().getOnlinePlayers()) {
                    if (player.hasPermission(Permissions.MANAGE.permissionId)) {
                        sendVersionUpdateMessage(player, utils, latest);
                    }
                }
                String message = String.format("ChatPointsTTV v%S has been released! Download the latest version in %s", latest, DOWNLOAD_URL);
                log.log(Level.INFO, message);
            }

        } catch (IOException | URISyntaxException e) {
            log.log(Level.WARNING, "Couldn't fetch latest version.", e);
        }
    }

    private static void sendVersionUpdateMessage(Player player, Utils utils, String latest) {
        ComponentBuilder formatted = new ComponentBuilder(ChatColor.YELLOW + "Click " + ChatColor.UNDERLINE + "here" + ChatColor.RESET + ChatColor.YELLOW + " to download the latest version");

        BaseComponent button = formatted.create()[0];
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to open in browser").create()));
        button.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, DOWNLOAD_URL));

        utils.sendMessage(player, new TextComponent(ChatColor.YELLOW + "ChatPointsTTV v" + latest + " has been released!"));
        utils.sendMessage(player, button);
    }

    private static String getVersionNumber() throws IOException, URISyntaxException {
        StringBuilder result = new StringBuilder();
        HttpURLConnection conn = (HttpURLConnection) new URI(URL).toURL().openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        conn.disconnect();

        JSONArray json = new JSONArray(result.toString());
        return json.getJSONObject(0).getString("version_number");
    }
}
