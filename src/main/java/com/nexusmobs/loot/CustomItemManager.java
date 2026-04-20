package com.nexusmobs.loot;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Manages custom items and their abilities
 */
public class CustomItemManager {
    
    private final NexusMobsPlugin plugin;
    private final Map<String, CustomItem> customItems;
    private final NamespacedKey customItemKey;
    private final Map<UUID, Map<ItemAbilityType, Long>> cooldowns;
    
    public CustomItemManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.customItems = new HashMap<>();
        this.customItemKey = new NamespacedKey(plugin, "custom_item");
        this.cooldowns = new HashMap<>();
        loadCustomItems();
    }
    
    /**
     * Load custom items from configuration
     */
    private void loadCustomItems() {
        customItems.clear();
        
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("custom-items");
        if (itemsSection == null) {
            plugin.getLogger().info("No custom items configured.");
            return;
        }
        
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;
            
            try {
                CustomItem item = loadCustomItem(key, itemSection);
                customItems.put(key, item);
                plugin.getLogger().info("Loaded custom item: " + key);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load custom item: " + key);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Load a single custom item from configuration
     */
    private CustomItem loadCustomItem(String id, ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("material", "DIAMOND_SWORD").toUpperCase());
        String displayName = section.getString("display-name", "&fUnknown Item");
        List<String> lore = section.getStringList("lore");
        int customModelData = section.getInt("custom-model-data", 0);
        boolean unbreakable = section.getBoolean("unbreakable", false);
        double dropChance = section.getDouble("drop-chance", 0.1);
        
        // Load enchantments
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String enchantName : enchantSection.getKeys(false)) {
                Enchantment enchant = getEnchantment(enchantName);
                if (enchant != null) {
                    enchantments.put(enchant, enchantSection.getInt(enchantName));
                } else {
                    plugin.getLogger().warning("Unknown enchantment: " + enchantName);
                }
            }
        }
        
        // Load abilities
        List<ItemAbility> abilities = new ArrayList<>();
        ConfigurationSection abilitiesSection = section.getConfigurationSection("abilities");
        if (abilitiesSection != null) {
            for (String abilityName : abilitiesSection.getKeys(false)) {
                ConfigurationSection abilitySection = abilitiesSection.getConfigurationSection(abilityName);
                if (abilitySection == null) continue;
                
                try {
                    ItemAbilityType type = ItemAbilityType.valueOf(abilityName.toUpperCase());
                    double value = abilitySection.getDouble("value", 0);
                    double chance = abilitySection.getDouble("chance", 1.0);
                    int cooldown = abilitySection.getInt("cooldown-ticks", 0);
                    int duration = abilitySection.getInt("duration-ticks", 100);
                    
                    abilities.add(new ItemAbility(type, value, chance, cooldown, duration));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown ability type: " + abilityName);
                }
            }
        }
        
        // Language overrides: display name and lore can be provided in language files
        if (plugin.getLanguageManager() != null) {
            String kebab = id.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
            String langDisplayKey1 = "custom-items." + id + ".display-name";
            String langDisplayKey2 = "items." + id + ".display-name";
            String langDisplayKey3 = "custom-items." + kebab + ".display-name";
            String langDisplayKey4 = "items." + kebab + ".display-name";
            if (plugin.getLanguageManager().hasKey(langDisplayKey1)) {
                displayName = plugin.getLanguageManager().getString(langDisplayKey1, displayName);
            } else if (plugin.getLanguageManager().hasKey(langDisplayKey2)) {
                displayName = plugin.getLanguageManager().getString(langDisplayKey2, displayName);
            } else if (plugin.getLanguageManager().hasKey(langDisplayKey3)) {
                displayName = plugin.getLanguageManager().getString(langDisplayKey3, displayName);
            } else if (plugin.getLanguageManager().hasKey(langDisplayKey4)) {
                displayName = plugin.getLanguageManager().getString(langDisplayKey4, displayName);
            }

            String langLoreKey1 = "custom-items." + id + ".lore";
            String langLoreKey2 = "items." + id + ".lore";
            String langLoreKey3 = "custom-items." + kebab + ".lore";
            String langLoreKey4 = "items." + kebab + ".lore";
            if (plugin.getLanguageManager().hasKey(langLoreKey1)) {
                lore = plugin.getLanguageManager().getStringList(langLoreKey1);
            } else if (plugin.getLanguageManager().hasKey(langLoreKey2)) {
                lore = plugin.getLanguageManager().getStringList(langLoreKey2);
            } else if (plugin.getLanguageManager().hasKey(langLoreKey3)) {
                lore = plugin.getLanguageManager().getStringList(langLoreKey3);
            } else if (plugin.getLanguageManager().hasKey(langLoreKey4)) {
                lore = plugin.getLanguageManager().getStringList(langLoreKey4);
            }
        }

        // Add ability descriptions to lore (localized if possible)
        if (!abilities.isEmpty()) {
            if (lore == null) lore = new ArrayList<>();
            lore.add("");
            lore.add("&6Special Abilities:");
            for (ItemAbility ability : abilities) {
                String abilityName = ability.getType().getDisplayName();
                if (plugin.getLanguageManager() != null) {
                    String ak = "ability." + ability.getType().name() + ".name";
                    if (plugin.getLanguageManager().hasKey(ak)) {
                        abilityName = plugin.getLanguageManager().getString(ak, abilityName);
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("§9✦ ").append(abilityName);
                if (ability.getValue() > 0) {
                    if (ability.getType() == ItemAbilityType.LIFESTEAL ||
                            ability.getType() == ItemAbilityType.DAMAGE_RESISTANCE ||
                            ability.getType() == ItemAbilityType.CRITICAL_BOOST) {
                        sb.append(" §7(").append((int) (ability.getValue() * 100)).append("%)");
                    } else {
                        sb.append(" §7(").append(ability.getValue()).append(")");
                    }
                }

                if (ability.getChance() < 1.0) {
                    sb.append(" §8[").append((int) (ability.getChance() * 100)).append("% chance]");
                }

                lore.add(sb.toString());
            }
        }
        
        return new CustomItem(id, material, displayName, lore, enchantments, 
                customModelData, unbreakable, abilities, dropChance);
    }
    
    /**
     * Get Enchantment by name (compatible with Paper 1.21+)
     */
    private Enchantment getEnchantment(String name) {
        if (name == null) return null;
        
        // Try to get from Registry (Paper 1.21+)
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return Registry.ENCHANTMENT.get(key);
        } catch (Exception e) {
            // Fallback for older API
            try {
                return Enchantment.getByName(name.toUpperCase());
            } catch (Exception e2) {
                return null;
            }
        }
    }
    
    /**
     * Get all custom items
     */
    public Map<String, CustomItem> getCustomItems() {
        return Collections.unmodifiableMap(customItems);
    }
    
    /**
     * Get a custom item by ID
     */
    public CustomItem getCustomItem(String id) {
        return customItems.get(id);
    }
    
    /**
     * Create an ItemStack from a custom item ID
     */
    public ItemStack createItemStack(String id) {
        CustomItem customItem = customItems.get(id);
        if (customItem == null) return null;
        
        return customItem.createItemStack(customItemKey);
    }
    
    /**
     * Check if an ItemStack is a custom item
     */
    public boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(customItemKey, PersistentDataType.STRING);
    }
    
    /**
     * Get the custom item ID from an ItemStack
     */
    public String getCustomItemId(ItemStack item) {
        if (!isCustomItem(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
    }
    
    /**
     * Get abilities for an item
     */
    public List<ItemAbility> getAbilities(ItemStack item) {
        String id = getCustomItemId(item);
        if (id == null) return Collections.emptyList();
        
        CustomItem customItem = customItems.get(id);
        if (customItem == null) return Collections.emptyList();
        
        return customItem.getAbilities();
    }
    
    /**
     * Check if a player can use an ability (cooldown check)
     */
    public boolean canUseAbility(Player player, ItemAbilityType abilityType) {
        Map<ItemAbilityType, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return true;
        
        Long lastUse = playerCooldowns.get(abilityType);
        if (lastUse == null) return true;
        
        return System.currentTimeMillis() >= lastUse;
    }
    
    /**
     * Set cooldown for an ability
     */
    public void setCooldown(Player player, ItemAbilityType abilityType, int cooldownTicks) {
        long cooldownEnd = System.currentTimeMillis() + (cooldownTicks * 50L); // Convert ticks to ms
        
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(abilityType, cooldownEnd);
    }
    
    /**
     * Get remaining cooldown in seconds
     */
    public int getRemainingCooldown(Player player, ItemAbilityType abilityType) {
        Map<ItemAbilityType, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        
        Long lastUse = playerCooldowns.get(abilityType);
        if (lastUse == null) return 0;
        
        long remaining = lastUse - System.currentTimeMillis();
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }
    
    /**
     * Clean up player cooldowns on logout
     */
    public void cleanupPlayer(UUID playerUUID) {
        cooldowns.remove(playerUUID);
    }
    
    /**
     * Reload custom items from config
     */
    public void reload() {
        loadCustomItems();
    }
}

