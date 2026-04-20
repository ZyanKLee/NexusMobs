package com.nexusmobs.commands;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.leaderboard.PlayerStats;
import com.nexusmobs.loot.CustomItem;
import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.resourcepack.MobModel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles /NexusMobs command and its subcommands
 */
public class NexusMobsCommand implements CommandExecutor, TabCompleter {
    
    private final NexusMobsPlugin plugin;
    
    public NexusMobsCommand(NexusMobsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Show help if no args
        if (args.length == 0) {
            if (sender instanceof Player) {
                // Open GUI for players
                if (sender.hasPermission("NexusMobs.admin")) {
                    plugin.getGuiManager().openMainMenu((Player) sender);
                } else {
                    // Show player stats for non-admins
                    plugin.getGuiManager().openPlayerStats((Player) sender);
                }
            } else {
                sendHelp(sender);
            }
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "help":
                sendHelp(sender);
                return true;
                
            case "reload":
                if (!checkPermission(sender, "NexusMobs.admin")) return true;
                return handleReload(sender);
                
            case "spawn":
                if (!checkPermission(sender, "NexusMobs.admin")) return true;
                return handleSpawn(sender, args);
                
            case "list":
                if (!checkPermission(sender, "NexusMobs.admin")) return true;
                return handleList(sender, args);
                
            case "info":
                return handleInfo(sender);
                
            case "stats":
                return handleStats(sender, args);
                
            case "leaderboard":
            case "top":
                return handleLeaderboard(sender, args);
                
            case "give":
                if (!checkPermission(sender, "NexusMobs.admin")) return true;
                return handleGive(sender, args);
                
            case "menu":
            case "gui":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                plugin.getGuiManager().openMainMenu((Player) sender);
                return true;
                
            case "generatepack":
            case "genpack":
                if (!checkPermission(sender, "NexusMobs.admin")) return true;
                return handleGeneratePack(sender);
                
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("no-permission", null)));
            return false;
        }
        return true;
    }
    
    /**
     * Handle reload subcommand
     */
    private boolean handleReload(CommandSender sender) {
        try {
            plugin.reload();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("reload-success", null)));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error reloading config: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
    
    /**
     * Handle spawn subcommand
     */
    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        String typeId = null;
        
        if (args.length > 1) {
            typeId = args[1];
            
            // Validate type ID
            if (plugin.getConfigManager().getNexusMobType(typeId) == null) {
                sender.sendMessage(ChatColor.RED + "Invalid mob type: " + typeId);
                sender.sendMessage(ChatColor.YELLOW + "Available types: " + 
                        String.join(", ", plugin.getConfigManager().getNexusMobTypes().keySet()));
                return true;
            }
        }
        
        boolean success = plugin.getNexusMobspawner().manualSpawn(player, typeId);
        
        if (success) {
            NexusMobType type = (typeId != null) 
                    ? plugin.getConfigManager().getNexusMobType(typeId)
                    : plugin.getConfigManager().getRandomNexusMobType();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", type.getDisplayName());
            placeholders.put("x", String.valueOf(player.getLocation().getBlockX()));
            placeholders.put("y", String.valueOf(player.getLocation().getBlockY()));
            placeholders.put("z", String.valueOf(player.getLocation().getBlockZ()));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("spawn-manual", placeholders)));
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to spawn Nexus mob!");
        }
        
        return true;
    }
    
    /**
     * Handle list subcommand
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String listType = args[1].toLowerCase();
            
            switch (listType) {
                case "mobs":
                    return listMobs(sender);
                case "items":
                    return listItems(sender);
                case "models":
                    return listModels(sender);
                default:
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /NexusMobs list <mobs|items|models>");
                    return true;
            }
        }
        
        return listMobs(sender);
    }
    
    private boolean listMobs(CommandSender sender) {
        Map<String, NexusMobType> types = plugin.getConfigManager().getNexusMobTypes();
        
        if (types.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No Nexus mob types configured!");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Nexus Mob Types (" + types.size() + ") ===");
        for (Map.Entry<String, NexusMobType> entry : types.entrySet()) {
            NexusMobType type = entry.getValue();
            sender.sendMessage(ChatColor.YELLOW + entry.getKey() + ChatColor.GRAY + " - " + 
                    ChatColor.translateAlternateColorCodes('&', type.getDisplayName()) +
                    ChatColor.GRAY + " (" + type.getBaseEntity().name() + ")");
        }
        
        return true;
    }
    
    private boolean listItems(CommandSender sender) {
        var items = plugin.getCustomItemManager().getCustomItems();
        
        if (items.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No custom items configured!");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Custom Items (" + items.size() + ") ===");
        for (Map.Entry<String, CustomItem> entry : items.entrySet()) {
            CustomItem item = entry.getValue();
            sender.sendMessage(ChatColor.YELLOW + entry.getKey() + ChatColor.GRAY + " - " + 
                    ChatColor.translateAlternateColorCodes('&', item.getDisplayName()) +
                    ChatColor.GRAY + " (CMD: " + item.getCustomModelData() + ")");
        }
        
        return true;
    }
    
    private boolean listModels(CommandSender sender) {
        var models = plugin.getModelManager().getMobModels();
        
        if (models.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No custom models configured!");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Custom Models (" + models.size() + ") ===");
        for (Map.Entry<String, MobModel> entry : models.entrySet()) {
            MobModel model = entry.getValue();
            sender.sendMessage(ChatColor.YELLOW + entry.getKey() + ChatColor.GRAY + 
                    " - Material: " + model.getMaterial().name() + 
                    ", CMD: " + model.getCustomModelData());
        }
        
        return true;
    }
    
    /**
     * Handle info subcommand
     */
    private boolean handleInfo(CommandSender sender) {
        int activeCount = plugin.getNexusMobManager().getActiveNexusMobCount();
        int maxConcurrent = plugin.getConfigManager().getMaxConcurrentElites();
        
        sender.sendMessage(ChatColor.GOLD + "=== NexusMobs Info ===");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + 
                plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Active Nexus Mobs: " + ChatColor.WHITE + 
            activeCount + " / " + maxConcurrent);
        sender.sendMessage(ChatColor.YELLOW + "Configured Types: " + ChatColor.WHITE + 
                plugin.getConfigManager().getNexusMobTypes().size());
        sender.sendMessage(ChatColor.YELLOW + "Custom Items: " + ChatColor.WHITE + 
                plugin.getCustomItemManager().getCustomItems().size());
        sender.sendMessage(ChatColor.YELLOW + "Custom Models: " + ChatColor.WHITE + 
                plugin.getModelManager().getMobModels().size());
        sender.sendMessage(ChatColor.YELLOW + "Total Server Kills: " + ChatColor.WHITE + 
                plugin.getLeaderboardManager().getTotalServerKills());
        sender.sendMessage(ChatColor.YELLOW + "Spawn Interval: " + ChatColor.WHITE + 
                plugin.getConfigManager().getMinSpawnIntervalHours() + "-" +
                plugin.getConfigManager().getMaxSpawnIntervalHours() + " hours");
        
        return true;
    }
    
    /**
     * Handle stats subcommand
     */
    private boolean handleStats(CommandSender sender, String[] args) {
        Player target;
        
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /NexusMobs stats <player>");
            return true;
        }
        
        PlayerStats stats = plugin.getLeaderboardManager().getPlayerStats(target);
        int rank = plugin.getLeaderboardManager().getPlayerRank(target.getUniqueId());
        
        sender.sendMessage(ChatColor.GOLD + "=== " + target.getName() + "'s Stats ===");
        sender.sendMessage(ChatColor.YELLOW + "Rank: " + ChatColor.WHITE + "#" + rank);
        sender.sendMessage(ChatColor.YELLOW + "Total Kills: " + ChatColor.WHITE + stats.getTotalKills());
        sender.sendMessage(ChatColor.YELLOW + "Total Damage: " + ChatColor.WHITE + stats.getTotalDamageDealt());
        sender.sendMessage(ChatColor.YELLOW + "Deaths to Elites: " + ChatColor.WHITE + stats.getDeathsToElites());
        sender.sendMessage(ChatColor.YELLOW + "K/D Ratio: " + ChatColor.WHITE + 
                String.format("%.2f", stats.getKDRatio()));
        
        if (!stats.getKillsByType().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Kills by Type:");
            for (Map.Entry<String, Integer> entry : stats.getKillsByType().entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + entry.getKey() + ": " + 
                        ChatColor.WHITE + entry.getValue());
            }
        }
        
        return true;
    }
    
    /**
     * Handle leaderboard subcommand
     */
    private boolean handleLeaderboard(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.min(limit, 50);
            } catch (NumberFormatException ignored) {}
        }
        
        List<PlayerStats> topPlayers = plugin.getLeaderboardManager().getTopKillers(limit);
        
        sender.sendMessage(ChatColor.GOLD + "=== NexusMobs Leaderboard ===");
        
        int rank = 1;
        for (PlayerStats stats : topPlayers) {
            String prefix;
            switch (rank) {
                case 1: prefix = ChatColor.GOLD + "§l#1 "; break;
                case 2: prefix = ChatColor.WHITE + "§l#2 "; break;
                case 3: prefix = ChatColor.RED + "§l#3 "; break;
                default: prefix = ChatColor.GRAY + "#" + rank + " "; break;
            }
            
            sender.sendMessage(prefix + ChatColor.YELLOW + stats.getPlayerName() + 
                    ChatColor.GRAY + " - " + ChatColor.WHITE + stats.getTotalKills() + " kills");
            rank++;
        }
        
        return true;
    }
    
    /**
     * Handle give subcommand
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /NexusMobs give <item_id> [player]");
            sender.sendMessage(ChatColor.YELLOW + "Available items: " + 
                    String.join(", ", plugin.getCustomItemManager().getCustomItems().keySet()));
            return true;
        }
        
        String itemId = args[1];
        Player target;
        
        if (args.length > 2) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /NexusMobs give <item_id> <player>");
            return true;
        }
        
        ItemStack item = plugin.getCustomItemManager().createItemStack(itemId);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Invalid item ID: " + itemId);
            sender.sendMessage(ChatColor.YELLOW + "Available items: " + 
                    String.join(", ", plugin.getCustomItemManager().getCustomItems().keySet()));
            return true;
        }
        
        target.getInventory().addItem(item);
        
        String itemName = item.getItemMeta().getDisplayName();
        sender.sendMessage(ChatColor.GREEN + "Gave " + itemName + ChatColor.GREEN + " to " + target.getName());
        
        if (target != sender) {
            target.sendMessage(ChatColor.GREEN + "You received " + itemName);
        }
        
        return true;
    }
    
    /**
     * Handle generatepack subcommand
     */
    private boolean handleGeneratePack(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Generating resource pack...");
        
        try {
            File packFile = plugin.getResourcePackGenerator().generateResourcePack();
            String hash = plugin.getResourcePackGenerator().calculateSHA1(packFile);
            
            sender.sendMessage(ChatColor.GREEN + "Resource pack generated successfully!");
            sender.sendMessage(ChatColor.GRAY + "Location: " + packFile.getAbsolutePath());
            sender.sendMessage(ChatColor.GRAY + "SHA-1: " + hash);
            sender.sendMessage(ChatColor.YELLOW + "Remember to add your textures to the pack!");
            sender.sendMessage(ChatColor.YELLOW + "See TEXTURE_INFO.txt in the resourcepack folder.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to generate resource pack: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== NexusMobs Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/NexusMobs" + ChatColor.GRAY + 
                " - Open GUI menu");
        sender.sendMessage(ChatColor.YELLOW + "/NexusMobs help" + ChatColor.GRAY + 
                " - Show this help");
        sender.sendMessage(ChatColor.YELLOW + "/NexusMobs stats [player]" + ChatColor.GRAY + 
                " - View statistics");
        sender.sendMessage(ChatColor.YELLOW + "/NexusMobs top [count]" + ChatColor.GRAY + 
                " - View leaderboard");
        
        if (sender.hasPermission("NexusMobs.admin")) {
            sender.sendMessage(ChatColor.GOLD + "=== Admin Commands ===");
            sender.sendMessage(ChatColor.YELLOW + "/NexusMobs reload" + ChatColor.GRAY + 
                    " - Reload configuration");
                sender.sendMessage(ChatColor.YELLOW + "/NexusMobs spawn [type]" + ChatColor.GRAY + 
                    " - Spawn a Nexus mob");
            sender.sendMessage(ChatColor.YELLOW + "/NexusMobs list <mobs|items|models>" + ChatColor.GRAY + 
                    " - List configurations");
            sender.sendMessage(ChatColor.YELLOW + "/NexusMobs info" + ChatColor.GRAY + 
                    " - Show plugin information");
            sender.sendMessage(ChatColor.YELLOW + "/NexusMobs give <item> [player]" + ChatColor.GRAY + 
                    " - Give custom item");
            sender.sendMessage(ChatColor.YELLOW + "/NexusMobs generatepack" + ChatColor.GRAY + 
                    " - Generate resource pack");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList("help", "stats", "top", "menu"));
            
            if (sender.hasPermission("NexusMobs.admin")) {
                subcommands.addAll(Arrays.asList("reload", "spawn", "list", "info", "give", "generatepack"));
            }
            
            return subcommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            
            if (sub.equals("spawn") && sender.hasPermission("NexusMobs.admin")) {
                return new ArrayList<>(plugin.getConfigManager().getNexusMobTypes().keySet())
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            
            if (sub.equals("list") && sender.hasPermission("NexusMobs.admin")) {
                return Arrays.asList("mobs", "items", "models").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            
            if (sub.equals("give") && sender.hasPermission("NexusMobs.admin")) {
                return new ArrayList<>(plugin.getCustomItemManager().getCustomItems().keySet())
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            
            if (sub.equals("stats")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("NexusMobs.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
}


