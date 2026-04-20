package com.nexusmobs;

import com.nexusmobs.commands.NexusMobsCommand;
import com.nexusmobs.config.ConfigManager;
import com.nexusmobs.effects.EffectsManager;
import com.nexusmobs.gui.GuiListener;
import com.nexusmobs.gui.GuiManager;
import com.nexusmobs.leaderboard.LeaderboardManager;
import com.nexusmobs.listeners.NexusMobDeathListener;
import com.nexusmobs.listeners.NexusMobDamageListener;
import com.nexusmobs.loot.CustomItemManager;
import com.nexusmobs.loot.ItemAbilityListener;
import com.nexusmobs.managers.NexusMobManager;
import com.nexusmobs.resourcepack.ModelManager;
import com.nexusmobs.resourcepack.ResourcePackGenerator;
import com.nexusmobs.spawner.NexusMobspawner;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Main plugin class for NexusMobs
 * A comprehensive elite mob system with custom models, abilities, and loot
 */
public class NexusMobsPlugin extends JavaPlugin {
    
    private static volatile NexusMobsPlugin instance;

    private static void setInstance(NexusMobsPlugin inst) {
        instance = inst;
    }
    
    // Managers
    private ConfigManager configManager;
    private com.nexusmobs.config.LanguageManager languageManager;
    private NexusMobManager nexusMobManager;
    private NexusMobspawner NexusMobspawner;
    private CustomItemManager customItemManager;
    private LeaderboardManager leaderboardManager;
    private GuiManager guiManager;
    private EffectsManager effectsManager;
    private ModelManager modelManager;
    private ResourcePackGenerator resourcePackGenerator;
    
    @Override
    public void onEnable() {
        setInstance(this);
        
        // Create plugin folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Initialize configuration
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // Load languages
        languageManager = new com.nexusmobs.config.LanguageManager(this);
        
        // Initialize managers (order matters!)
        customItemManager = new CustomItemManager(this);
        modelManager = new ModelManager(this);
        leaderboardManager = new LeaderboardManager(this);
        effectsManager = new EffectsManager(this);
        nexusMobManager = new NexusMobManager(this);
        NexusMobspawner = new NexusMobspawner(this);
        guiManager = new GuiManager(this);
        resourcePackGenerator = new ResourcePackGenerator(this);
        
        // Register listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        // Start spawn scheduler
        if (getConfig().getBoolean("spawn.enabled", true)) {
            NexusMobspawner.scheduleNextSpawn();
        }
        
        // Generate resource pack if configured
        if (getConfig().getBoolean("resourcepack.auto-generate", false)) {
            try {
                File packFile = resourcePackGenerator.generateResourcePack();
                getLogger().info("Resource pack generated: " + packFile.getName());
            } catch (Exception e) {
                getLogger().warning("Failed to generate resource pack: " + e.getMessage());
            }
        }
        
        // Auto-save task
        int saveInterval = getConfig().getInt("auto-save-minutes", 5) * 60 * 20;
        getServer().getScheduler().runTaskTimer(this, () -> {
            leaderboardManager.saveData();
        }, saveInterval, saveInterval);
        
        getLogger().info("========================================");
        getLogger().info("NexusMobs v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Loaded " + configManager.getNexusMobTypes().size() + " Nexus mob types");
        getLogger().info("Loaded " + customItemManager.getCustomItems().size() + " custom items");
        getLogger().info("Loaded " + modelManager.getMobModels().size() + " custom models");
        getLogger().info("========================================");
    }
    
    @Override
    public void onDisable() {
        // Save player data
        if (leaderboardManager != null) {
            leaderboardManager.saveData();
        }
        
        // Clean up active Nexus mobs
        if (nexusMobManager != null) {
            nexusMobManager.cleanup();
        }
        
        // Clean up model armor stands
        if (modelManager != null) {
            modelManager.cleanup();
        }
        
        // Cancel all scheduled tasks
        getServer().getScheduler().cancelTasks(this);
        
        getLogger().info("NexusMobs plugin has been disabled!");
    }
    
    /**
     * Register event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new NexusMobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(
                new NexusMobDamageListener(this), this);
        getServer().getPluginManager().registerEvents(
                new ItemAbilityListener(this, customItemManager), this);
        getServer().getPluginManager().registerEvents(
                new GuiListener(this, guiManager), this);
    }
    
    /**
     * Register commands
     */
    private void registerCommands() {
        NexusMobsCommand command = new NexusMobsCommand(this);
        getCommand("NexusMobs").setExecutor(command);
        getCommand("NexusMobs").setTabCompleter(command);
    }
    
    /**
     * Reload plugin configuration and managers
     */
    public void reload() {
        // Save current data
        leaderboardManager.saveData();
        
        // Reload config
        reloadConfig();
        configManager = new ConfigManager(this);

        // Reload languages and apply selected
        if (languageManager != null) {
            languageManager.loadLanguages();
            languageManager.applyDefault();
        } else {
            languageManager = new com.nexusmobs.config.LanguageManager(this);
        }
        
        // Reload managers
        customItemManager.reload();
        modelManager.reload();
        
        // Refresh active mobs (update names/bossbars) instead of full cleanup to preserve entities
        nexusMobManager.updateAllMobDisplayNames();
        NexusMobspawner.cancelScheduledSpawns();
        
        if (getConfig().getBoolean("spawn.enabled", true)) {
            NexusMobspawner.scheduleNextSpawn();
        }
    }
    
    // ================== GETTERS ==================
    
    public static NexusMobsPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public com.nexusmobs.config.LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public NexusMobManager getNexusMobManager() {
        return nexusMobManager;
    }
    
    public NexusMobspawner getNexusMobspawner() {
        return NexusMobspawner;
    }
    
    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }
    
    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    public EffectsManager getEffectsManager() {
        return effectsManager;
    }
    
    public ModelManager getModelManager() {
        return modelManager;
    }
    
    public ResourcePackGenerator getResourcePackGenerator() {
        return resourcePackGenerator;
    }
}


