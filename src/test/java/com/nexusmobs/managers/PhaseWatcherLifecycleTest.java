package com.nexusmobs.managers;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.models.NexusMob;
import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.models.Phase;
import com.nexusmobs.testutil.EffectlessWorldMock;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.LivingEntityMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for the phase watcher task lifecycle (#2).
 *
 * <p>{@code spawnNexusMob()} cannot run end-to-end on MockBukkit 4.20
 * ({@code setRemoveWhenFarAway} is unimplemented), so the watcher is started
 * directly on a mock zombie and the mob is registered via reflection.
 */
class PhaseWatcherLifecycleTest {

    private static final long WATCHER_PERIOD = 40L;

    private ServerMock server;
    private NexusMobsPlugin plugin;
    private WorldMock world;
    private NexusMobManager manager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = EffectlessWorldMock.addTo(server, "world");
        plugin = MockBukkit.load(NexusMobsPlugin.class);
        manager = plugin.getNexusMobManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void watcherCancelsItselfWhenEntityDisappearsWithoutRemoveNexusMob() throws Exception {
        int from = nextTaskId();
        LivingEntity entity = spawnTrackedMobWithWatcher();
        int to = nextTaskId();
        server.getScheduler().performTicks(WATCHER_PERIOD + 5);
        assertEquals(1, queued(from, to).size(), "exactly one watcher task expected while the mob is alive");

        // chunk unload, removal by another plugin, ...: no death event, no removeNexusMob()
        entity.remove();
        server.getScheduler().performTicks(WATCHER_PERIOD * 10);

        assertEquals(Collections.emptySet(), queued(from, to), "watcher task leaked after entity removal");
    }

    @Test
    void removeNexusMobCancelsWatcherImmediately() throws Exception {
        int from = nextTaskId();
        LivingEntity entity = spawnTrackedMobWithWatcher();
        int to = nextTaskId();

        manager.removeNexusMob(entity.getUniqueId());

        assertEquals(Collections.emptySet(), queued(from, to), "watcher task still queued after removeNexusMob()");
    }

    @Test
    void cleanupCancelsAllWatchers() throws Exception {
        int from = nextTaskId();
        spawnTrackedMobWithWatcher();
        spawnTrackedMobWithWatcher();
        int to = nextTaskId();

        manager.cleanup();

        assertEquals(Collections.emptySet(), queued(from, to), "watcher tasks still queued after cleanup()");
    }

    @Test
    void phaseIsAppliedAtHalfHealth() throws Exception {
        LivingEntity entity = spawnTrackedMobWithWatcher();
        entity.setHealth(40.0);
        server.getScheduler().performTicks(WATCHER_PERIOD + 1);

        assertEquals(5.0 * 1.25, entity.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue(), 1e-9);
        assertEquals(2.0, entity.getAttribute(Attribute.ARMOR).getBaseValue(), 1e-9);
    }

    // ---------------------------------------------------------------- helpers

    private static NexusMobType testType() {
        return new NexusMobType("test_mob", EntityType.ZOMBIE, "Test Mob", 100.0, 5.0, 0.0, false,
                Collections.emptyList(), Collections.emptyList(), null,
                List.of(new Phase(50.0, 1.25, 2.0, Collections.emptyList())));
    }

    @SuppressWarnings("unchecked")
    private LivingEntity spawnTrackedMobWithWatcher() throws Exception {
        LivingEntityMock entity = (LivingEntityMock) world.spawnEntity(new Location(world, 0, 70, 0), EntityType.ZOMBIE);
        // MockBukkit 4.20 throws for attributes it does not pre-register; real zombies have them
        entity.registerAttribute(Attribute.ATTACK_DAMAGE);
        entity.registerAttribute(Attribute.ARMOR);
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100.0);
        entity.setHealth(100.0);

        Field active = NexusMobManager.class.getDeclaredField("activeNexusMobs");
        active.setAccessible(true);
        ((Map<UUID, NexusMob>) active.get(manager))
                .put(entity.getUniqueId(), new NexusMob(entity.getUniqueId(), "test_mob", entity.getLocation()));

        Method start = NexusMobManager.class.getDeclaredMethod("startPhaseWatcher", LivingEntity.class, NexusMobType.class);
        start.setAccessible(true);
        start.invoke(manager, entity, testType());
        return entity;
    }

    /** MockBukkit hands out sequential task ids; this returns the next one. */
    private int nextTaskId() {
        BukkitTask probe = server.getScheduler().runTaskLater(plugin, () -> { }, Long.MAX_VALUE / 2);
        probe.cancel();
        return probe.getTaskId() + 1;
    }

    /** Task ids in [from, to) that are still scheduled. ({@code getPendingTasks()} is unimplemented.) */
    private Set<Integer> queued(int from, int to) {
        Set<Integer> ids = new TreeSet<>();
        for (int id = from; id < to; id++) {
            if (server.getScheduler().isQueued(id)) {
                ids.add(id);
            }
        }
        return ids;
    }
}
