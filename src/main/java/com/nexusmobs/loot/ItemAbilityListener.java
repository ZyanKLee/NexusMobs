package com.nexusmobs.loot;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

/**
 * Handles custom item ability triggers
 */
public class ItemAbilityListener implements Listener {
    
    private final NexusMobsPlugin plugin;
    private final CustomItemManager itemManager;
    
    public ItemAbilityListener(NexusMobsPlugin plugin, CustomItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity victim = (LivingEntity) event.getEntity();

        // Support both melee (player) and projectile (arrow/bullet) attacks
        Object damager = event.getDamager();

        Player player = null;
        ItemStack weapon = null;

        if (damager instanceof Player) {
            player = (Player) damager;
            weapon = player.getInventory().getItemInMainHand();
        } else if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player) {
                player = (Player) proj.getShooter();
                // Attempt to use the bow in main hand (common case)
                weapon = player.getInventory().getItemInMainHand();
                // fallback to offhand if main hand is not a custom item
                if ((weapon == null || !itemManager.isCustomItem(weapon)) && player.getInventory().getItemInOffHand() != null) {
                    weapon = player.getInventory().getItemInOffHand();
                }
            }
        }

        if (player == null || weapon == null) return;

        if (!itemManager.isCustomItem(weapon)) return;

        List<ItemAbility> abilities = itemManager.getAbilities(weapon);
        double finalDamage = event.getFinalDamage();

        for (ItemAbility ability : abilities) {
            if (!ability.shouldTrigger()) continue;

            switch (ability.getType()) {
                case LIFESTEAL:
                    handleLifesteal(player, finalDamage, ability);
                    break;
                case LIGHTNING_STRIKE:
                    handleLightningStrike(victim, ability);
                    break;
                case FIRE_ASPECT_AOE:
                    handleFireAoe(victim, ability);
                    break;
                case WITHER_HIT:
                    handleWitherHit(victim, ability);
                    break;
                case POISON_HIT:
                    handlePoisonHit(victim, ability);
                    break;
                case SLOWNESS_HIT:
                    handleSlownessHit(victim, ability);
                    break;
                case KNOCKBACK_BOOST:
                    handleKnockbackBoost(player, victim, ability);
                    break;
                case CRITICAL_BOOST:
                    handleCriticalBoost(event, ability);
                    break;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // Check all armor pieces and held items
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null || !itemManager.isCustomItem(item)) continue;
            
            List<ItemAbility> abilities = itemManager.getAbilities(item);
            
            for (ItemAbility ability : abilities) {
                if (!ability.shouldTrigger()) continue;
                
                switch (ability.getType()) {
                    case THORNS_AOE:
                        handleThornsAoe(player, event.getDamager(), ability);
                        break;
                    case DAMAGE_RESISTANCE:
                        handleDamageResistance(event, ability);
                        break;
                    case REFLECT_PROJECTILES:
                        if (event.getDamager() instanceof Projectile) {
                            handleReflectProjectile(player, (Projectile) event.getDamager(), ability);
                        }
                        break;
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!itemManager.isCustomItem(item)) return;
        
        List<ItemAbility> abilities = itemManager.getAbilities(item);
        
        for (ItemAbility ability : abilities) {
            ItemAbilityType type = ability.getType();
            
            // Only handle active abilities
            if (!isActiveAbility(type)) continue;
            
            // Check cooldown
            if (!itemManager.canUseAbility(player, type)) {
                int remaining = itemManager.getRemainingCooldown(player, type);
                player.sendMessage("§c" + type.getDisplayName() + " is on cooldown! (" + remaining + "s)");
                continue;
            }
            
            boolean success = false;
            
            switch (type) {
                case TELEPORT:
                    success = handleTeleportAbility(player, ability);
                    break;
                case HEAL_BURST:
                    success = handleHealBurst(player, ability);
                    break;
                case FIREBALL:
                    success = handleFireball(player, ability);
                    break;
                case SHOCKWAVE:
                    success = handleShockwave(player, ability);
                    break;
                case INVISIBILITY_ACTIVE:
                    success = handleInvisibility(player, ability);
                    break;
            }
            
            if (success) {
                itemManager.setCooldown(player, type, ability.getCooldownTicks());
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        itemManager.cleanupPlayer(event.getPlayer().getUniqueId());
    }
    
    // ================== ABILITY HANDLERS ==================
    
    private void handleLifesteal(Player player, double damage, ItemAbility ability) {
        double healAmount = damage * ability.getValue();
        
        // Get max health using Paper 1.21+ API
        double maxHealth = 20;
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        }
        
        double newHealth = Math.min(player.getHealth() + healAmount, maxHealth);
        player.setHealth(newHealth);
        
        // Visual effect
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 
                5, 0.3, 0.3, 0.3, 0);
    }
    
    private void handleLightningStrike(LivingEntity victim, ItemAbility ability) {
        victim.getWorld().strikeLightning(victim.getLocation());
    }
    
    private void handleFireAoe(LivingEntity victim, ItemAbility ability) {
        Location loc = victim.getLocation();
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, 
                ability.getValue(), ability.getValue(), ability.getValue());
        
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != victim) {
                entity.setFireTicks(ability.getDurationTicks());
            }
        }
        
        // Visual effect
        loc.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 
                50, ability.getValue(), 0.5, ability.getValue(), 0.05);
    }
    
    private void handleWitherHit(LivingEntity victim, ItemAbility ability) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 
                ability.getDurationTicks(), (int) ability.getValue() - 1));
        
        victim.getWorld().spawnParticle(Particle.SMOKE, victim.getLocation().add(0, 1, 0), 
                20, 0.3, 0.5, 0.3, 0.02);
    }
    
    private void handlePoisonHit(LivingEntity victim, ItemAbility ability) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 
                ability.getDurationTicks(), (int) ability.getValue() - 1));
        
        victim.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, victim.getLocation().add(0, 1, 0), 
                15, 0.3, 0.5, 0.3, 0);
    }
    
    private void handleSlownessHit(LivingEntity victim, ItemAbility ability) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 
                ability.getDurationTicks(), (int) ability.getValue() - 1));
        
        victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 0.5, 0), 
                10, 0.3, 0.2, 0.3, 0.02);
    }
    
    private void handleKnockbackBoost(Player attacker, LivingEntity victim, ItemAbility ability) {
        Vector direction = victim.getLocation().toVector()
                .subtract(attacker.getLocation().toVector())
                .normalize()
                .multiply(ability.getValue());
        direction.setY(0.4);
        
        victim.setVelocity(direction);
    }
    
    private void handleCriticalBoost(EntityDamageByEntityEvent event, ItemAbility ability) {
        // Increase damage by percentage
        double newDamage = event.getDamage() * (1 + ability.getValue());
        event.setDamage(newDamage);
        
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) event.getEntity();
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 
                    30, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    private void handleThornsAoe(Player player, Entity attacker, ItemAbility ability) {
        Location loc = player.getLocation();
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, 3, 3, 3);
        
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage(ability.getValue(), player);
            }
        }
        
        loc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 
                20, 1, 0.5, 1, 0);
    }
    
    private void handleDamageResistance(EntityDamageByEntityEvent event, ItemAbility ability) {
        double reduction = 1 - ability.getValue();
        event.setDamage(event.getDamage() * reduction);
    }
    
    private void handleReflectProjectile(Player player, Projectile projectile, ItemAbility ability) {
        if (projectile.getShooter() instanceof LivingEntity) {
            LivingEntity shooter = (LivingEntity) projectile.getShooter();
            
            Vector direction = shooter.getLocation().toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize();
            
            projectile.setVelocity(direction.multiply(projectile.getVelocity().length()));
            projectile.setShooter(player);
            
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.5f);
        }
    }
    
    private boolean handleTeleportAbility(Player player, ItemAbility ability) {
        Location target = player.getTargetBlock(null, (int) ability.getValue()).getLocation();
        target.setY(target.getY() + 1);
        target.setPitch(player.getLocation().getPitch());
        target.setYaw(player.getLocation().getYaw());
        
        // Visual effect at start
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 
                50, 0.5, 1, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        
        player.teleport(target);
        
        // Visual effect at destination
        player.getWorld().spawnParticle(Particle.PORTAL, target.add(0, 1, 0), 
                50, 0.5, 1, 0.5, 0.1);
        
        return true;
    }
    
    private boolean handleHealBurst(Player player, ItemAbility ability) {
        Location loc = player.getLocation();
        double radius = 5;
        
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, radius, radius, radius);
        
        for (Entity entity : nearby) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                
                // Get max health using Paper 1.21+ API
                double maxHealth = 20;
                if (target.getAttribute(Attribute.MAX_HEALTH) != null) {
                    maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                }
                
                double newHealth = Math.min(target.getHealth() + ability.getValue(), maxHealth);
                target.setHealth(newHealth);
            }
        }
        
        // Visual effect
        loc.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 
                40, radius, 1, radius, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.5f);
        
        return true;
    }
    
    private boolean handleFireball(Player player, ItemAbility ability) {
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield((float) ability.getValue());
        fireball.setIsIncendiary(true);
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1f, 1f);
        
        return true;
    }
    
    private boolean handleShockwave(Player player, ItemAbility ability) {
        Location loc = player.getLocation();
        double radius = ability.getValue();
        
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, radius, radius, radius);
        
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity victim = (LivingEntity) entity;
                victim.damage(5, player);
                
                Vector knockback = victim.getLocation().toVector()
                        .subtract(loc.toVector())
                        .normalize()
                        .multiply(1.5);
                knockback.setY(0.5);
                victim.setVelocity(knockback);
            }
        }
        
        // Visual effect
        for (int i = 0; i < 360; i += 10) {
            double radians = Math.toRadians(i);
            for (double r = 0; r < radius; r += 0.5) {
                double x = Math.cos(radians) * r;
                double z = Math.sin(radians) * r;
                loc.getWorld().spawnParticle(Particle.EXPLOSION, 
                        loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
            }
        }
        
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
        
        return true;
    }
    
    private boolean handleInvisibility(Player player, ItemAbility ability) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 
                ability.getDurationTicks(), 0, false, false));
        
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 
                30, 0.5, 1, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
        
        player.sendMessage("§7You have become invisible for " + (ability.getDurationTicks() / 20) + " seconds!");
        
        return true;
    }
    
    private boolean isActiveAbility(ItemAbilityType type) {
        return type == ItemAbilityType.TELEPORT ||
               type == ItemAbilityType.HEAL_BURST ||
               type == ItemAbilityType.FIREBALL ||
               type == ItemAbilityType.SHOCKWAVE ||
               type == ItemAbilityType.INVISIBILITY_ACTIVE;
    }
}

