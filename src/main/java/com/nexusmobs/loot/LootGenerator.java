package com.nexusmobs.loot;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.models.LootDrop;
import com.nexusmobs.models.NexusMobType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates the loot of a killed elite mob.
 *
 * <ul>
 *   <li>Every entry of the mob's {@code drops:} list is rolled with its own chance and amount.
 *       An entry is either a vanilla material or a custom item id.</li>
 *   <li>Then, with probability {@code loot.global-custom-item-chance}, ONE custom item from the
 *       global pool drops, picked weighted by its {@code drop-chance}. Custom items that appear in
 *       any mob's {@code drops:} are mob-exclusive and not part of the global pool (#28, #5).</li>
 * </ul>
 */
public class LootGenerator {

    /** Default for loot.global-custom-item-chance: the average of the previous roll-every-item logic. */
    public static final double DEFAULT_GLOBAL_CUSTOM_ITEM_CHANCE = 0.67;

    private final NexusMobsPlugin plugin;
    private final Random random;

    public LootGenerator(NexusMobsPlugin plugin, Random random) {
        this.plugin = plugin;
        this.random = random;
    }

    public List<ItemStack> generate(NexusMobType type) {
        List<ItemStack> loot = new ArrayList<>();

        for (LootDrop drop : type.getDrops()) {
            if (random.nextDouble() > drop.getChance()) {
                continue;
            }

            int amount = drop.getMinAmount();
            if (drop.getMaxAmount() > drop.getMinAmount()) {
                amount += random.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
            }

            if (drop.isCustomItem()) {
                ItemStack item = plugin.getCustomItemManager().createItemStack(drop.getCustomItemId());
                if (item != null) {
                    addStacks(loot, item, amount);
                    plugin.getLogger().info("Custom item dropped: " + drop.getCustomItemId());
                }
            } else {
                addStacks(loot, new ItemStack(drop.getMaterial()), amount);
            }
        }

        String globalItem = rollGlobalCustomItem();
        if (globalItem != null) {
            ItemStack item = plugin.getCustomItemManager().createItemStack(globalItem);
            if (item != null) {
                loot.add(item);
                plugin.getLogger().info("Custom item dropped: " + globalItem);
            }
        }

        return loot;
    }

    /**
     * @return the id of the custom item dropped from the global pool, or null if none drops
     */
    String rollGlobalCustomItem() {
        double chance = plugin.getConfig().getDouble("loot.global-custom-item-chance", DEFAULT_GLOBAL_CUSTOM_ITEM_CHANCE);
        if (random.nextDouble() >= chance) {
            return null;
        }

        Set<String> mobExclusive = plugin.getConfigManager().getMobExclusiveCustomItems();
        List<Map.Entry<String, CustomItem>> pool = new ArrayList<>();
        double totalWeight = 0;
        for (Map.Entry<String, CustomItem> entry : plugin.getCustomItemManager().getCustomItems().entrySet()) {
            if (entry.getValue().getDropChance() > 0 && !mobExclusive.contains(entry.getKey())) {
                pool.add(entry);
                totalWeight += entry.getValue().getDropChance();
            }
        }
        if (pool.isEmpty()) {
            return null;
        }

        double pick = random.nextDouble() * totalWeight;
        for (Map.Entry<String, CustomItem> entry : pool) {
            pick -= entry.getValue().getDropChance();
            if (pick < 0) {
                return entry.getKey();
            }
        }
        return pool.get(pool.size() - 1).getKey(); // floating point remainder
    }

    /** Add {@code amount} items, split into stacks that respect the item's max stack size. */
    private static void addStacks(List<ItemStack> loot, ItemStack prototype, int amount) {
        int maxStack = Math.max(1, prototype.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = prototype.clone();
            stack.setAmount(Math.min(remaining, maxStack));
            loot.add(stack);
            remaining -= stack.getAmount();
        }
    }
}
