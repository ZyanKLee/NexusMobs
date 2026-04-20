package com.nexusmobs.loot;

/**
 * Enum representing different item ability types
 */
public enum ItemAbilityType {
    
    // On-hit abilities
    LIFESTEAL("Lifesteal", "Heals you for a percentage of damage dealt"),
    LIGHTNING_STRIKE("Lightning Strike", "Chance to strike target with lightning"),
    FIRE_ASPECT_AOE("Fire Burst", "Sets nearby enemies on fire"),
    WITHER_HIT("Wither Touch", "Applies wither effect on hit"),
    POISON_HIT("Venom", "Applies poison effect on hit"),
    SLOWNESS_HIT("Frost Touch", "Slows enemies on hit"),
    KNOCKBACK_BOOST("Impact", "Enhanced knockback on hit"),
    CRITICAL_BOOST("Critical Master", "Increased critical hit damage"),
    
    // On-damage-taken abilities
    THORNS_AOE("Thorns Aura", "Damages nearby attackers"),
    DAMAGE_RESISTANCE("Guardian Shield", "Reduces incoming damage"),
    REFLECT_PROJECTILES("Arrow Deflection", "Chance to reflect projectiles"),
    
    // Passive abilities
    SPEED_BOOST("Swift", "Increases movement speed"),
    HEALTH_BOOST("Vitality", "Increases max health"),
    NIGHT_VISION("Dark Sight", "Grants night vision"),
    WATER_BREATHING("Aquatic", "Allows breathing underwater"),
    DOUBLE_JUMP("Leap", "Allows double jumping"),
    
    // Active abilities (right-click)
    TELEPORT("Blink", "Teleport forward on right-click"),
    HEAL_BURST("Healing Wave", "Heal yourself and nearby allies"),
    FIREBALL("Inferno", "Launch a fireball"),
    SHOCKWAVE("Earthquake", "Create a damaging shockwave"),
    INVISIBILITY_ACTIVE("Vanish", "Become invisible temporarily");
    
    private final String displayName;
    private final String description;
    
    ItemAbilityType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}

