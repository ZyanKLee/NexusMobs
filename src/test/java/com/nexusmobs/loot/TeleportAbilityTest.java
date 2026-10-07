package com.nexusmobs.loot;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.testutil.EffectlessWorldMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for #9: the TELEPORT ability must never put a player inside a block. */
class TeleportAbilityTest {

    private static final int FLOOR_Y = 64;
    private static final float EAST = -90f; // yaw looking towards +x

    private ServerMock server;
    private WorldMock world;
    private NexusMobsPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = EffectlessWorldMock.addTo(server, "world");
        plugin = MockBukkit.load(NexusMobsPlugin.class);
        // empty room: stone floor at y=64, air above
        for (int x = -3; x <= 20; x++) {
            for (int z = -3; z <= 3; z++) {
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.STONE);
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 6; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ------------------------------------------------------- TeleportTargets

    @Test
    void stopsInFrontOfAWall() {
        wall(5);

        Location target = TeleportTargets.find(eye(0, EAST, 0), 10);

        assertNotNull(target);
        assertEquals(4, target.getBlockX(), "directly in front of the wall at x=5");
        assertEquals(FLOOR_Y + 1, target.getBlockY());
        assertPlayerFits(target);
    }

    @Test
    void standsOnTheFloorItLooksAt() {
        Location target = TeleportTargets.find(eye(0, EAST, 40), 10);

        assertNotNull(target);
        assertEquals(FLOOR_Y + 1, target.getBlockY(), "on top of the floor, not in it");
        assertTrue(target.getBlockX() > 0);
        assertPlayerFits(target);
    }

    @Test
    void lowCeilingIsNotAValidTarget() {
        // only a 1-block high gap from x=3 on: a player does not fit
        for (int x = 3; x <= 20; x++) {
            for (int z = -3; z <= 3; z++) {
                world.getBlockAt(x, FLOOR_Y + 2, z).setType(Material.STONE);
            }
        }

        Location target = TeleportTargets.find(eye(0, EAST, 0), 10);

        assertNotNull(target);
        assertTrue(target.getBlockX() < 3, "must stop before the 1-high gap, was x=" + target.getBlockX());
        assertPlayerFits(target);
    }

    @Test
    void neverIntoLava() {
        for (int x = 2; x <= 20; x++) {
            for (int z = -3; z <= 3; z++) {
                world.getBlockAt(x, FLOOR_Y + 1, z).setType(Material.LAVA);
                world.getBlockAt(x, FLOOR_Y + 2, z).setType(Material.LAVA);
            }
        }

        Location target = TeleportTargets.find(eye(0, EAST, 0), 10);

        assertNotNull(target);
        assertPlayerFits(target);
    }

    @Test
    void noRoomMeansNoTarget() {
        wall(1);

        assertNull(TeleportTargets.find(eye(0, EAST, 0), 10));
    }

    @Test
    void openSpaceUsesTheFullDistance() {
        Location target = TeleportTargets.find(eye(0, EAST, 0), 6);

        assertNotNull(target);
        assertEquals(6, target.getBlockX(), 1);
        assertPlayerFits(target);
    }

    // ------------------------------------------- ability via the item (VoidStaff)

    @Test
    void voidStaffTeleportsInFrontOfTheWall() {
        wall(5);
        PlayerMock player = playerHolding("VoidStaff", EAST, 0);

        rightClick(player);

        assertEquals(4, player.getLocation().getBlockX());
        assertEquals(FLOOR_Y + 1, player.getLocation().getBlockY());
        assertPlayerFits(player.getLocation());
    }

    @Test
    void failedTeleportKeepsPlayerAndCostsNoCooldown() {
        wall(1);
        PlayerMock player = playerHolding("VoidStaff", EAST, 0);
        Location before = player.getLocation();

        rightClick(player);

        assertEquals(before, player.getLocation());
        assertTrue(plugin.getCustomItemManager().canUseAbility(player, ItemAbilityType.TELEPORT),
                "a failed teleport must not start the cooldown");
    }

    // ---------------------------------------------------------------- helpers

    private void wall(int x) {
        for (int z = -3; z <= 3; z++) {
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 6; y++) {
                world.getBlockAt(x, y, z).setType(Material.STONE);
            }
        }
    }

    private Location eye(double x, float yaw, float pitch) {
        return new Location(world, x + 0.5, FLOOR_Y + 1 + 1.62, 0.5, yaw, pitch);
    }

    private PlayerMock playerHolding(String itemId, float yaw, float pitch) {
        PlayerMock player = server.addPlayer();
        player.teleport(new Location(world, 0.5, FLOOR_Y + 1, 0.5, yaw, pitch));
        ItemStack item = plugin.getCustomItemManager().createItemStack(itemId);
        assertNotNull(item, itemId);
        player.getInventory().setItemInMainHand(item);
        return player;
    }

    private void rightClick(PlayerMock player) {
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR,
                player.getInventory().getItemInMainHand(), null, BlockFace.SELF, EquipmentSlot.HAND));
    }

    private static void assertPlayerFits(Location feet) {
        Block feetBlock = feet.getBlock();
        Block headBlock = feetBlock.getRelative(BlockFace.UP);
        assertTrue(!feetBlock.getType().isSolid() && !feetBlock.isLiquid(), "feet inside " + feetBlock.getType());
        assertTrue(!headBlock.getType().isSolid() && !headBlock.isLiquid(), "head inside " + headBlock.getType());
        assertTrue(feet.getBlockY() > FLOOR_Y, "below the floor");
    }
}
