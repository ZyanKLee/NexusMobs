package com.nexusmobs.managers;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.config.ConfigManager;
import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.models.Phase;
import com.nexusmobs.testutil.EffectlessWorldMock;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.LivingEntityMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for #4: the phase watcher must use each phase's own threshold
 * instead of jumping to the last phase at 50 % HP.
 */
class PhaseProgressionTest {

    private static final double BASE_ATTACK = 10.0;
    private static final double MAX_HEALTH = 100.0;

    private ServerMock server;
    private WorldMock world;
    private NexusMobManager manager;
    private NexusMobsPlugin plugin;

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

    /** Three percentage phases like AshTitan in the bundled config. */
    private static NexusMobType threePhaseType() {
        return type(List.of(
                new Phase(75.0, 1.1, 2.0, Collections.emptyList()),
                new Phase(50.0, 1.25, 4.0, Collections.emptyList()),
                new Phase(25.0, 1.5, 6.0, Collections.emptyList())));
    }

    @Test
    void firstPhaseActivatesAtItsOwnThreshold() throws Exception {
        LivingEntity mob = spawnWithWatcher(threePhaseType());

        setHealthAndTick(mob, 70.0); // below 75 %, above 50 %

        assertEquals(BASE_ATTACK * 1.1, attack(mob), 1e-9);
        assertEquals(2.0, armor(mob), 1e-9);
    }

    @Test
    void phasesActivateOneAfterAnother() throws Exception {
        LivingEntity mob = spawnWithWatcher(threePhaseType());

        setHealthAndTick(mob, 70.0);
        assertEquals(BASE_ATTACK * 1.1, attack(mob), 1e-9);

        setHealthAndTick(mob, 45.0);
        assertEquals(BASE_ATTACK * 1.25, attack(mob), 1e-9);
        assertEquals(4.0, armor(mob), 1e-9);

        setHealthAndTick(mob, 20.0);
        assertEquals(BASE_ATTACK * 1.5, attack(mob), 1e-9);
        assertEquals(6.0, armor(mob), 1e-9);
    }

    @Test
    void noPhaseAboveTheFirstThreshold() throws Exception {
        LivingEntity mob = spawnWithWatcher(threePhaseType());

        setHealthAndTick(mob, 80.0);

        assertEquals(BASE_ATTACK, attack(mob), 1e-9);
        assertEquals(0.0, armor(mob), 1e-9);
    }

    @Test
    void bigHitSkipsToTheDeepestReachedPhase() throws Exception {
        LivingEntity mob = spawnWithWatcher(threePhaseType());

        setHealthAndTick(mob, 10.0); // all three thresholds crossed in one tick

        assertEquals(BASE_ATTACK * 1.5, attack(mob), 1e-9);
    }

    @Test
    void absoluteHpThresholdIsHonoured() throws Exception {
        LivingEntity mob = spawnWithWatcher(type(List.of(
                new Phase(30.0, true, 2.0, 1.0, Collections.emptyList()))));

        setHealthAndTick(mob, 35.0);
        assertEquals(BASE_ATTACK, attack(mob), 1e-9, "35 HP is above the 30 HP threshold");

        setHealthAndTick(mob, 29.0);
        assertEquals(BASE_ATTACK * 2.0, attack(mob), 1e-9);
    }

    @Test
    void phasesAreNotReappliedOrReversedWhenHealing() throws Exception {
        LivingEntity mob = spawnWithWatcher(threePhaseType());

        setHealthAndTick(mob, 45.0);
        setHealthAndTick(mob, 90.0); // regeneration / healing

        assertEquals(BASE_ATTACK * 1.25, attack(mob), 1e-9, "a reached phase stays active");
    }

    @Test
    void phasesFromConfigAreOrderedByThreshold() {
        plugin.getConfig().set("elite-mobs.Unordered.base-entity", "ZOMBIE");
        plugin.getConfig().set("elite-mobs.Unordered.max-health", 200.0);
        plugin.getConfig().set("elite-mobs.Unordered.phases", List.of(
                Map.of("threshold", 25, "attack-multiplier", 3.0),
                Map.of("threshold", 75, "attack-multiplier", 1.0),
                Map.of("threshold-hp", 100, "attack-multiplier", 2.0))); // 100 of 200 HP = 50 %

        List<Phase> phases = new ConfigManager(plugin).getNexusMobType("Unordered").getPhases();

        assertEquals(List.of(1.0, 2.0, 3.0), phases.stream().map(Phase::getAttackMultiplier).toList());
    }

    // ---------------------------------------------------------------- helpers

    private static NexusMobType type(List<Phase> phases) {
        return new NexusMobType("phase_test", EntityType.ZOMBIE, "Phase Test", MAX_HEALTH, BASE_ATTACK, 0.0, false,
                Collections.emptyList(), Collections.emptyList(), null, phases);
    }

    private LivingEntity spawnWithWatcher(NexusMobType type) throws Exception {
        LivingEntityMock entity = (LivingEntityMock) world.spawnEntity(new Location(world, 0, 70, 0), EntityType.ZOMBIE);
        entity.registerAttribute(Attribute.ATTACK_DAMAGE);
        entity.registerAttribute(Attribute.ARMOR);
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(BASE_ATTACK);
        entity.setHealth(MAX_HEALTH);

        Method start = NexusMobManager.class.getDeclaredMethod("startPhaseWatcher", LivingEntity.class, NexusMobType.class);
        start.setAccessible(true);
        start.invoke(manager, entity, type);
        return entity;
    }

    private void setHealthAndTick(LivingEntity mob, double health) {
        mob.setHealth(Math.min(health, mob.getAttribute(Attribute.MAX_HEALTH).getValue()));
        server.getScheduler().performTicks(41); // watcher period is 40 ticks
    }

    private static double attack(LivingEntity mob) {
        return mob.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue();
    }

    private static double armor(LivingEntity mob) {
        return mob.getAttribute(Attribute.ARMOR).getBaseValue();
    }
}
