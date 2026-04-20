package com.nexusmobs.managers;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.abilities.AbilityManager;
import com.nexusmobs.models.NexusMob;
import com.nexusmobs.models.NexusMobType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import com.nexusmobs.models.Phase;
import org.bukkit.NamespacedKey;

import java.util.*;

/**
 * Manages active Nexus mobs in the world
 */
public class NexusMobManager {
    
    private final NexusMobsPlugin plugin;
    private final Map<UUID, NexusMob> activeNexusMobs;
    private final NamespacedKey nexusMobKey;
    private final AbilityManager abilityManager;
    private final Map<UUID, Integer> currentPhaseIndex;
    
    public NexusMobManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.activeNexusMobs = new HashMap<>();
        this.nexusMobKey = new NamespacedKey(plugin, "nexus_mob_type");
        this.abilityManager = new AbilityManager(plugin);
        this.currentPhaseIndex = new HashMap<>();
    }
    
    /**
     * Spawn a Nexus mob at the given location
     */
    public NexusMob spawnNexusMob(NexusMobType type, Location location) {
        // Play spawn effect
        plugin.getEffectsManager().playSpawnEffect(location);
        
        // Spawn the base entity (slightly delayed to allow effect)
        Entity entity = location.getWorld().spawnEntity(location, type.getBaseEntity());
        
        if (!(entity instanceof LivingEntity)) {
            entity.remove();
            plugin.getLogger().warning("Failed to spawn Nexus mob: entity is not living");
            return null;
        }
        
        LivingEntity livingEntity = (LivingEntity) entity;
        
        // Configure the Nexus mob
        configureNexusMob(livingEntity, type);
        
        // Create tracking object
        NexusMob nexusMob = new NexusMob(livingEntity.getUniqueId(), type.getId(), location);
        activeNexusMobs.put(livingEntity.getUniqueId(), nexusMob);
        
        // Start ability tasks for this mob
        abilityManager.startAbilities(livingEntity, type);

        // Start phase watcher if phases configured
        if (type.getPhases() != null && !type.getPhases().isEmpty()) {
            startPhaseWatcher(livingEntity, type);
        }
        
        // Apply custom model if configured
        String modelId = plugin.getConfig().getString("elite-mobs." + type.getId() + ".model");
        if (modelId != null && plugin.getModelManager().hasModel(modelId)) {
            plugin.getModelManager().applyModel(livingEntity, modelId);
        }
        
        // Start ambient particles
        String particleType = plugin.getConfig().getString("elite-mobs." + type.getId() + ".ambient-particles", "flame");
        plugin.getEffectsManager().startAmbientParticles(livingEntity, particleType);
        
        // Create boss bar
        if (plugin.getConfig().getBoolean("elite-mobs." + type.getId() + ".boss-bar", true)) {
            BarColor barColor = getBarColor(type.getId());
            plugin.getEffectsManager().createBossBar(livingEntity, type.getDisplayName(), barColor);
        }
        
        plugin.getLogger().info("Spawned Nexus mob: " + type.getId() + " at " + 
            location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        
        return nexusMob;
    }

    /**
     * Start a repeating task that watches the mob's health and applies phase modifiers
     */
    private void startPhaseWatcher(LivingEntity entity, NexusMobType type) {
        UUID id = entity.getUniqueId();
        currentPhaseIndex.put(id, -1);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!entity.isValid() || entity.isDead()) {
                currentPhaseIndex.remove(id);
                return;
            }

            double maxHealth = type.getMaxHealth();
            double currentHealth = entity.getHealth();
            double percent = (currentHealth / Math.max(1.0, maxHealth)) * 100.0;

            List<Phase> phases = type.getPhases();
            if (phases == null || phases.isEmpty()) return;

            // Simplified behavior: only a single "Next" phase will activate when mob reaches half HP
            int targetIndex = -1;
            double halfHp = Math.max(1.0, maxHealth) * 0.5;
            if (currentHealth <= halfHp) {
                targetIndex = phases.size() - 1; // apply the last configured phase as the "next" phase
            }

            int currentIndex = currentPhaseIndex.getOrDefault(id, -1);
            if (targetIndex != -1 && targetIndex != currentIndex) {
                // Apply phase changes
                Phase phase = phases.get(targetIndex);

                // Adjust attack damage attribute
                if (entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE) != null) {
                    double base = type.getAttackDamage();
                    double newVal = base * phase.getAttackMultiplier();
                    entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(newVal);
                }

                // Adjust armor
                if (entity.getAttribute(org.bukkit.attribute.Attribute.ARMOR) != null) {
                    double baseArmor = type.getArmor();
                    entity.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(baseArmor + phase.getArmorBonus());
                }

                // Apply potion effects for this phase
                for (PotionEffect pe : phase.getPotionEffects()) {
                    entity.addPotionEffect(pe);
                }

                // Broadcast phase change to nearby players and play effects
                String phaseName = "Next Phase";
                if (plugin.getLanguageManager() != null) {
                    phaseName = plugin.getLanguageManager().getString("messages.phase-next", phaseName);
                }
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("name", type.getDisplayName());
                placeholders.put("phase", phaseName);

                String msg = plugin.getConfigManager().getMessage("phase-changed", placeholders);

                // Send message to players within 80 blocks
                entity.getWorld().getPlayers().forEach(p -> {
                    if (p.getLocation().distanceSquared(entity.getLocation()) <= (80 * 80)) {
                        p.sendMessage(msg);
                    }
                });

                // Play dramatic effects
                entity.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, entity.getLocation().add(0, 1, 0), 1);
                entity.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, entity.getLocation().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.2);
                try {
                    entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.9f);
                } catch (Exception ignored) {}

                // Final-phase special: make stronger and more dramatic
                if (targetIndex == phases.size() - 1) {
                    // Increase max health and set to full
                    if (entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
                        double curMax = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
                        double newMax = Math.max(curMax, type.getMaxHealth()) * 1.25;
                        entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(newMax);
                        entity.setHealth(newMax);
                    }

                    // Extra visual lightning strikes (effect only)
                    for (int i = 0; i < 3; i++) {
                        entity.getWorld().strikeLightningEffect(entity.getLocation().add((i - 1) * 2, 0, 0));
                    }

                    // Make sure glowing is on
                    entity.setGlowing(true);
                }

                // Mark applied
                currentPhaseIndex.put(id, targetIndex);
                plugin.getLogger().info("Applied phase " + targetIndex + " for mob " + type.getId() + " (" + id + ")");
            }
        }, 40L, 40L); // check every 2 seconds
    }
    
    /**
     * Configure a living entity as an elite mob
     */
    private void configureNexusMob(LivingEntity entity, NexusMobType type) {
        // Set custom name
        entity.setCustomName(type.getDisplayName());
        entity.setCustomNameVisible(true);
        
        // Set health - Paper 1.21+ uses Attribute.MAX_HEALTH
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(type.getMaxHealth());
            entity.setHealth(type.getMaxHealth());
        }
        
        // Set attack damage (if applicable) - Paper 1.21+ uses Attribute.ATTACK_DAMAGE
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(type.getAttackDamage());
        }
        
        // Set armor - Paper 1.21+ uses Attribute.ARMOR
        if (entity.getAttribute(Attribute.ARMOR) != null) {
            entity.getAttribute(Attribute.ARMOR).setBaseValue(type.getArmor());
        }
        
        // Set glowing
        entity.setGlowing(type.isGlowing());
        
        // Apply potion effects
        for (PotionEffect effect : type.getPotionEffects()) {
            entity.addPotionEffect(effect);
        }
        
        // Prevent despawning
        entity.setRemoveWhenFarAway(false);
        if (entity instanceof Mob) {
            ((Mob) entity).setPersistent(true);
        }
        
        // Mark as elite mob using persistent data
        entity.getPersistentDataContainer().set(nexusMobKey, PersistentDataType.STRING, type.getId());
    }
    
    /**
     * Get boss bar color based on mob type
     */
    private BarColor getBarColor(String typeId) {
        String colorStr = plugin.getConfig().getString("elite-mobs." + typeId + ".bar-color", "RED");
        try {
            return BarColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.RED;
        }
    }
    
    /**
     * Check if an entity is an elite mob
     */
    public boolean isNexusMob(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        
        return entity.getPersistentDataContainer().has(nexusMobKey, PersistentDataType.STRING);
    }
    
    /**
     * Get the elite mob type ID for an entity
     */
    public String getNexusMobTypeId(Entity entity) {
        if (!isNexusMob(entity)) {
            return null;
        }
        
        return entity.getPersistentDataContainer().get(nexusMobKey, PersistentDataType.STRING);
    }
    
    /**
     * Get elite mob instance by UUID
     */
    public NexusMob getNexusMob(UUID uuid) {
        return activeNexusMobs.get(uuid);
    }
    
    /**
     * Remove an elite mob from tracking
     */
    public void removeNexusMob(UUID uuid) {
        NexusMob removed = activeNexusMobs.remove(uuid);
        if (removed != null) {
            abilityManager.stopAbilities(uuid);
            plugin.getModelManager().removeModel(uuid);
        }
    }
    
    /**
     * Get count of active elite mobs
     */
    public int getActiveNexusMobCount() {
        // Clean up invalid mobs first
        activeNexusMobs.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            return entity == null || !entity.isValid() || entity.isDead();
        });
        
        return activeNexusMobs.size();
    }
    
    /**
     * Get all active elite mobs
     */
    public Collection<NexusMob> getActiveNexusMobs() {
        return Collections.unmodifiableCollection(activeNexusMobs.values());
    }
    
    /**
     * Get the ability manager
     */
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    /**
     * Update display names and boss bars for all active Nexus mobs (used after language/config reload)
     */
    public void updateAllMobDisplayNames() {
        for (NexusMob nm : getActiveNexusMobs()) {
            Entity e = Bukkit.getEntity(nm.getEntityUUID());
            if (e instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) e;
                String typeId = nm.getTypeId();
                NexusMobType type = plugin.getConfigManager().getNexusMobType(typeId);
                if (type != null) {
                    le.setCustomName(type.getDisplayName());
                    // refresh boss bar if present
                    if (plugin.getConfig().getBoolean("elite-mobs." + typeId + ".boss-bar", true)) {
                        plugin.getEffectsManager().createBossBar(le, type.getDisplayName(), getBarColor(typeId));
                    }
                }
            }
        }
    }
    
    /**
     * Cleanup all elite mobs
     */
    public void cleanup() {
        abilityManager.cleanup();
        
        // Remove model armor stands
        for (UUID uuid : activeNexusMobs.keySet()) {
            plugin.getModelManager().removeModel(uuid);
        }
        
        activeNexusMobs.clear();
    }
}


