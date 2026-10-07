package com.nexusmobs.models;

import org.bukkit.Material;

/**
 * Represents a loot drop with randomized quantity and chance.
 * Drops either a vanilla material or a custom item (by custom item id).
 */
public class LootDrop {
    
    private final Material material;
    private final String customItemId;
    private final int minAmount;
    private final int maxAmount;
    private final double chance;
    
    public LootDrop(Material material, int minAmount, int maxAmount, double chance) {
        this(material, null, minAmount, maxAmount, chance);
    }

    private LootDrop(Material material, String customItemId, int minAmount, int maxAmount, double chance) {
        this.material = material;
        this.customItemId = customItemId;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.chance = chance;
    }

    /**
     * A drop of the custom item with the given id (see config_items.yml).
     */
    public static LootDrop customItem(String customItemId, int minAmount, int maxAmount, double chance) {
        return new LootDrop(null, customItemId, minAmount, maxAmount, chance);
    }
    
    /**
     * @return the vanilla material, or null for a custom item drop
     */
    public Material getMaterial() {
        return material;
    }

    public boolean isCustomItem() {
        return customItemId != null;
    }

    /**
     * @return the custom item id, or null for a vanilla material drop
     */
    public String getCustomItemId() {
        return customItemId;
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

