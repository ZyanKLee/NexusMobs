package com.nexusmobs.gui;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.leaderboard.LeaderboardManager;
import com.nexusmobs.leaderboard.PlayerStats;
import com.nexusmobs.models.NexusMobType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Manages GUI menus for the plugin
 */
public class GuiManager {
    
    private final NexusMobsPlugin plugin;
    
    // GUI identifiers stored in inventory title
    public static final String MAIN_MENU_TITLE = "§6§lElite Mobs Menu";
    public static final String SPAWN_MENU_TITLE = "§6§lSpawn Elite Mob";
    public static final String LEADERBOARD_TITLE = "§6§lLeaderboard";
    public static final String PLAYER_STATS_TITLE = "§6§lYour Statistics";
    public static final String ITEMS_MENU_TITLE = "§6§lCustom Items";
    
    public GuiManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open the main admin menu
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_MENU_TITLE);
        
        // Fill borders with glass
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }
        
        // Spawn Elite Mob
        inv.setItem(10, createItem(Material.ZOMBIE_SPAWN_EGG, 
                "§a§lSpawn Elite Mob",
                "§7Click to manually spawn",
                "§7an elite mob near you"));
        
        // Leaderboard
        inv.setItem(12, createItem(Material.GOLDEN_SWORD, 
                "§e§lLeaderboard",
                "§7View the top elite",
                "§7mob hunters"));
        
        // Your Stats
        inv.setItem(14, createItem(Material.BOOK, 
                "§b§lYour Statistics",
                "§7View your personal",
                "§7elite mob statistics"));
        
        // Custom Items
        inv.setItem(16, createItem(Material.NETHER_STAR, 
                "§d§lCustom Items",
                "§7Browse and give",
                "§7custom elite items"));
        
        // Info
        inv.setItem(22, createItem(Material.PAPER, 
                "§f§lPlugin Info",
                "§7Active Elites: §e" + plugin.getNexusMobManager().getActiveNexusMobCount(),
                "§7Configured Types: §e" + plugin.getConfigManager().getNexusMobTypes().size(),
                "§7Total Server Kills: §e" + plugin.getLeaderboardManager().getTotalServerKills()));
        
        player.openInventory(inv);
    }
    
    /**
     * Open spawn selection menu
     */
    public void openSpawnMenu(Player player) {
        Map<String, NexusMobType> types = plugin.getConfigManager().getNexusMobTypes();
        int size = Math.min(54, ((types.size() / 9) + 1) * 9);
        size = Math.max(9, size);
        
        Inventory inv = Bukkit.createInventory(null, size, SPAWN_MENU_TITLE);
        
        int slot = 0;
        for (Map.Entry<String, NexusMobType> entry : types.entrySet()) {
            if (slot >= size) break;
            
            NexusMobType type = entry.getValue();
            Material eggMaterial = getMobEgg(type.getBaseEntity().name());
            
            inv.setItem(slot, createItem(eggMaterial,
                    type.getDisplayName(),
                    "§7ID: §f" + entry.getKey(),
                    "§7Base: §f" + type.getBaseEntity().name(),
                    "§7Health: §c" + type.getMaxHealth(),
                    "§7Damage: §c" + type.getAttackDamage(),
                    "",
                    "§aClick to spawn!"));
            
            slot++;
        }
        
        // Back button
        inv.setItem(size - 1, createItem(Material.BARRIER, "§c§lBack", "§7Return to main menu"));
        
        player.openInventory(inv);
    }
    
    /**
     * Open leaderboard menu
     */
    public void openLeaderboard(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LEADERBOARD_TITLE);
        
        // Fill top row with decoration
        ItemStack topDecor = createItem(Material.GOLD_INGOT, "§6§lTop Elite Hunters");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, topDecor);
        }
        
        // Get top 10 players
        List<PlayerStats> topPlayers = plugin.getLeaderboardManager().getTopKillers(10);
        
        int slot = 9;
        int rank = 1;
        for (PlayerStats stats : topPlayers) {
            if (slot >= 45) break;
            
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            
            String prefix;
            switch (rank) {
                case 1: prefix = "§6§l#1 "; break;
                case 2: prefix = "§f§l#2 "; break;
                case 3: prefix = "§c§l#3 "; break;
                default: prefix = "§7#" + rank + " "; break;
            }
            
            meta.setDisplayName(prefix + "§e" + stats.getPlayerName());
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Total Kills: §a" + stats.getTotalKills());
            lore.add("§7Total Damage: §c" + stats.getTotalDamageDealt());
            lore.add("§7Deaths: §4" + stats.getDeathsToElites());
            lore.add("§7K/D Ratio: §e" + String.format("%.2f", stats.getKDRatio()));
            lore.add("");
            lore.add("§8Kills by Type:");
            for (Map.Entry<String, Integer> entry : stats.getKillsByType().entrySet()) {
                lore.add("§7- " + entry.getKey() + ": §f" + entry.getValue());
            }
            
            meta.setLore(lore);
            skull.setItemMeta(meta);
            
            inv.setItem(slot, skull);
            slot++;
            rank++;
        }
        
        // Player's own rank
        PlayerStats playerStats = plugin.getLeaderboardManager().getPlayerStats(player);
        int playerRank = plugin.getLeaderboardManager().getPlayerRank(player.getUniqueId());
        
        inv.setItem(49, createItem(Material.DIAMOND,
                "§b§lYour Rank: #" + playerRank,
                "§7Kills: §a" + playerStats.getTotalKills(),
                "§7K/D: §e" + String.format("%.2f", playerStats.getKDRatio())));
        
        // Back button
        inv.setItem(53, createItem(Material.BARRIER, "§c§lBack", "§7Return to main menu"));
        
        player.openInventory(inv);
    }
    
    /**
     * Open player statistics menu
     */
    public void openPlayerStats(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, PLAYER_STATS_TITLE);
        
        PlayerStats stats = plugin.getLeaderboardManager().getPlayerStats(player);
        int rank = plugin.getLeaderboardManager().getPlayerRank(player.getUniqueId());
        
        // Border
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(36 + i, border);
        }
        
        // Player head in center top
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName("§a§l" + player.getName());
        skullMeta.setLore(Arrays.asList(
                "§7Server Rank: §e#" + rank,
                "§7Member since the beginning!"
        ));
        skull.setItemMeta(skullMeta);
        inv.setItem(4, skull);
        
        // Total Kills
        inv.setItem(19, createItem(Material.DIAMOND_SWORD,
                "§a§lTotal Kills",
                "§7You have killed",
                "§f" + stats.getTotalKills() + " §7elite mobs!"));
        
        // Total Damage
        inv.setItem(21, createItem(Material.RED_DYE,
                "§c§lTotal Damage Dealt",
                "§7You have dealt",
                "§f" + stats.getTotalDamageDealt() + " §7damage!"));
        
        // Deaths
        inv.setItem(23, createItem(Material.SKELETON_SKULL,
                "§4§lDeaths to Elites",
                "§7Elite mobs have killed",
                "§7you §f" + stats.getDeathsToElites() + " §7times!"));
        
        // K/D Ratio
        inv.setItem(25, createItem(Material.EXPERIENCE_BOTTLE,
                "§e§lK/D Ratio",
                "§7Your kill/death ratio:",
                "§f" + String.format("%.2f", stats.getKDRatio())));
        
        // Kills by type
        inv.setItem(31, createKillsByTypeItem(stats));
        
        // Back button
        inv.setItem(44, createItem(Material.BARRIER, "§c§lBack", "§7Return to main menu"));
        
        player.openInventory(inv);
    }
    
    /**
     * Open custom items menu
     */
    public void openItemsMenu(Player player) {
        var customItems = plugin.getCustomItemManager().getCustomItems();
        int size = Math.min(54, ((customItems.size() / 9) + 1) * 9 + 9);
        size = Math.max(18, size);
        
        Inventory inv = Bukkit.createInventory(null, size, ITEMS_MENU_TITLE);
        
        // Header
        ItemStack header = createItem(Material.NETHER_STAR, "§d§lCustom Elite Items", 
                "§7Click to receive an item");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, header);
        }
        
        int slot = 9;
        for (Map.Entry<String, ?> entry : customItems.entrySet()) {
            if (slot >= size - 9) break;
            
            ItemStack displayItem = plugin.getCustomItemManager().createItemStack(entry.getKey());
            if (displayItem != null) {
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§a» Click to receive!");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
                inv.setItem(slot, displayItem);
                slot++;
            }
        }
        
        // Back button
        inv.setItem(size - 1, createItem(Material.BARRIER, "§c§lBack", "§7Return to main menu"));
        
        player.openInventory(inv);
    }
    
    // ================== HELPER METHODS ==================
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createKillsByTypeItem(PlayerStats stats) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        if (stats.getKillsByType().isEmpty()) {
            lore.add("§7No kills yet!");
        } else {
            for (Map.Entry<String, Integer> entry : stats.getKillsByType().entrySet()) {
                NexusMobType type = plugin.getConfigManager().getNexusMobType(entry.getKey());
                String displayName = type != null ? type.getDisplayName() : entry.getKey();
                lore.add("§7" + ChatColor.stripColor(displayName.replace("§", "")) + ": §f" + entry.getValue());
            }
        }
        
        return createItem(Material.WRITABLE_BOOK, "§6§lKills by Type", lore.toArray(new String[0]));
    }
    
    private Material getMobEgg(String entityType) {
        try {
            return Material.valueOf(entityType + "_SPAWN_EGG");
        } catch (IllegalArgumentException e) {
            return Material.ZOMBIE_SPAWN_EGG;
        }
    }
    
    /**
     * Check if an inventory is a plugin GUI
     */
    public boolean isPluginGui(String title) {
        return title.equals(MAIN_MENU_TITLE) ||
               title.equals(SPAWN_MENU_TITLE) ||
               title.equals(LEADERBOARD_TITLE) ||
               title.equals(PLAYER_STATS_TITLE) ||
               title.equals(ITEMS_MENU_TITLE);
    }
}


