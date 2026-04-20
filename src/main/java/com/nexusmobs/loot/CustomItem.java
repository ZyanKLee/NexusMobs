package com.nexusmobs.loot;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a custom item that can drop from elite mobs
 */
public class CustomItem {
    
    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final int customModelData;
    private final boolean unbreakable;
    private final List<ItemAbility> abilities;
    private final double dropChance;
    
    public CustomItem(String id, Material material, String displayName, List<String> lore,
                      Map<Enchantment, Integer> enchantments, int customModelData,
                      boolean unbreakable, List<ItemAbility> abilities, double dropChance) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.enchantments = enchantments;
        this.customModelData = customModelData;
        this.unbreakable = unbreakable;
        this.abilities = abilities;
        this.dropChance = dropChance;
    }
    
    /**
     * Create the ItemStack with all properties applied
     */
    public ItemStack createItemStack(NamespacedKey itemKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) return item;
        
        // Set display name
        meta.setDisplayName(displayName.replace("&", "§"));
        
        // Set lore
        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(line.replace("&", "§"));
            }
            meta.setLore(coloredLore);
        }
        
        // Apply enchantments
        if (enchantments != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        
        // Set custom model data (for resource pack textures)
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        // Set unbreakable
        if (unbreakable) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        
        // Hide enchants if item has custom abilities
        if (abilities != null && !abilities.isEmpty()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Store custom item ID in persistent data
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
        
        item.setItemMeta(meta);
        return item;
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
    
    public int getCustomModelData() {
        return customModelData;
    }
    
    public boolean isUnbreakable() {
        return unbreakable;
    }
    
    public List<ItemAbility> getAbilities() {
        return abilities;
    }
    
    public double getDropChance() {
        return dropChance;
    }
}

