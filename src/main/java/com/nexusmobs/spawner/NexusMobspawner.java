package com.nexusmobs.spawner;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.models.NexusMob;
import com.nexusmobs.models.NexusMobType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Handles spawning of elite mobs at configured intervals
 */
public class NexusMobspawner {
    
    private final NexusMobsPlugin plugin;
    private final Random random = new Random();
    private BukkitTask scheduledTask;
    private File dataFile;
    private FileConfiguration dataCfg;
    private long lastSpawnTimestamp = 0L;
    
    public NexusMobspawner(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        // load persistent data
        try {
            dataFile = new File(plugin.getDataFolder(), "data.yml");
            if (!dataFile.exists()) {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            }
            dataCfg = YamlConfiguration.loadConfiguration(dataFile);
            lastSpawnTimestamp = dataCfg.getLong("last-spawn-timestamp", 0L);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to initialize spawner data file: " + e.getMessage());
        }
    }
    
    /**
     * Schedule the next elite mob spawn
     */
    public void scheduleNextSpawn() {
        cancelScheduledSpawns();
        double minHours = plugin.getConfigManager().getMinSpawnIntervalHours();
        double maxHours = plugin.getConfigManager().getMaxSpawnIntervalHours();
        double hoursDelay = minHours + random.nextDouble() * Math.max(0.0, (maxHours - minHours));
        long ticksDelay = (long) (hoursDelay * 60 * 60 * 20L); // Convert hours to ticks
        plugin.getLogger().info("Next Nexus mob spawn scheduled in " + String.format("%.2f", hoursDelay) + " hours");
        
        scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            attemptSpawn();
            scheduleNextSpawn(); // Schedule the next one
        }, ticksDelay);
    }
    
    /**
     * Attempt to spawn an elite mob
     */
    public void attemptSpawn() {
        // Check if spawning is enabled
        if (!plugin.getConfig().getBoolean("spawn.enabled", true)) {
            return;
        }
        // Enforce global cooldown between successful spawns (configurable)
        double cooldownHours = plugin.getConfig().getDouble("spawn.cooldown-hours", 48.0);
        long cooldownMs = (long) (cooldownHours * 3600_000L);
        long now = System.currentTimeMillis();
        if (lastSpawnTimestamp > 0 && (now - lastSpawnTimestamp) < cooldownMs) {
            plugin.getLogger().info("Nexus mob spawn cancelled: global cooldown active");
            return;
        }
        
        // Check if we've reached the concurrent elite mob limit
        int maxConcurrent = plugin.getConfigManager().getMaxConcurrentElites();
        if (plugin.getNexusMobManager().getActiveNexusMobCount() >= maxConcurrent) {
            plugin.getLogger().info("Nexus mob spawn cancelled: maximum concurrent mobs reached");
            return;
        }
        
        // Get online players
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            plugin.getLogger().info("Nexus mob spawn cancelled: no players online");
            return;
        }
        
        // Choose a random elite mob type
        NexusMobType type = plugin.getConfigManager().getRandomNexusMobType();
        if (type == null) {
            plugin.getLogger().warning("No Nexus mob types configured!");
            return;
        }
        
        // Try to find a valid spawn location
        Location spawnLocation = findSpawnLocation(players);
        if (spawnLocation == null) {
            plugin.getLogger().warning("Failed to find a valid spawn location for Nexus mob");
            return;
        }
        
        // Spawn the elite mob
        NexusMob nexusMob = plugin.getNexusMobManager().spawnNexusMob(type, spawnLocation);
        if (nexusMob == null) {
            plugin.getLogger().warning("Failed to spawn Nexus mob");
            return;
        }
        
        // Broadcast spawn message
        broadcastSpawn(type, spawnLocation);
        // Save last spawn timestamp
        lastSpawnTimestamp = System.currentTimeMillis();
        if (dataCfg != null && dataFile != null) {
            dataCfg.set("last-spawn-timestamp", lastSpawnTimestamp);
            try {
                dataCfg.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save spawner data: " + e.getMessage());
            }
        }
    }
    
    /**
     * Find a valid spawn location far from players
     */
    private Location findSpawnLocation(Collection<? extends Player> players) {
        List<String> allowedWorlds = plugin.getConfigManager().getAllowedWorlds();
        int minDistance = plugin.getConfigManager().getMinSpawnDistance();
        int maxDistance = plugin.getConfigManager().getMaxSpawnDistance();
        int maxAttempts = plugin.getConfigManager().getMaxSpawnAttempts();
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Choose a random online player as reference
            Player randomPlayer = players.stream()
                    .skip(random.nextInt(players.size()))
                    .findFirst()
                    .orElse(null);
            
            if (randomPlayer == null) continue;
            
            World world = randomPlayer.getWorld();
            
            // Check if world is allowed
            if (!allowedWorlds.contains(world.getName())) {
                continue;
            }
            
            Location playerLoc = randomPlayer.getLocation();
            
            // Generate random polar coordinates
            double angle = random.nextDouble() * 2 * Math.PI;
            int distance = minDistance + random.nextInt(maxDistance - minDistance);
            
            int offsetX = (int) (Math.cos(angle) * distance);
            int offsetZ = (int) (Math.sin(angle) * distance);
            
            int spawnX = playerLoc.getBlockX() + offsetX;
            int spawnZ = playerLoc.getBlockZ() + offsetZ;
            
            // Load chunk if necessary
            Chunk chunk = world.getChunkAt(spawnX >> 4, spawnZ >> 4);
            if (!chunk.isLoaded()) {
                chunk.load();
            }
            
            // Find safe Y coordinate
            Location spawnLoc = findSafeY(world, spawnX, spawnZ);
            if (spawnLoc != null) {
                return spawnLoc;
            }
        }
        
        return null;
    }
    
    /**
     * Find a safe Y coordinate at the given X and Z
     */
    private Location findSafeY(World world, int x, int z) {
        // Start from world max height and go down
        int maxY = world.getMaxHeight() - 1;
        int minY = world.getMinHeight();
        
        for (int y = maxY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block below = world.getBlockAt(x, y - 1, z);
            Block above = world.getBlockAt(x, y + 1, z);
            
            // Check if this is a safe spawn location
            if (!block.getType().isSolid() && 
                !above.getType().isSolid() &&
                below.getType().isSolid() &&
                !below.isLiquid()) {
                
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        
        return null;
    }
    
    /**
     * Manually spawn an elite mob near a player
     */
    public boolean manualSpawn(Player player, String typeId) {
        NexusMobType type;
        
        if (typeId == null) {
            type = plugin.getConfigManager().getRandomNexusMobType();
        } else {
            type = plugin.getConfigManager().getNexusMobType(typeId);
        }
        
        if (type == null) {
            return false;
        }
        
        // Spawn near player (50 blocks away)
        Location playerLoc = player.getLocation();
        double angle = random.nextDouble() * 2 * Math.PI;
        int distance = 30 + random.nextInt(40);
        
        int offsetX = (int) (Math.cos(angle) * distance);
        int offsetZ = (int) (Math.sin(angle) * distance);
        
        int spawnX = playerLoc.getBlockX() + offsetX;
        int spawnZ = playerLoc.getBlockZ() + offsetZ;
        
        Location spawnLoc = findSafeY(player.getWorld(), spawnX, spawnZ);
        if (spawnLoc == null) {
            spawnLoc = playerLoc.clone().add(offsetX, 0, offsetZ);
        }
        
        NexusMob nexusMob = plugin.getNexusMobManager().spawnNexusMob(type, spawnLoc);
        if (nexusMob != null) {
            broadcastSpawn(type, spawnLoc);
            return true;
        }
        
        return false;
    }
    
    /**
     * Broadcast elite mob spawn to all players
     */
    private void broadcastSpawn(NexusMobType type, Location location) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", type.getDisplayName());
        placeholders.put("world", location.getWorld().getName());
        placeholders.put("x", String.valueOf(location.getBlockX()));
        placeholders.put("y", String.valueOf(location.getBlockY()));
        placeholders.put("z", String.valueOf(location.getBlockZ()));
        placeholders.put("type", type.getId());
        String message = plugin.getConfigManager().getMessage("spawn-broadcast", placeholders);
        Bukkit.getServer().broadcastMessage(message);
    }
    
    /**
     * Cancel all scheduled spawns
     */
    public void cancelScheduledSpawns() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }
}


