package com.nexusmobs.loot;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.models.LootDrop;
import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.models.Phase;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for custom item drops (#28) and the global custom item roll (#5). */
class LootGeneratorTest {

    private static final String GLOBAL_CHANCE = "loot.global-custom-item-chance";

    private NexusMobsPlugin plugin;
    private CustomItemManager items;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(NexusMobsPlugin.class);
        items = plugin.getCustomItemManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void customItemDropEntriesProduceTaggedCustomItems() {
        plugin.getConfig().set(GLOBAL_CHANCE, 0.0);
        NexusMobType type = typeWithDrops(
                LootDrop.customItem("ASH_PLATE", 2, 2, 1.0),
                new LootDrop(Material.COAL_BLOCK, 3, 3, 1.0));

        List<ItemStack> loot = new LootGenerator(plugin, new Random(1)).generate(type);

        int ashPlates = loot.stream().filter(i -> "ASH_PLATE".equals(items.getCustomItemId(i)))
                .mapToInt(ItemStack::getAmount).sum();
        int coal = loot.stream().filter(i -> i.getType() == Material.COAL_BLOCK && !items.isCustomItem(i))
                .mapToInt(ItemStack::getAmount).sum();
        assertEquals(2, ashPlates);
        assertEquals(3, coal);
    }

    @Test
    void nonStackableCustomItemsAreSplitIntoSingleStacks() {
        plugin.getConfig().set(GLOBAL_CHANCE, 0.0);
        ItemStack prototype = items.createItemStack("BruteSlayer");
        assertNotNull(prototype);
        assertEquals(1, prototype.getMaxStackSize(), "test needs a non-stackable item");

        List<ItemStack> loot = new LootGenerator(plugin, new Random(1))
                .generate(typeWithDrops(LootDrop.customItem("BruteSlayer", 3, 3, 1.0)));

        assertEquals(3, loot.size());
        assertTrue(loot.stream().allMatch(i -> i.getAmount() == 1 && "BruteSlayer".equals(items.getCustomItemId(i))));
    }

    @Test
    void unknownCustomItemDropIsSkipped() {
        plugin.getConfig().set(GLOBAL_CHANCE, 0.0);

        List<ItemStack> loot = new LootGenerator(plugin, new Random(1))
                .generate(typeWithDrops(LootDrop.customItem("DOES_NOT_EXIST", 1, 1, 1.0)));

        assertEquals(List.of(), loot);
    }

    @Test
    void globalRollNeverPicksMobExclusiveItems() {
        plugin.getConfig().set(GLOBAL_CHANCE, 1.0);
        Set<String> exclusive = plugin.getConfigManager().getMobExclusiveCustomItems();
        assertTrue(exclusive.contains("ASH_PLATE"), "bundled config should make ASH_PLATE mob-exclusive");
        LootGenerator generator = new LootGenerator(plugin, new Random(7));

        for (int i = 0; i < 5_000; i++) {
            String id = generator.rollGlobalCustomItem();
            assertNotNull(id, "chance 1.0 must always drop");
            assertFalse(exclusive.contains(id), id + " is mob-exclusive but came from the global roll");
        }
    }

    @Test
    void globalRollDropsAtMostOneItemPerKill() {
        plugin.getConfig().set(GLOBAL_CHANCE, 1.0);
        LootGenerator generator = new LootGenerator(plugin, new Random(3));

        for (int i = 0; i < 1_000; i++) {
            List<ItemStack> loot = generator.generate(typeWithDrops());
            assertEquals(1, loot.size());
            assertTrue(items.isCustomItem(loot.get(0)));
        }
    }

    @Test
    void globalChanceZeroDropsNothing() {
        plugin.getConfig().set(GLOBAL_CHANCE, 0.0);
        LootGenerator generator = new LootGenerator(plugin, new Random(5));

        for (int i = 0; i < 1_000; i++) {
            assertNull(generator.rollGlobalCustomItem());
        }
    }

    @Test
    void bundledRateMatchesThePreviousAverage() {
        assertEquals(LootGenerator.DEFAULT_GLOBAL_CUSTOM_ITEM_CHANCE, plugin.getConfig().getDouble(GLOBAL_CHANCE), 1e-9,
                "bundled config.yml and code default should agree");
        LootGenerator generator = new LootGenerator(plugin, new Random(42));
        int kills = 20_000;
        int dropped = 0;
        for (int i = 0; i < kills; i++) {
            if (generator.rollGlobalCustomItem() != null) {
                dropped++;
            }
        }
        assertEquals(LootGenerator.DEFAULT_GLOBAL_CUSTOM_ITEM_CHANCE, dropped / (double) kills, 0.02);
    }

    private static NexusMobType typeWithDrops(LootDrop... drops) {
        return new NexusMobType("loot_test", EntityType.ZOMBIE, "Loot Test", 100.0, 5.0, 0.0, false,
                Collections.emptyList(), List.of(drops), null,
                List.of(new Phase(50.0, 1.25, 2.0, Collections.emptyList())));
    }
}
