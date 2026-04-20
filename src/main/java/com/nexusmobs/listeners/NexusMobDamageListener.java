package com.nexusmobs.listeners;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.abilities.AbilityManager;
import com.nexusmobs.models.NexusMobType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles elite mob damage events and triggers abilities
 */
public class NexusMobDamageListener implements Listener {
    
    private final NexusMobsPlugin plugin;
    
    public NexusMobDamageListener(NexusMobsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        // Check if the damager is an elite mob
        if (plugin.getNexusMobManager().isNexusMob(damager)) {
            handleNexusMobAttack(event, damager, victim);
        }
        
        // Check if the victim is an elite mob being attacked by player
        if (plugin.getNexusMobManager().isNexusMob(victim) && damager instanceof Player) {
            handlePlayerAttackElite(event, (Player) damager, victim);
        }
    }
    
    /**
     * Handle when an elite mob attacks something
     */
    private void handleNexusMobAttack(EntityDamageByEntityEvent event, Entity damager, Entity victim) {
        if (!(damager instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity nexusMob = (LivingEntity) damager;
        String typeId = plugin.getNexusMobManager().getNexusMobTypeId(nexusMob);
        
        if (typeId == null) {
            return;
        }
        
        NexusMobType type = plugin.getConfigManager().getNexusMobType(typeId);
        if (type == null) {
            return;
        }
        
        // Get abilities configuration
        ConfigurationSection abilitiesConfig = type.getAbilitiesConfig();
        if (abilitiesConfig == null) {
            return;
        }
        
        AbilityManager abilityManager = plugin.getNexusMobManager().getAbilityManager();
        
        // Trigger on-hit abilities
        
        // Knockback ability
        if (abilitiesConfig.contains("knockback")) {
            ConfigurationSection knockbackConfig = abilitiesConfig.getConfigurationSection("knockback");
            abilityManager.handleKnockbackAbility(nexusMob, victim, knockbackConfig);
        }
        
        // Fire attack ability
        if (abilitiesConfig.contains("fire-attack")) {
            ConfigurationSection fireConfig = abilitiesConfig.getConfigurationSection("fire-attack");
            abilityManager.handleFireAttackAbility(nexusMob, victim, fireConfig);
        }
        
        // Debuff ability
        if (abilitiesConfig.contains("debuff")) {
            ConfigurationSection debuffConfig = abilitiesConfig.getConfigurationSection("debuff");
            abilityManager.handleDebuffAbility(nexusMob, victim, debuffConfig);
        }
        
        // Teleport attack ability
        if (abilitiesConfig.contains("teleport-attack")) {
            ConfigurationSection teleportConfig = abilitiesConfig.getConfigurationSection("teleport-attack");
            abilityManager.handleTeleportAttackAbility(nexusMob, victim, teleportConfig);
        }
    }
    
    /**
     * Handle when a player attacks an elite mob
     */
    private void handlePlayerAttackElite(EntityDamageByEntityEvent event, Player attacker, Entity victim) {
        // Track damage for statistics (could be expanded)
        // The actual stat recording happens on death
    }
    
    /**
     * Handle player death by elite mob
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Entity killer = player.getKiller();
        
        // Check if killed by an elite mob
        if (killer != null && plugin.getNexusMobManager().isNexusMob(killer)) {
            String typeId = plugin.getNexusMobManager().getNexusMobTypeId(killer);
            NexusMobType type = plugin.getConfigManager().getNexusMobType(typeId);
            
            // Record death
            plugin.getLeaderboardManager().recordDeath(player);
            
            // Custom death message
            if (type != null) {
                String deathMessage = "§c" + player.getName() + " §7was slain by " + 
                        type.getDisplayName().replace("§", "&").replace("&", "§");
                event.deathMessage(net.kyori.adventure.text.Component.text(deathMessage));
            }
        }
    }
}


