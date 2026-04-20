package com.nexusmobs.gui;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.loot.CustomItem;
import com.nexusmobs.models.NexusMobType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

/**
 * Handles GUI click events
 */
public class GuiListener implements Listener {
    
    private final NexusMobsPlugin plugin;
    private final GuiManager guiManager;
    
    public GuiListener(NexusMobsPlugin plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (!guiManager.isPluginGui(title)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        switch (title) {
            case GuiManager.MAIN_MENU_TITLE:
                handleMainMenuClick(player, displayName);
                break;
            case GuiManager.SPAWN_MENU_TITLE:
                handleSpawnMenuClick(player, clicked);
                break;
            case GuiManager.LEADERBOARD_TITLE:
            case GuiManager.PLAYER_STATS_TITLE:
                if (displayName.equals("Back")) {
                    guiManager.openMainMenu(player);
                }
                break;
            case GuiManager.ITEMS_MENU_TITLE:
                handleItemsMenuClick(player, clicked, displayName);
                break;
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (guiManager.isPluginGui(title)) {
            event.setCancelled(true);
        }
    }
    
    private void handleMainMenuClick(Player player, String displayName) {
        switch (displayName) {
            case "Spawn Elite Mob":
                guiManager.openSpawnMenu(player);
                break;
            case "Leaderboard":
                guiManager.openLeaderboard(player);
                break;
            case "Your Statistics":
                guiManager.openPlayerStats(player);
                break;
            case "Custom Items":
                guiManager.openItemsMenu(player);
                break;
        }
    }
    
    private void handleSpawnMenuClick(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        
        if (displayName.equals("Back")) {
            guiManager.openMainMenu(player);
            return;
        }
        
        // Find the elite mob type by display name
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                if (line.startsWith("§7ID: §f")) {
                    String typeId = ChatColor.stripColor(line).replace("ID: ", "");
                    
                    boolean success = plugin.getNexusMobspawner().manualSpawn(player, typeId);
                    if (success) {
                        player.closeInventory();
                        player.sendMessage("§aElite mob spawned successfully!");
                    } else {
                        player.sendMessage("§cFailed to spawn elite mob!");
                    }
                    return;
                }
            }
        }
    }
    
    private void handleItemsMenuClick(Player player, ItemStack clicked, String displayName) {
        if (displayName.equals("Back")) {
            guiManager.openMainMenu(player);
            return;
        }
        
        if (displayName.equals("Custom Elite Items")) {
            return; // Header item
        }
        
        // Try to find and give the custom item
        String itemId = plugin.getCustomItemManager().getCustomItemId(clicked);
        if (itemId != null) {
            ItemStack item = plugin.getCustomItemManager().createItemStack(itemId);
            if (item != null) {
                player.getInventory().addItem(item);
                player.sendMessage("§aYou received: " + clicked.getItemMeta().getDisplayName());
            }
        }
    }
}


