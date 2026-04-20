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
