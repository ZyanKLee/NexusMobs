package com.nexusmobs.models;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import com.nexusmobs.models.Phase;

/**
 * Represents a type of elite mob with its configuration
 */
public class NexusMobType {
    
    private final String id;
    private final EntityType baseEntity;
    private final String displayName;
    private final double maxHealth;
    private final double attackDamage;
    private final double armor;
    private final boolean glowing;
    private final List<PotionEffect> potionEffects;
    private final List<LootDrop> drops;
    private final ConfigurationSection abilitiesConfig;
    private final List<Phase> phases;
    
    public NexusMobType(String id, EntityType baseEntity, String displayName, 
                        double maxHealth, double attackDamage, double armor, 
                        boolean glowing, List<PotionEffect> potionEffects,
                        List<LootDrop> drops, ConfigurationSection abilitiesConfig,
                        List<Phase> phases) {
        this.id = id;
        this.baseEntity = baseEntity;
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.armor = armor;
        this.glowing = glowing;
        this.potionEffects = potionEffects;
        this.drops = drops;
        this.abilitiesConfig = abilitiesConfig;
        this.phases = phases;
    }
    
    public String getId() {
        return id;
    }
    
    public EntityType getBaseEntity() {
        return baseEntity;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getMaxHealth() {
        return maxHealth;
    }
    
    public double getAttackDamage() {
        return attackDamage;
    }
    
    public double getArmor() {
        return armor;
    }
    
    public boolean isGlowing() {
        return glowing;
    }
    
    public List<PotionEffect> getPotionEffects() {
        return potionEffects;
    }
    
    public List<LootDrop> getDrops() {
        return drops;
    }
    
    public ConfigurationSection getAbilitiesConfig() {
        return abilitiesConfig;
    }

    public List<Phase> getPhases() {
        return phases;
    }
}


