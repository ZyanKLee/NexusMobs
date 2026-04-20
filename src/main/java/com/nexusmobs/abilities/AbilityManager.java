package com.nexusmobs.abilities;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.models.NexusMobType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Manages abilities for elite mobs
 */
public class AbilityManager {
    
    private final NexusMobsPlugin plugin;
    private final Map<UUID, List<BukkitTask>> abilityTasks;
    private final Random random = new Random();
    
    public AbilityManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.abilityTasks = new HashMap<>();
    }
    
    /**
     * Start all abilities for an elite mob
     */
    public void startAbilities(LivingEntity entity, NexusMobType type) {
        ConfigurationSection abilitiesConfig = type.getAbilitiesConfig();
        if (abilitiesConfig == null) {
            return;
        }
        
        List<BukkitTask> tasks = new ArrayList<>();
        
        // AOE Damage Ability
        if (abilitiesConfig.contains("aoe-damage") && 
            abilitiesConfig.getBoolean("aoe-damage.enabled", false)) {
            ConfigurationSection aoeConfig = abilitiesConfig.getConfigurationSection("aoe-damage");
            BukkitTask task = startAoeDamageAbility(entity, aoeConfig);
            if (task != null) tasks.add(task);
        }
        
        // Summon Minions Ability
        if (abilitiesConfig.contains("summon-minions") && 
            abilitiesConfig.getBoolean("summon-minions.enabled", false)) {
            ConfigurationSection summonConfig = abilitiesConfig.getConfigurationSection("summon-minions");
            BukkitTask task = startSummonMinionsAbility(entity, summonConfig);
            if (task != null) tasks.add(task);
        }
        
        abilityTasks.put(entity.getUniqueId(), tasks);
    }
    
    /**
     * Start AOE damage ability
     */
    private BukkitTask startAoeDamageAbility(LivingEntity entity, ConfigurationSection config) {
        double damage = config.getDouble("damage", 5.0);
        double radius = config.getDouble("radius", 5.0);
        int interval = config.getInt("interval-ticks", 100);
        
        return Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!entity.isValid() || entity.isDead()) {
                return;
            }
            
            Location loc = entity.getLocation();
            Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, radius, radius, radius);
            
            for (Entity nearbyEntity : nearby) {
                if (nearbyEntity instanceof Player && nearbyEntity != entity) {
                    Player player = (Player) nearbyEntity;
                    player.damage(damage, entity);
                    
                    // Visual effect
                    player.getWorld().spawnParticle(
                        org.bukkit.Particle.SMOKE,
                        player.getLocation().add(0, 1, 0),
                        20, 0.5, 0.5, 0.5, 0.05
                    );
                }
            }
        }, interval, interval);
    }
    
    /**
     * Start summon minions ability
     */
    private BukkitTask startSummonMinionsAbility(LivingEntity entity, ConfigurationSection config) {
        String mobTypeStr = config.getString("mob-type", "ZOMBIE");
        int count = config.getInt("count", 3);
        double chance = config.getDouble("chance", 0.2);
        int interval = config.getInt("interval-ticks", 400);
        
        EntityType minionType;
        try {
            minionType = EntityType.valueOf(mobTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid minion type: " + mobTypeStr);
            return null;
        }
        
        return Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!entity.isValid() || entity.isDead()) {
                return;
            }
            
            // Check chance
            if (Math.random() > chance) {
                return;
            }
            
            Location loc = entity.getLocation();
            
            for (int i = 0; i < count; i++) {
                // Spawn minion nearby
                double offsetX = (random.nextDouble() - 0.5) * 6;
                double offsetZ = (random.nextDouble() - 0.5) * 6;
                Location spawnLoc = loc.clone().add(offsetX, 0, offsetZ);
                
                Entity minion = loc.getWorld().spawnEntity(spawnLoc, minionType);
                
                // Visual effect
                spawnLoc.getWorld().spawnParticle(
                    org.bukkit.Particle.PORTAL,
                    spawnLoc.add(0, 1, 0),
                    30, 0.5, 0.5, 0.5, 0.1
                );
            }
        }, interval, interval);
    }
    
    /**
     * Handle knockback ability (called from damage listener)
     */
    public void handleKnockbackAbility(LivingEntity attacker, Entity victim, ConfigurationSection config) {
        if (config == null || !config.getBoolean("enabled", false)) {
            return;
        }
        
        double strength = config.getDouble("strength", 2.0);
        double chance = config.getDouble("chance", 0.3);
        
        if (Math.random() > chance) {
            return;
        }
        
        if (victim instanceof LivingEntity) {
            Vector direction = victim.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector())
                    .normalize()
                    .multiply(strength);
            direction.setY(0.5);
            
            victim.setVelocity(direction);
        }
    }
    
    /**
     * Handle fire attack ability (called from damage listener)
     */
    public void handleFireAttackAbility(LivingEntity attacker, Entity victim, ConfigurationSection config) {
        if (config == null || !config.getBoolean("enabled", false)) {
            return;
        }
        
        int duration = config.getInt("duration-ticks", 100);
        double chance = config.getDouble("chance", 0.4);
        
        if (Math.random() > chance) {
            return;
        }
        
        victim.setFireTicks(duration);
    }
    
    /**
     * Handle debuff ability (called from damage listener)
     */
    public void handleDebuffAbility(LivingEntity attacker, Entity victim, ConfigurationSection config) {
        if (config == null || !config.getBoolean("enabled", false)) {
            return;
        }
        
        if (!(victim instanceof LivingEntity)) {
            return;
        }
        
        List<String> effects = config.getStringList("effects");
        int duration = config.getInt("duration-ticks", 100);
        double chance = config.getDouble("chance", 0.3);
        
        if (Math.random() > chance) {
            return;
        }
        
        LivingEntity livingVictim = (LivingEntity) victim;
        
        for (String effectName : effects) {
            PotionEffectType effectType = getPotionEffectType(effectName);
            if (effectType != null) {
                livingVictim.addPotionEffect(new PotionEffect(effectType, duration, 0));
            }
        }
    }
    
    /**
     * Get PotionEffectType by name (compatible with Paper 1.21+)
     */
    private PotionEffectType getPotionEffectType(String name) {
        if (name == null) return null;
        
        // Try to get from Registry (Paper 1.21+)
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return Registry.POTION_EFFECT_TYPE.get(key);
        } catch (Exception e) {
            // Fallback for older API
            try {
                return PotionEffectType.getByName(name);
            } catch (Exception e2) {
                return null;
            }
        }
    }
    
    /**
     * Handle teleport attack ability (called from damage listener)
     */
    public void handleTeleportAttackAbility(LivingEntity attacker, Entity victim, ConfigurationSection config) {
        if (config == null || !config.getBoolean("enabled", false)) {
            return;
        }
        
        double chance = config.getDouble("chance", 0.5);
        
        if (Math.random() > chance) {
            return;
        }
        
        // Teleport behind the victim
        Location victimLoc = victim.getLocation();
        Vector direction = victimLoc.getDirection().multiply(-2);
        Location teleportLoc = victimLoc.clone().add(direction);
        teleportLoc.setY(victimLoc.getY());
        
        attacker.teleport(teleportLoc);
        
        // Visual effect
        attacker.getWorld().spawnParticle(
            org.bukkit.Particle.PORTAL,
            attacker.getLocation().add(0, 1, 0),
            50, 0.5, 1, 0.5, 0.1
        );
    }
    
    /**
     * Stop all abilities for an elite mob
     */
    public void stopAbilities(UUID entityUUID) {
        List<BukkitTask> tasks = abilityTasks.remove(entityUUID);
        if (tasks != null) {
            for (BukkitTask task : tasks) {
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
            }
        }
    }
    
    /**
     * Cleanup all ability tasks
     */
    public void cleanup() {
        for (List<BukkitTask> tasks : abilityTasks.values()) {
            for (BukkitTask task : tasks) {
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
            }
        }
        abilityTasks.clear();
    }
}

