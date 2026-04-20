package com.nexusmobs.resourcepack;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages custom model integration for elite mobs
 * Similar to ModelEngine/Oraxen system using CustomModelData
 */
public class ModelManager {
    
    private final NexusMobsPlugin plugin;
    private final Map<String, MobModel> mobModels;
    private final Map<UUID, ArmorStand> modelArmorStands;
    
    public ModelManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.mobModels = new HashMap<>();
        this.modelArmorStands = new HashMap<>();
        loadModels();
    }
    
    /**
     * Load model configurations from config
     */
    private void loadModels() {
        mobModels.clear();
        
        ConfigurationSection modelsSection = plugin.getConfig().getConfigurationSection("models");
        if (modelsSection == null) {
            plugin.getLogger().info("No custom models configured.");
            return;
        }
        
        for (String key : modelsSection.getKeys(false)) {
            ConfigurationSection modelSection = modelsSection.getConfigurationSection(key);
            if (modelSection == null) continue;
            
            try {
                MobModel model = loadModel(key, modelSection);
                mobModels.put(key, model);
                plugin.getLogger().info("Loaded model: " + key);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load model: " + key);
                e.printStackTrace();
            }
        }
    }
    
    private MobModel loadModel(String id, ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("material", "LEATHER_HORSE_ARMOR").toUpperCase());
        int customModelData = section.getInt("custom-model-data", 1);
        double offsetY = section.getDouble("offset-y", 0);
        double scale = section.getDouble("scale", 1.0);
        boolean small = section.getBoolean("small", false);
        boolean invisible = section.getBoolean("invisible-base", true);
        String animation = section.getString("animation", "idle");
        
        return new MobModel(id, material, customModelData, offsetY, scale, small, invisible, animation);
    }
    
    /**
     * Apply a custom model to an elite mob using armor stand
     */
    public void applyModel(LivingEntity entity, String modelId) {
        MobModel model = mobModels.get(modelId);
        if (model == null) {
            plugin.getLogger().warning("Model not found: " + modelId);
            return;
        }
        
        // Make base entity invisible if configured
        if (model.isInvisibleBase()) {
            entity.setInvisible(true);
        }
        
        // Create armor stand for model display
        ArmorStand stand = entity.getWorld().spawn(
                entity.getLocation().add(0, model.getOffsetY(), 0),
                ArmorStand.class
        );
        
        // Configure armor stand
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(model.isSmall());
        stand.setMarker(true);
        stand.setInvulnerable(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        
        // Create model item with CustomModelData
        ItemStack modelItem = createModelItem(model);
        stand.setItem(EquipmentSlot.HEAD, modelItem);
        
        // Track the armor stand
        modelArmorStands.put(entity.getUniqueId(), stand);
        
        // Start following task
        startModelFollowTask(entity, stand, model);
    }
    
    /**
     * Create the item that displays the custom model
     */
    private ItemStack createModelItem(MobModel model) {
        ItemStack item = new ItemStack(model.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(model.getCustomModelData());
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Start task to make armor stand follow the entity
     */
    private void startModelFollowTask(LivingEntity entity, ArmorStand stand, MobModel model) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || !stand.isValid()) {
                    removeModel(entity.getUniqueId());
                    cancel();
                    return;
                }
                
                // Update armor stand position
                stand.teleport(entity.getLocation().add(0, model.getOffsetY(), 0));
                
                // Match rotation
                stand.setRotation(entity.getLocation().getYaw(), 0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * Remove model from entity
     */
    public void removeModel(UUID entityUUID) {
        ArmorStand stand = modelArmorStands.remove(entityUUID);
        if (stand != null && stand.isValid()) {
            stand.remove();
        }
    }
    
    /**
     * Get all registered mob models
     */
    public Map<String, MobModel> getMobModels() {
        return mobModels;
    }
    
    /**
     * Get a specific model
     */
    public MobModel getModel(String id) {
        return mobModels.get(id);
    }
    
    /**
     * Check if a model exists
     */
    public boolean hasModel(String id) {
        return mobModels.containsKey(id);
    }
    
    /**
     * Cleanup all model armor stands
     */
    public void cleanup() {
        for (ArmorStand stand : modelArmorStands.values()) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
        modelArmorStands.clear();
    }
    
    /**
     * Reload models from config
     */
    public void reload() {
        cleanup();
        loadModels();
    }
}

