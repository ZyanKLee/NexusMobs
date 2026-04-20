package com.nexusmobs.effects;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Manages visual and sound effects for elite mobs
 */
public class EffectsManager {
    
    private final NexusMobsPlugin plugin;
    
    public EffectsManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Play spawn effect for elite mob
     */
    public void playSpawnEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        // Spiral particle effect
        new BukkitRunnable() {
            double angle = 0;
            double y = 0;
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40) {
                    // Final explosion effect
                    world.spawnParticle(Particle.EXPLOSION, location.clone().add(0, 1, 0), 3);
                    world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);
                    cancel();
                    return;
                }
                
                for (int i = 0; i < 2; i++) {
                    double x = Math.cos(angle) * (2 - y/3);
                    double z = Math.sin(angle) * (2 - y/3);
                    
                    Location particleLoc = location.clone().add(x, y, z);
                    world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0, 0, 0, 0.02);
                    
                    angle += Math.PI / 8;
                }
                
                y += 0.1;
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        // Sound effect
        world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);
    }
    
    /**
     * Play death effect for elite mob
     */
    public void playDeathEffect(Location location, String mobTypeId) {
        World world = location.getWorld();
        if (world == null) return;
        
        // Explosion of particles
        world.spawnParticle(Particle.EXPLOSION_EMITTER, location.clone().add(0, 1, 0), 1);
        world.spawnParticle(Particle.SOUL, location.clone().add(0, 1, 0), 50, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.FLAME, location.clone().add(0, 1, 0), 100, 1, 1, 1, 0.15);
        
        // Ascending soul effect
        new BukkitRunnable() {
            double y = 1;
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 30) {
                    cancel();
                    return;
                }
                
                Location effectLoc = location.clone().add(0, y, 0);
                world.spawnParticle(Particle.SOUL, effectLoc, 5, 0.3, 0.3, 0.3, 0.02);
                
                y += 0.2;
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // Sound
        world.playSound(location, Sound.ENTITY_WITHER_DEATH, 0.7f, 0.7f);
        world.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }
    
    /**
     * Play ability activation effect
     */
    public void playAbilityEffect(LivingEntity entity, String abilityType) {
        Location location = entity.getLocation();
        World world = location.getWorld();
        if (world == null) return;
        
        switch (abilityType.toLowerCase()) {
            case "aoe-damage":
                playAoeDamageEffect(location, 5);
                break;
            case "summon-minions":
                playSummonEffect(location);
                break;
            case "teleport":
                playTeleportEffect(location);
                break;
            case "fire-attack":
                playFireEffect(location);
                break;
            case "knockback":
                playKnockbackEffect(location);
                break;
        }
    }
    
    private void playAoeDamageEffect(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        
        // Circular wave effect
        new BukkitRunnable() {
            double currentRadius = 0;
            
            @Override
            public void run() {
                if (currentRadius >= radius) {
                    cancel();
                    return;
                }
                
                for (int angle = 0; angle < 360; angle += 10) {
                    double radians = Math.toRadians(angle);
                    double x = Math.cos(radians) * currentRadius;
                    double z = Math.sin(radians) * currentRadius;
                    
                    Location particleLoc = center.clone().add(x, 0.2, z);
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.RED, 1));
                }
                
                currentRadius += 0.5;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
    }
    
    private void playSummonEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        world.spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 100, 1, 1, 1, 0.5);
        world.spawnParticle(Particle.WITCH, location.clone().add(0, 1, 0), 30, 1, 1, 1, 0.1);
        world.playSound(location, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);
    }
    
    private void playTeleportEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        world.spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.5);
        world.spawnParticle(Particle.REVERSE_PORTAL, location.clone().add(0, 1, 0), 40, 0.3, 0.5, 0.3, 0.1);
        world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
    }
    
    private void playFireEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        world.spawnParticle(Particle.FLAME, location.clone().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.LAVA, location.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
        world.playSound(location, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
    }
    
    private void playKnockbackEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        world.spawnParticle(Particle.CLOUD, location.clone().add(0, 1, 0), 20, 0.5, 0.3, 0.5, 0.1);
        world.spawnParticle(Particle.SWEEP_ATTACK, location.clone().add(0, 1, 0), 5, 0.5, 0.3, 0.5, 0);
        world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
    }
    
    /**
     * Create ambient particles around elite mob
     */
    public void startAmbientParticles(LivingEntity entity, String particleType) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead()) {
                    cancel();
                    return;
                }
                
                Location loc = entity.getLocation().add(0, 1, 0);
                World world = loc.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }
                
                switch (particleType.toLowerCase()) {
                    case "flame":
                        world.spawnParticle(Particle.FLAME, loc, 3, 0.3, 0.5, 0.3, 0.01);
                        break;
                    case "soul":
                        world.spawnParticle(Particle.SOUL, loc, 2, 0.3, 0.5, 0.3, 0.01);
                        break;
                    case "portal":
                        world.spawnParticle(Particle.PORTAL, loc, 5, 0.3, 0.5, 0.3, 0.1);
                        break;
                    case "enchant":
                        world.spawnParticle(Particle.ENCHANT, loc, 5, 0.5, 0.5, 0.5, 0.5);
                        break;
                    case "smoke":
                        world.spawnParticle(Particle.SMOKE, loc, 3, 0.3, 0.5, 0.3, 0.02);
                        break;
                    case "drip_lava":
                        world.spawnParticle(Particle.DRIPPING_LAVA, loc.add(0, 0.5, 0), 2, 0.3, 0.3, 0.3, 0);
                        break;
                    case "cloud":
                        world.spawnParticle(Particle.CLOUD, loc, 2, 0.3, 0.5, 0.3, 0.01);
                        break;
                    default:
                        world.spawnParticle(Particle.WITCH, loc, 2, 0.3, 0.5, 0.3, 0.01);
                        break;
                }
            }
        }.runTaskTimer(plugin, 0, 10);
    }
    
    /**
     * Play boss bar progress effect
     */
    public void createBossBar(LivingEntity entity, String name, org.bukkit.boss.BarColor color) {
        org.bukkit.boss.BossBar bossBar = Bukkit.createBossBar(
                name,
                color,
                org.bukkit.boss.BarStyle.SEGMENTED_10
        );
        
        // Update boss bar progress with entity health
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead()) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }
                
                // Paper 1.21+ uses Attribute.MAX_HEALTH
                double maxHealth = 100;
                if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
                    maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
                }
                
                double progress = entity.getHealth() / maxHealth;
                bossBar.setProgress(Math.max(0, Math.min(1, progress)));
                
                // Add nearby players to boss bar
                for (Entity nearby : entity.getNearbyEntities(50, 50, 50)) {
                    if (nearby instanceof Player) {
                        Player player = (Player) nearby;
                        if (!bossBar.getPlayers().contains(player)) {
                            bossBar.addPlayer(player);
                        }
                    }
                }
                
                // Remove far players
                for (Player player : bossBar.getPlayers()) {
                    if (player.getLocation().distance(entity.getLocation()) > 60) {
                        bossBar.removePlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }
}

