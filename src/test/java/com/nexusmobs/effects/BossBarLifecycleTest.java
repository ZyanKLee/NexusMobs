package com.nexusmobs.effects;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.testutil.EffectlessWorldMock;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regression tests for #6: one boss bar (and one updater task) per mob, also across reloads. */
class BossBarLifecycleTest {

    private ServerMock server;
    private NexusMobsPlugin plugin;
    private WorldMock world;
    private EffectsManager effects;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = EffectlessWorldMock.addTo(server, "world");
        plugin = MockBukkit.load(NexusMobsPlugin.class);
        effects = plugin.getEffectsManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void creatingTheBarAgainDoesNotAddASecondUpdater() {
        LivingEntity mob = spawnZombie();
        int from = nextTaskId();
        effects.createBossBar(mob, "Boss", BarColor.RED);
        effects.createBossBar(mob, "Boss (reloaded)", BarColor.RED); // what /nm reload does
        effects.createBossBar(mob, "Boss (reloaded twice)", BarColor.RED);
        int to = nextTaskId();

        assertEquals(1, queued(from, to).size(), "one updater task per mob, no matter how often it is refreshed");
    }

    @Test
    void reloadingDisplayNamesKeepsOneBarPerMob() throws Exception {
        LivingEntity mob = spawnZombie();
        TrackedMobs.track(plugin, mob, "AshTitan");
        int from = nextTaskId();
        effects.createBossBar(mob, "Ash Titan", BarColor.RED);

        plugin.getNexusMobManager().updateAllMobDisplayNames();
        plugin.getNexusMobManager().updateAllMobDisplayNames();
        int to = nextTaskId();

        assertEquals(1, queued(from, to).size());
    }

    @Test
    void refreshingUpdatesTitleAndColour() {
        LivingEntity mob = spawnZombie();
        effects.createBossBar(mob, "Old", BarColor.RED);

        effects.createBossBar(mob, "New", BarColor.BLUE);

        assertEquals("New", effects.getBossBar(mob.getUniqueId()).getTitle());
        assertEquals(BarColor.BLUE, effects.getBossBar(mob.getUniqueId()).getColor());
    }

    @Test
    void removeBossBarHidesItAndStopsTheUpdater() {
        LivingEntity mob = spawnZombie();
        PlayerMock player = server.addPlayer();
        player.teleport(mob.getLocation());
        int from = nextTaskId();
        effects.createBossBar(mob, "Boss", BarColor.RED);
        int to = nextTaskId();
        server.getScheduler().performTicks(6); // updater adds nearby players
        var bar = effects.getBossBar(mob.getUniqueId());
        assertEquals(1, bar.getPlayers().size(), "nearby player should see the bar");

        effects.removeBossBar(mob.getUniqueId());

        assertEquals(0, bar.getPlayers().size());
        assertEquals(Set.of(), queued(from, to));
        assertEquals(null, effects.getBossBar(mob.getUniqueId()));
    }

    @Test
    void removingTheMobRemovesItsBar() throws Exception {
        LivingEntity mob = spawnZombie();
        TrackedMobs.track(plugin, mob, "AshTitan");
        effects.createBossBar(mob, "Boss", BarColor.RED);

        plugin.getNexusMobManager().removeNexusMob(mob.getUniqueId()); // death listener path

        assertEquals(null, effects.getBossBar(mob.getUniqueId()));
    }

    @Test
    void cleanupRemovesAllBarsFromPlayers() {
        LivingEntity mob = spawnZombie();
        PlayerMock player = server.addPlayer();
        player.teleport(mob.getLocation());
        effects.createBossBar(mob, "Boss", BarColor.RED);
        server.getScheduler().performTicks(6);
        var bar = effects.getBossBar(mob.getUniqueId());

        effects.cleanup(); // plugin disable

        assertEquals(0, bar.getPlayers().size(), "bar must not stay on players' screens after disable");
    }

    @Test
    void playerChangingWorldIsRemovedWithoutError() {
        LivingEntity mob = spawnZombie();
        PlayerMock player = server.addPlayer();
        player.teleport(mob.getLocation());
        effects.createBossBar(mob, "Boss", BarColor.RED);
        server.getScheduler().performTicks(6);
        var bar = effects.getBossBar(mob.getUniqueId());
        assertEquals(1, bar.getPlayers().size());

        WorldMock nether = EffectlessWorldMock.addTo(server, "world_nether");
        player.teleport(new Location(nether, 0, 70, 0));
        // Location.distance() across worlds throws IllegalArgumentException
        server.getScheduler().performTicks(6);

        assertEquals(0, bar.getPlayers().size());
    }

    // ---------------------------------------------------------------- helpers

    private LivingEntity spawnZombie() {
        return (LivingEntity) world.spawnEntity(new Location(world, 0, 70, 0), EntityType.ZOMBIE);
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
