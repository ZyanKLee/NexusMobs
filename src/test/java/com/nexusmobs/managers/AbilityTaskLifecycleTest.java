package com.nexusmobs.managers;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.abilities.AbilityManager;
import com.nexusmobs.models.NexusMob;
import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.models.Phase;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the ability task lifecycle (#21) and the cleanup done by
 * {@link NexusMobManager#getActiveNexusMobCount()} (#3).
 */
class AbilityTaskLifecycleTest {

    private static final int INTERVAL = 20;

    // chance 0: the summon task never actually spawns (WorldMock.spawnParticle is unimplemented)
    private static final String ABILITIES = """
            aoe-damage:
              enabled: true
              damage: 1.0
              radius: 3.0
              interval-ticks: %d
            summon-minions:
              enabled: true
              mob-type: ZOMBIE
              count: 1
              chance: 0.0
              interval-ticks: %d
            """.formatted(INTERVAL, INTERVAL);

    private ServerMock server;
    private NexusMobsPlugin plugin;
    private WorldMock world;
    private NexusMobManager manager;
    private AbilityManager abilities;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.load(NexusMobsPlugin.class);
        manager = plugin.getNexusMobManager();
        abilities = manager.getAbilityManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ------------------------------------------------------------------ #21

    @Test
    void abilityTasksCancelThemselvesWhenEntityDisappears() throws Exception {
        int from = nextTaskId();
        LivingEntity entity = spawnZombie();
        abilities.startAbilities(entity, typeWithAbilities());
        int to = nextTaskId();
        server.getScheduler().performTicks(INTERVAL + 1);
        assertEquals(2, queued(from, to).size(), "aoe-damage and summon-minions tasks expected");

        // no death event, nobody calls stopAbilities()
        entity.remove();
        server.getScheduler().performTicks(INTERVAL * 3L);

        assertEquals(Collections.emptySet(), queued(from, to), "ability tasks leaked after entity removal");
    }

    @Test
    void stopAbilitiesStillCancelsImmediately() throws Exception {
        int from = nextTaskId();
        LivingEntity entity = spawnZombie();
        abilities.startAbilities(entity, typeWithAbilities());
        int to = nextTaskId();

        abilities.stopAbilities(entity.getUniqueId());

        assertEquals(Collections.emptySet(), queued(from, to));
    }

    @Test
    void abilitiesKeepRunningWhileEntityIsAlive() throws Exception {
        int from = nextTaskId();
        LivingEntity entity = spawnZombie();
        abilities.startAbilities(entity, typeWithAbilities());
        int to = nextTaskId();

        server.getScheduler().performTicks(INTERVAL * 5L);

        assertTrue(entity.isValid());
        assertEquals(2, queued(from, to).size());
    }

    // ------------------------------------------------------------------- #3

    @Test
    void activeCountCleansUpStaleMobsCompletely() throws Exception {
        int from = nextTaskId();
        LivingEntity entity = spawnZombie();
        track(entity);
        abilities.startAbilities(entity, typeWithAbilities());
        startPhaseWatcher(entity, typeWithAbilities());
        int to = nextTaskId();
        assertEquals(3, queued(from, to).size(), "2 ability tasks + phase watcher expected");

        entity.remove();
        int count = manager.getActiveNexusMobCount(); // no ticks: cleanup must not depend on task self-cancel

        assertEquals(0, count);
        assertEquals(Collections.emptySet(), queued(from, to),
                "getActiveNexusMobCount() dropped the mob without stopping its tasks");
    }

    @Test
    void activeCountKeepsLiveMobs() throws Exception {
        LivingEntity alive = spawnZombie();
        LivingEntity gone = spawnZombie();
        track(alive);
        track(gone);

        gone.remove();

        assertEquals(1, manager.getActiveNexusMobCount());
        assertFalse(manager.getActiveNexusMobs().isEmpty());
        assertEquals(alive.getUniqueId(), manager.getActiveNexusMobs().iterator().next().getEntityUUID());
    }

    // -------------------------------------------------------------- helpers

    private NexusMobType typeWithAbilities() throws InvalidConfigurationException {
        YamlConfiguration abilitiesConfig = new YamlConfiguration();
        abilitiesConfig.loadFromString(ABILITIES);
        return new NexusMobType("test_mob", EntityType.ZOMBIE, "Test Mob", 100.0, 5.0, 0.0, false,
                Collections.emptyList(), Collections.emptyList(), abilitiesConfig,
                List.of(new Phase(50.0, 1.25, 2.0, Collections.emptyList())));
    }

    private LivingEntity spawnZombie() {
        LivingEntityMock entity = (LivingEntityMock) world.spawnEntity(new Location(world, 0, 70, 0), EntityType.ZOMBIE);
        entity.registerAttribute(Attribute.ATTACK_DAMAGE);
        entity.registerAttribute(Attribute.ARMOR);
        return entity;
    }

    @SuppressWarnings("unchecked")
    private void track(LivingEntity entity) throws ReflectiveOperationException {
        Field active = NexusMobManager.class.getDeclaredField("activeNexusMobs");
        active.setAccessible(true);
        ((Map<UUID, NexusMob>) active.get(manager))
                .put(entity.getUniqueId(), new NexusMob(entity.getUniqueId(), "test_mob", entity.getLocation()));
    }

    private void startPhaseWatcher(LivingEntity entity, NexusMobType type) throws ReflectiveOperationException {
        Method start = NexusMobManager.class.getDeclaredMethod("startPhaseWatcher", LivingEntity.class, NexusMobType.class);
        start.setAccessible(true);
        start.invoke(manager, entity, type);
    }

    private int nextTaskId() {
        BukkitTask probe = server.getScheduler().runTaskLater(plugin, () -> { }, Long.MAX_VALUE / 2);
        probe.cancel();
        return probe.getTaskId() + 1;
    }

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
