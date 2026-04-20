package com.nexusmobs.leaderboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents player statistics for elite mob interactions
 */
public class PlayerStats {
    
    private final UUID playerUUID;
    private String playerName;
    private int totalKills;
    private int totalDamageDealt;
    private int deathsToElites;
    private long lastKillTime;
    private final Map<String, Integer> killsByType;
    
    public PlayerStats(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.totalKills = 0;
        this.totalDamageDealt = 0;
        this.deathsToElites = 0;
        this.lastKillTime = 0;
        this.killsByType = new HashMap<>();
    }
    
    public PlayerStats(UUID playerUUID, String playerName, int totalKills, int totalDamageDealt,
                       int deathsToElites, long lastKillTime, Map<String, Integer> killsByType) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.totalKills = totalKills;
        this.totalDamageDealt = totalDamageDealt;
        this.deathsToElites = deathsToElites;
        this.lastKillTime = lastKillTime;
        this.killsByType = new HashMap<>(killsByType);
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public int getTotalKills() {
        return totalKills;
    }
    
    public int getTotalDamageDealt() {
        return totalDamageDealt;
    }
    
    public int getDeathsToElites() {
        return deathsToElites;
    }
    
    public long getLastKillTime() {
        return lastKillTime;
    }
    
    public void setLastKillTime(long lastKillTime) {
        this.lastKillTime = lastKillTime;
    }
    
    public Map<String, Integer> getKillsByType() {
        return killsByType;
    }
    
    public void addKill(String nexusMobTypeId) {
        totalKills++;
        killsByType.merge(nexusMobTypeId, 1, Integer::sum);
    }
    
    public void addDamageDealt(int damage) {
        totalDamageDealt += damage;
    }
    
    public void addDeath() {
        deathsToElites++;
    }
    
    public int getKillsForType(String nexusMobTypeId) {
        return killsByType.getOrDefault(nexusMobTypeId, 0);
    }
    
    /**
     * Calculate K/D ratio (kills / deaths)
     */
    public double getKDRatio() {
        if (deathsToElites == 0) {
            return totalKills;
        }
        return (double) totalKills / deathsToElites;
    }
}


