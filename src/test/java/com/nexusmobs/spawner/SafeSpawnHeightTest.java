package com.nexusmobs.spawner;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Regression tests for #11: the safe-Y scan must not read blocks outside the world. */
class SafeSpawnHeightTest {

    private ServerMock server;
    private NexusMobsPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(NexusMobsPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void findsTheSurface() {
        WorldMock world = world(Material.STONE, 64);

        Location spawn = spawner().findSafeY(world, 10, 10);

        assertNotNull(spawn);
        assertEquals(65, spawn.getBlockY());
    }

    @Test
    void columnSolidUpToTheTopHasNoSpotAndDoesNotReadAboveTheWorld() {
        WorldMock world = world(Material.STONE, 64);
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            world.getBlockAt(3, y, 3).setType(Material.STONE);
        }

        assertNull(spawner().findSafeY(world, 3, 3)); // MockBukkit throws for y >= max height
    }

    @Test
    void emptyColumnHasNoSpotAndDoesNotReadBelowTheWorld() {
        WorldMock world = world(Material.STONE, 64);
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            world.getBlockAt(5, y, 5).setType(Material.AIR);
        }

        assertNull(spawner().findSafeY(world, 5, 5)); // MockBukkit throws for y < min height
    }

    private WorldMock world(Material ground, int height) {
        WorldMock world = new WorldMock(ground, height);
        server.addWorld(world);
        return world;
    }

    private NexusMobspawner spawner() {
        return plugin.getNexusMobspawner();
    }
}
