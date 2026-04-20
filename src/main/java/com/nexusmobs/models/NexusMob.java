package com.nexusmobs.models;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Represents an active elite mob instance in the world
 */
public class NexusMob {
    
    private final UUID entityUUID;
    private final String typeId;
    private final Location spawnLocation;
    private final long spawnTime;
    
    public NexusMob(UUID entityUUID, String typeId, Location spawnLocation) {
        this.entityUUID = entityUUID;
        this.typeId = typeId;
        this.spawnLocation = spawnLocation;
        this.spawnTime = System.currentTimeMillis();
    }
    
    public UUID getEntityUUID() {
        return entityUUID;
    }
    
    public String getTypeId() {
        return typeId;
    }
    
    public Location getSpawnLocation() {
        return spawnLocation;
    }
    
    public long getSpawnTime() {
        return spawnTime;
    }
    
    /**
     * Check if the entity is still alive and valid
     */
    public boolean isValid(LivingEntity entity) {
        return entity != null && entity.isValid() && !entity.isDead();
    }
}


