package com.nexusmobs.loot;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where custom items come from (#28): config_items.yml, overridden per id by config.yml. */
class CustomItemSourcesTest {

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
    void configItemsFileIsWrittenToDataFolderAndReReadOnReload() throws Exception {
        File file = new File(plugin.getDataFolder(), "config_items.yml");
        assertTrue(file.isFile(), "config_items.yml should be saved to the data folder");

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("custom-items.TestWand.material", "STICK");
        yaml.set("custom-items.TestWand.display-name", "&fTest Wand");
        yaml.save(file);
        items.reload();

        CustomItem wand = items.getCustomItem("TestWand");
        assertNotNull(wand, "edits to the data folder copy must be picked up by reload");
        assertEquals(Material.STICK, wand.getMaterial());
    }

    @Test
    void customItemsInConfigYmlOverrideConfigItemsYml() {
        int before = items.getCustomItems().size();
        assertEquals(Material.NETHERITE_SWORD, items.getCustomItem("BruteSlayer").getMaterial());

        // existing installs still have the old custom-items section in their config.yml
        plugin.getConfig().set("custom-items.BruteSlayer.material", "WOODEN_SWORD");
        items.reload();

        assertEquals(Material.WOODEN_SWORD, items.getCustomItem("BruteSlayer").getMaterial());
        assertEquals(before, items.getCustomItems().size(), "override must not drop the other items");
    }
}
