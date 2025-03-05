package me.gosdev.chatpointsttv.utils;

import me.gosdev.chatpointsttv.ChatPointsTTV;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.logging.LogLevel;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bukkit.Bukkit.getLogger;

public class LibraryLoader {

    private static final Logger log = getLogger();

    private static final String RELOCATION_BASE = "me{}gosdev{}chatpointsttv{}libraries{}";

    public static final List<Library> LIBRARIES = List.of(
            Library.builder()
                    .groupId("org{}json")
                    .artifactId("json")
                    .version("20240303")
                    .relocate("org{}json{}json", RELOCATION_BASE + "json")
                    .build(),
            Library.builder()
                    .groupId("com{}github{}philippheuer.events4j")
                    .artifactId("events4j-handler-simple")
                    .version("0.12.2")
                    .relocate("com{}github{}philippheuer.events4j", RELOCATION_BASE + "events4j")
                    .build(),
            Library.builder()
                    .groupId("com{}github{}twitch4j")
                    .artifactId("twitch4j")
                    .version("1.23.0")
                    .relocate("com{}github{}twitch4j{}twitch4j", RELOCATION_BASE + "twitch4j")
                    .relocate("com{}fasterxml{}jackson", RELOCATION_BASE + "jackson")
                    .build()
    );

    private LibraryLoader() {
    }

    public static void loadLibraries(ChatPointsTTV plugin) {
        BukkitLibraryManager libraryManager = new BukkitLibraryManager(plugin);
        libraryManager.setLogLevel(LogLevel.WARN);
        libraryManager.addMavenCentral();
        log.log(Level.INFO, "Loading libraries...");
        for (Library lib : LIBRARIES) {
            try {
                libraryManager.loadLibrary(lib);
            } catch (Exception e) {
                String message = String.format("Failed to load library: %s v%s", lib.getArtifactId(), lib.getVersion());
                log.log(Level.SEVERE, message, e.getMessage());
            }
        }
    }
}
