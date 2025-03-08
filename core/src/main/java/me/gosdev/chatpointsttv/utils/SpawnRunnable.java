package me.gosdev.chatpointsttv.utils;

import lombok.Setter;
import me.gosdev.chatpointsttv.ChatPointsTTV;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

@Setter
public class SpawnRunnable implements Runnable {
    private EntityType entity;
    private int iterations = 0;
    private int amount;
    private String entityName;
    private Integer explosionTime = null;
    private Player player;
    private int id;


    @Override
    public void run() {
        iterations++;
        if (iterations >= amount) Bukkit.getScheduler().cancelTask(id);
        if (entity == EntityType.PRIMED_TNT) {
            TNTPrimed tnt = (TNTPrimed) player.getWorld().spawnEntity(player.getLocation(), EntityType.PRIMED_TNT);
            if (explosionTime != null) tnt.setFuseTicks(explosionTime);
        } else {
            Entity e = player.getWorld().spawnEntity(player.getLocation(), entity);
            ChatPointsTTV chatpoints = ChatPointsTTV.getInstance();
            e.setGlowing(chatpoints.isShouldMobsGlow());
            if (chatpoints.isNameSpawnedMobs()) {
                e.setCustomName(entityName);
                e.setCustomNameVisible(true);
            }
        }
    }

}
    