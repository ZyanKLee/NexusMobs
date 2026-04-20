package com.nexusmobs.models;

import org.bukkit.Material;

/**
 * Represents a loot drop with randomized quantity and chance
 */
public class LootDrop {
    
    private final Material material;
    private final int minAmount;
    private final int maxAmount;
    private final double chance;
    
    public LootDrop(Material material, int minAmount, int maxAmount, double chance) {
        this.material = material;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.chance = chance;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public int getMinAmount() {
        return minAmount;
    }
    
    public int getMaxAmount() {
        return maxAmount;
    }
    
    public double getChance() {
        return chance;
    }
}

