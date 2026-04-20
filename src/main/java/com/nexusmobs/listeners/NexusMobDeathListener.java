package com.nexusmobs.listeners;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.config.ConfigManager;
import com.nexusmobs.models.NexusMob;
import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.models.LootDrop;
import com.nexusmobs.loot.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
    
    public NexusMobDeathListener(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
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
        List<ItemStack> customLoot = generateLoot(type);
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
     * Generate loot drops for an elite mob
     */
    private List<ItemStack> generateLoot(NexusMobType type) {
        List<ItemStack> loot = new ArrayList<>();
        
        // Regular drops
        for (LootDrop drop : type.getDrops()) {
            // Check chance
            if (random.nextDouble() > drop.getChance()) {
                continue;
            }
            
            // Calculate amount
            int amount = drop.getMinAmount();
            if (drop.getMaxAmount() > drop.getMinAmount()) {
                amount += random.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
            }
            
            // Create item stack
            ItemStack item = new ItemStack(drop.getMaterial(), amount);
            loot.add(item);
        }
        
        // Custom item drops
        for (Map.Entry<String, CustomItem> entry : plugin.getCustomItemManager().getCustomItems().entrySet()) {
            CustomItem customItem = entry.getValue();
            
            if (random.nextDouble() <= customItem.getDropChance()) {
                ItemStack item = plugin.getCustomItemManager().createItemStack(entry.getKey());
                if (item != null) {
                    loot.add(item);
                    plugin.getLogger().info("Custom item dropped: " + entry.getKey());
                }
            }
        }
        
        return loot;
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


