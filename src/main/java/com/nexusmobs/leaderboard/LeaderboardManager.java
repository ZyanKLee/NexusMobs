package com.nexusmobs.leaderboard;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages player statistics and leaderboards for elite mob kills
 */
public class LeaderboardManager {
    
    private final NexusMobsPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    private final Map<UUID, PlayerStats> playerStats;
    
    public LeaderboardManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerStats = new HashMap<>();
        loadData();
    }
    
    /**
     * Load player data from file
     */
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create player data file!");
                e.printStackTrace();
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String path = "players." + uuidStr;
                    
                    String name = dataConfig.getString(path + ".name", "Unknown");
                    int totalKills = dataConfig.getInt(path + ".total-kills", 0);
                    int totalDamageDealt = dataConfig.getInt(path + ".total-damage-dealt", 0);
                    int totalDeaths = dataConfig.getInt(path + ".deaths-to-elites", 0);
                    long lastKillTime = dataConfig.getLong(path + ".last-kill-time", 0);
                    
                    Map<String, Integer> killsByType = new HashMap<>();
                    if (dataConfig.contains(path + ".kills-by-type")) {
                        for (String type : dataConfig.getConfigurationSection(path + ".kills-by-type").getKeys(false)) {
                            killsByType.put(type, dataConfig.getInt(path + ".kills-by-type." + type, 0));
                        }
                    }
                    
                    PlayerStats stats = new PlayerStats(uuid, name, totalKills, totalDamageDealt, 
                            totalDeaths, lastKillTime, killsByType);
                    playerStats.put(uuid, stats);
                    
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in player data: " + uuidStr);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + playerStats.size() + " player stat records.");
    }
    
    /**
     * Save player data to file
     */
    public void saveData() {
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            
            dataConfig.set(path + ".name", stats.getPlayerName());
            dataConfig.set(path + ".total-kills", stats.getTotalKills());
            dataConfig.set(path + ".total-damage-dealt", stats.getTotalDamageDealt());
            dataConfig.set(path + ".deaths-to-elites", stats.getDeathsToElites());
            dataConfig.set(path + ".last-kill-time", stats.getLastKillTime());
            
            for (Map.Entry<String, Integer> killEntry : stats.getKillsByType().entrySet()) {
                dataConfig.set(path + ".kills-by-type." + killEntry.getKey(), killEntry.getValue());
            }
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data!");
            e.printStackTrace();
        }
    }
    
    /**
     * Get or create player stats
     */
    public PlayerStats getPlayerStats(Player player) {
        return playerStats.computeIfAbsent(player.getUniqueId(), 
                uuid -> new PlayerStats(uuid, player.getName()));
    }
    
    /**
     * Get player stats by UUID
     */
    public PlayerStats getPlayerStats(UUID uuid) {
        return playerStats.get(uuid);
    }
    
    /**
     * Record an elite mob kill
     */
    public void recordKill(Player player, String nexusMobTypeId, double damageDealt) {
        PlayerStats stats = getPlayerStats(player);
        stats.addKill(nexusMobTypeId);
        stats.addDamageDealt((int) damageDealt);
        stats.setLastKillTime(System.currentTimeMillis());
        
        // Update player name in case it changed
        stats.setPlayerName(player.getName());
    }
    
    /**
     * Record a death to an elite mob
     */
    public void recordDeath(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.addDeath();
    }
    
    /**
     * Get top players by total kills
     */
    public List<PlayerStats> getTopKillers(int limit) {
        return playerStats.values().stream()
                .sorted(Comparator.comparingInt(PlayerStats::getTotalKills).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Get top players by kills of a specific elite mob type
     */
    public List<PlayerStats> getTopKillersByType(String nexusMobTypeId, int limit) {
        return playerStats.values().stream()
                .filter(stats -> stats.getKillsByType().containsKey(nexusMobTypeId))
                .sorted((a, b) -> Integer.compare(
                        b.getKillsByType().getOrDefault(nexusMobTypeId, 0),
                        a.getKillsByType().getOrDefault(nexusMobTypeId, 0)))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Get top players by damage dealt
     */
    public List<PlayerStats> getTopDamage(int limit) {
        return playerStats.values().stream()
                .sorted(Comparator.comparingInt(PlayerStats::getTotalDamageDealt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Get player rank by total kills
     */
    public int getPlayerRank(UUID playerUUID) {
        List<PlayerStats> sorted = playerStats.values().stream()
                .sorted(Comparator.comparingInt(PlayerStats::getTotalKills).reversed())
                .collect(Collectors.toList());
        
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getPlayerUUID().equals(playerUUID)) {
                return i + 1;
            }
        }
        
        return -1;
    }
    
    /**
     * Get total number of elite mobs killed by all players
     */
    public int getTotalServerKills() {
        return playerStats.values().stream()
                .mapToInt(PlayerStats::getTotalKills)
                .sum();
    }
}


