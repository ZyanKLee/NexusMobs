package com.nexusmobs.listeners;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.managers.NexusMobManager;
import com.nexusmobs.testutil.EffectlessWorldMock;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.LivingEntityMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for #22: Nexus mobs are persistent, but everything that makes them a boss
 * (abilities, phases, boss bar, particles, model) lives in tasks bound to one Entity object.
 * Natural spawns happen 800-1200 blocks from any player, so their chunk unloads right away.
 */
class NexusMobChunkReloadTest {

    /** periodic aoe-damage, boss bar, no model, default 50 % phase */
    private static final String TYPE = "TempestRider";
    /** has a custom model */
    private static final String MODEL_TYPE = "AncientBrute";

    private ServerMock server;
    private WorldMock world;
    private NexusMobsPlugin plugin;
    private NexusMobManager manager;

    private void startServer() {
        server = MockBukkit.mock();
        world = EffectlessWorldMock.addTo(server, "world");
    }

    private void loadPlugin() {
        plugin = MockBukkit.load(NexusMobsPlugin.class);
        manager = plugin.getNexusMobManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void mobGetsItsRuntimeBackWhenItsChunkLoads() {
        startServer();
        loadPlugin();
        LivingEntity mob = taggedZombie(TYPE);
        int from = nextTaskId();

        load(mob);

        int to = nextTaskId();
        assertNotNull(manager.getNexusMob(mob.getUniqueId()), "mob should be tracked again");
        assertNotNull(plugin.getEffectsManager().getBossBar(mob.getUniqueId()), "boss bar should be back");
        // aoe-damage + phase watcher + ambient particles + boss bar updater
        assertEquals(4, queued(from, to).size());
    }

    @Test
    void unloadStopsEverythingAndLoadRestartsIt() {
        startServer();
        loadPlugin();
        LivingEntity mob = taggedZombie(TYPE);
        int from = nextTaskId();
        load(mob);
        int to = nextTaskId();

        unload(mob);

        assertNull(manager.getNexusMob(mob.getUniqueId()));
        assertNull(plugin.getEffectsManager().getBossBar(mob.getUniqueId()));
        // particles stop on their next run; everything else is cancelled immediately
        server.getScheduler().performTicks(1);
        mob.remove(); // the unloaded Entity object is invalid
        server.getScheduler().performTicks(20);
        assertEquals(Set.of(), queued(from, to), "no task may survive the unload");

        LivingEntity reloaded = taggedZombie(TYPE); // a chunk load creates a new Entity object
        load(reloaded);
        assertNotNull(manager.getNexusMob(reloaded.getUniqueId()));
    }

    @Test
    void loadingTheSameEntityTwiceDoesNotDuplicateTasks() {
        startServer();
        loadPlugin();
        LivingEntity mob = taggedZombie(TYPE);
        int from = nextTaskId();

        load(mob);
        load(mob);

        assertEquals(4, queued(from, nextTaskId()).size());
    }

    @Test
    void reachedPhaseIsNotRepeatedAfterReload() {
        startServer();
        loadPlugin();
        LivingEntity mob = taggedZombie(TYPE);
        load(mob);
        mob.setHealth(mob.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.4);
        server.getScheduler().performTicks(41); // default phase at 50 %: +25 % max health, healed
        double maxAfterPhase = mob.getAttribute(Attribute.MAX_HEALTH).getBaseValue();

        unload(mob);
        load(mob);
        mob.setHealth(maxAfterPhase * 0.4);
        server.getScheduler().performTicks(41);

        assertEquals(maxAfterPhase, mob.getAttribute(Attribute.MAX_HEALTH).getBaseValue(), 1e-9,
                "the final phase (and its heal) must not trigger a second time");
    }

    @Test
    void mobsAlreadyLoadedWhenThePluginEnablesAreReattached() {
        startServer();
        // tagged before the plugin exists, like a mob saved in a spawn chunk before a restart
        LivingEntity mob = (LivingEntity) world.spawnEntity(new Location(world, 0, 70, 0), EntityType.ZOMBIE);
        mob.getPersistentDataContainer().set(new NamespacedKey("nexusmobs", "nexus_mob_type"), PersistentDataType.STRING, TYPE);

        loadPlugin();

        assertNotNull(manager.getNexusMob(mob.getUniqueId()));
        assertNotNull(plugin.getEffectsManager().getBossBar(mob.getUniqueId()));
    }

    @Test
    void mobOfARemovedTypeIsLeftAlone() {
        startServer();
        loadPlugin();
        LivingEntity mob = taggedZombie("NoLongerConfigured");

        load(mob);

        assertNull(manager.getNexusMob(mob.getUniqueId()));
    }

    @Test
    void otherEntitiesAreIgnored() {
        startServer();
        loadPlugin();
        LivingEntity zombie = (LivingEntity) world.spawnEntity(new Location(world, 0, 70, 0), EntityType.ZOMBIE);

        load(zombie);

        assertNull(manager.getNexusMob(zombie.getUniqueId()));
    }

    @Test
    void modelStandIsRecreatedAndNeverSaved() {
        startServer();
        loadPlugin();
        LivingEntity mob = taggedZombie(MODEL_TYPE);

        load(mob);
        List<ArmorStand> stands = world.getEntitiesByClass(ArmorStand.class).stream().toList();
        assertEquals(1, stands.size());
        assertFalse(stands.get(0).isPersistent(), "model stand must not be saved with the chunk");

        unload(mob);
        assertTrue(world.getEntitiesByClass(ArmorStand.class).stream().noneMatch(Entity::isValid),
                "unload must remove the model stand");

        load(mob);
        assertEquals(1, world.getEntitiesByClass(ArmorStand.class).stream().filter(Entity::isValid).count(),
                "exactly one stand after reload");
    }

    // ---------------------------------------------------------------- helpers

    private LivingEntity taggedZombie(String typeId) {
        LivingEntityMock mob = (LivingEntityMock) world.spawnEntity(new Location(world, 0, 70, 0), EntityType.ZOMBIE);
        mob.registerAttribute(Attribute.ATTACK_DAMAGE);
        mob.registerAttribute(Attribute.ARMOR);
        mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100.0);
        mob.setHealth(100.0);
        mob.getPersistentDataContainer().set(new NamespacedKey(plugin, "nexus_mob_type"), PersistentDataType.STRING, typeId);
        return mob;
    }

    private void load(Entity entity) {
        server.getPluginManager().callEvent(new EntitiesLoadEvent(entity.getLocation().getChunk(), List.of(entity)));
    }

    private void unload(Entity entity) {
        server.getPluginManager().callEvent(new EntitiesUnloadEvent(entity.getLocation().getChunk(), List.of(entity)));
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
