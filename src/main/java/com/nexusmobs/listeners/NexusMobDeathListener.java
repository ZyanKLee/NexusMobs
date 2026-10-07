package com.nexusmobs.listeners;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.loot.LootGenerator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Handles elite mob death and loot drops
 */
public class NexusMobDeathListener implements Listener {
    
    private final NexusMobsPlugin plugin;
    private final Random random;
    private final LootGenerator lootGenerator;
    
    public NexusMobDeathListener(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.lootGenerator = new LootGenerator(plugin, random);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        
        // Check if this is an elite mob
        if (!plugin.getNexusMobManager().isNexusMob(entity)) {
            return;
        }
        
        String typeId = plugin.getNexusMobManager().getNexusMobTypeId(entity);
        if (typeId == null) {
            return;
        }
        
        NexusMobType type = plugin.getConfigManager().getNexusMobType(typeId);
        if (type == null) {
            return;
        }
        
        // Get the killer for statistics
        Player killer = event.getEntity().getKiller();
        
        // Record the kill
        if (killer != null) {
            double totalDamage = type.getMaxHealth(); // Simplified - use actual damage tracking if needed
            plugin.getLeaderboardManager().recordKill(killer, typeId, totalDamage);
        }
        
        // Remove from tracking
        plugin.getNexusMobManager().removeNexusMob(entity.getUniqueId());
        
        // Play death effect
        plugin.getEffectsManager().playDeathEffect(entity.getLocation(), typeId);
        
        // Clear default drops
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Add custom loot
        List<ItemStack> customLoot = lootGenerator.generate(type);
        event.getDrops().addAll(customLoot);
        
        // Add bonus experience based on mob health
        int baseXP = (int) (type.getMaxHealth() / 5);
        event.setDroppedExp(baseXP + random.nextInt(50));
        
        // Broadcast death message
        broadcastDeath(type, killer);
        
        plugin.getLogger().info("Elite mob defeated: " + type.getId() + 
                (killer != null ? " by " + killer.getName() : ""));
    }
    
    /**
     * Broadcast elite mob death message
     */
    private void broadcastDeath(NexusMobType type, Player killer) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", type.getDisplayName());
        placeholders.put("killer", killer != null ? killer.getName() : "Unknown");
        
        String message = plugin.getConfigManager().getMessage("death-broadcast", placeholders);
        if (!message.isEmpty()) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text(message));
        }
    }
}


