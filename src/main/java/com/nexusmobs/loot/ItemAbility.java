package com.nexusmobs.loot;

/**
 * Represents an ability attached to a custom item
 */
public class ItemAbility {
    
    private final ItemAbilityType type;
    private final double value; // Effect strength/percentage
    private final double chance; // Chance to trigger (0.0 - 1.0)
    private final int cooldownTicks; // Cooldown in ticks for active abilities
    private final int durationTicks; // Duration for effects
    
    public ItemAbility(ItemAbilityType type, double value, double chance, int cooldownTicks, int durationTicks) {
        this.type = type;
        this.value = value;
        this.chance = chance;
        this.cooldownTicks = cooldownTicks;
        this.durationTicks = durationTicks;
    }
    
    public ItemAbilityType getType() {
        return type;
    }
    
    public double getValue() {
        return value;
    }
    
    public double getChance() {
        return chance;
    }
    
    public int getCooldownTicks() {
        return cooldownTicks;
    }
    
    public int getDurationTicks() {
        return durationTicks;
    }
    
    /**
     * Check if this ability should trigger based on chance
     */
    public boolean shouldTrigger() {
        return Math.random() < chance;
    }
    
    /**
     * Generate lore line for this ability
     */
    public String toLoreLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("§9✦ ").append(type.getDisplayName());
        
        if (value > 0) {
            if (type == ItemAbilityType.LIFESTEAL || 
                type == ItemAbilityType.DAMAGE_RESISTANCE ||
                type == ItemAbilityType.CRITICAL_BOOST) {
                sb.append(" §7(").append((int)(value * 100)).append("%)");
            } else {
                sb.append(" §7(").append(value).append(")");
            }
        }
        
        if (chance < 1.0) {
            sb.append(" §8[").append((int)(chance * 100)).append("% chance]");
        }
        
        return sb.toString();
    }
}

