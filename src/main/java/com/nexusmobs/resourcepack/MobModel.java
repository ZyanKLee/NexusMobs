package com.nexusmobs.resourcepack;

import org.bukkit.Material;

/**
 * Represents a custom model for elite mobs
 */
public class MobModel {
    
    private final String id;
    private final Material material;
    private final int customModelData;
    private final double offsetY;
    private final double scale;
    private final boolean small;
    private final boolean invisibleBase;
    private final String animation;
    
    public MobModel(String id, Material material, int customModelData, double offsetY, 
                    double scale, boolean small, boolean invisibleBase, String animation) {
        this.id = id;
        this.material = material;
        this.customModelData = customModelData;
        this.offsetY = offsetY;
        this.scale = scale;
        this.small = small;
        this.invisibleBase = invisibleBase;
        this.animation = animation;
    }
    
    public String getId() {
        return id;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public int getCustomModelData() {
        return customModelData;
    }
    
    public double getOffsetY() {
        return offsetY;
    }
    
    public double getScale() {
        return scale;
    }
    
    public boolean isSmall() {
        return small;
    }
    
    public boolean isInvisibleBase() {
        return invisibleBase;
    }
    
    public String getAnimation() {
        return animation;
    }
}

