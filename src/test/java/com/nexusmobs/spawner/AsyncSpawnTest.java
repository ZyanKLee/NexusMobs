package com.nexusmobs.spawner;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.block.BlockMock;
import org.mockbukkit.mockbukkit.world.ChunkMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for #16: the spawner must not load (or generate) chunks 800-1200 blocks
 * away synchronously on the main thread.
 */
class AsyncSpawnTest {

    private ServerMock server;
    private ChunkLoadRecordingWorld world;
    private NexusMobsPlugin plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = new ChunkLoadRecordingWorld();
        server.addWorld(world);
        plugin = MockBukkit.load(NexusMobsPlugin.class);
        plugin.getConfig().set("spawn.worlds", List.of(world.getName()));
        plugin.getConfig().set("spawn.cooldown-hours", 0.0);
        // one zombie type, so the randomly picked type is always one MockBukkit can spawn
        plugin.getConfig().set("elite-mobs", null);
        plugin.getConfig().set("elite-mobs.TestZombie.base-entity", "ZOMBIE");
        plugin.saveConfig();
        plugin.reload();
        player = server.addPlayer();
        player.teleport(new Location(world, 0.5, 65, 0.5));
        world.syncChunkLoads = 0; // only count what the spawner does
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void spawnAttemptDoesNotLoadChunksOnTheMainThread() {
        plugin.getNexusMobspawner().attemptSpawn();

        assertEquals(0, world.syncChunkLoads, "chunks far from players must be loaded asynchronously");
        assertEquals(1, world.pending.size(), "one async chunk request per attempt");
    }

    @Test
    void mobSpawnsOnceTheChunkHasLoaded() {
        plugin.getNexusMobspawner().attemptSpawn();
        assertFalse(farMobExists(), "nothing may spawn before the chunk is there");

        world.completeAll();

        assertTrue(farMobExists(), "the mob spawns in the loaded chunk, 800-1200 blocks away");
        assertEquals(0, world.syncChunkLoads);
    }

    @Test
    void unsuitableLocationTriesTheNextCandidate() {
        world.solidUpTo = -1; // only air: no safe Y anywhere

        plugin.getNexusMobspawner().attemptSpawn();
        int attempts = 0;
        while (!world.pending.isEmpty() && attempts < 1000) {
            world.completeAll();
            attempts++;
        }

        assertEquals(plugin.getConfigManager().getMaxSpawnAttempts(), world.requested,
                "every attempt requests its own chunk asynchronously");
        assertFalse(farMobExists());
    }

    @Test
    void secondAttemptWhileSearchingDoesNotStartAnotherSearch() {
        plugin.getNexusMobspawner().attemptSpawn();
        plugin.getNexusMobspawner().attemptSpawn();

        assertEquals(1, world.pending.size());
    }

    // ---------------------------------------------------------------- helpers

    private boolean farMobExists() {
        for (LivingEntity entity : world.getLivingEntities()) {
            if (!(entity instanceof Player) && entity.getLocation().distance(player.getLocation()) >= 700) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stone up to y=64, air above. Counts synchronous {@code getChunkAt(x, z)} calls and holds
     * back {@code getChunkAtAsync} until the test completes it (like a chunk still generating).
     */
    static final class ChunkLoadRecordingWorld extends WorldMock {
        int syncChunkLoads;
        int requested;
        int solidUpTo = 64;
        final List<Runnable> pending = new ArrayList<>();

        ChunkLoadRecordingWorld() {
            super(Material.STONE, 64);
        }

        @Override
        public ChunkMock getChunkAt(int x, int z) {
            syncChunkLoads++;
            return super.getChunkAt(x, z);
        }

        @Override
        public CompletableFuture<Chunk> getChunkAtAsync(int x, int z, boolean gen, boolean urgent) {
            requested++;
            CompletableFuture<Chunk> future = new CompletableFuture<>();
            pending.add(() -> future.complete(super.getChunkAt(x, z)));
            return future;
        }

        @Override
        public BlockMock getBlockAt(int x, int y, int z) {
            BlockMock block = super.getBlockAt(x, y, z);
            if (solidUpTo < 0 && block.getType() != Material.AIR) {
                block.setType(Material.AIR);
            }
            return block;
        }

        void completeAll() {
            List<Runnable> now = new ArrayList<>(pending);
            pending.clear();
            now.forEach(Runnable::run);
        }
    }
}
