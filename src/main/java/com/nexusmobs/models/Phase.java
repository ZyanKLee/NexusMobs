package com.nexusmobs.models;

import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * Represents a phase of a Nexus mob. Supports threshold as percent OR absolute HP.
 */
public class Phase {
    // If thresholdHp is >= 0, it's used as absolute HP threshold. Otherwise thresholdPercent is used (0-100).
    private final double thresholdPercent; // -1 if unused
    private final double thresholdHp; // -1 if unused
    private final double attackMultiplier;
    private final double armorBonus;
    private final List<PotionEffect> potionEffects;

    public Phase(double thresholdPercent, double attackMultiplier, double armorBonus, List<PotionEffect> potionEffects) {
        this.thresholdPercent = thresholdPercent;
        this.thresholdHp = -1;
        this.attackMultiplier = attackMultiplier;
        this.armorBonus = armorBonus;
        this.potionEffects = potionEffects;
    }

    public Phase(double thresholdHp, boolean absoluteHp, double attackMultiplier, double armorBonus, List<PotionEffect> potionEffects) {
        this.thresholdPercent = -1;
        this.thresholdHp = thresholdHp;
        this.attackMultiplier = attackMultiplier;
        this.armorBonus = armorBonus;
        this.potionEffects = potionEffects;
    }

    /**
     * Whether this phase's threshold has been reached.
     *
     * @param health    the mob's current health
     * @param maxHealth the mob type's configured max health (base for percentage thresholds)
     */
    public boolean isReachedAt(double health, double maxHealth) {
        if (usesAbsoluteHp()) {
            return health <= thresholdHp;
        }
        return health / Math.max(1.0, maxHealth) * 100.0 <= thresholdPercent;
    }

    /**
     * The threshold as a percentage of {@code maxHealth}, for ordering phases.
     */
    public double thresholdPercentOf(double maxHealth) {
        return usesAbsoluteHp() ? thresholdHp / Math.max(1.0, maxHealth) * 100.0 : thresholdPercent;
    }

    public boolean usesAbsoluteHp() {
        return thresholdHp >= 0;
    }

    public double getThresholdPercent() {
        return thresholdPercent;
    }

    public double getThresholdHp() {
        return thresholdHp;
    }

    public double getAttackMultiplier() {
        return attackMultiplier;
    }

    public double getArmorBonus() {
        return armorBonus;
    }

    public List<PotionEffect> getPotionEffects() {
        return potionEffects;
    }
}
