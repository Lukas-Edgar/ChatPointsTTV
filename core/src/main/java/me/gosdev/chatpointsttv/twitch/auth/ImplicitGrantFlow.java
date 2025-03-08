package me.gosdev.chatpointsttv.twitch.auth;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import me.gosdev.chatpointsttv.ChatPointsTTV;
import me.gosdev.chatpointsttv.twitch.TwitchClient;
import me.gosdev.chatpointsttv.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImplicitGrantFlow {

    public static final AuthenticationCallbackServer server = new AuthenticationCallbackServer(3000);

    private static final Utils utils = ChatPointsTTV.getUtils();

    public static CompletableFuture<String> getAccessToken(ChatPointsTTV plugin, CommandSender sender, String clientID) {
        CompletableFuture<String> future = new CompletableFuture<>();
        String authURL = "https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=" + clientID + "&redirect_uri=http://localhost:3000&scope=" + TwitchClient.SCOPES;

        plugin.getTwitch().close();

        if (sender == Bukkit.getServer().getConsoleSender()) {
            TextComponent msg = new TextComponent("Link your Twitch account to set ChatPointsTTV up. Open this link in your browser to login:\n" + authURL);
            utils.sendMessage(sender, msg);
        } else {
            BaseComponent msg = new TextComponent(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "--------------- ChatPointsTTV ---------------\n" + ChatColor.RESET + ChatColor.WHITE + "Link your Twitch account to set ChatPointsTTV up\n");
            BaseComponent btn = new TextComponent(ChatColor.LIGHT_PURPLE + "[Click here to login with Twitch]");
            btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to open in browser").create()));
            btn.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, authURL));

            msg.addExtra(btn);

            utils.sendMessage(sender, msg);
        }
        
        int serverCloseId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> server.stop(), 6000L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (server.isNotRunning()) {
                    server.start();
                }
                if(server.getAccessToken() != null) {
                    server.stop();
                    Bukkit.getScheduler().cancelTask(serverCloseId);
                    future.complete(server.getAccessToken());
                }
            } catch(IOException e) {
                plugin.log.warning(e.getMessage());
            }
        });
        return future;
    }
}
