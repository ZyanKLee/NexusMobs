package com.nexusmobs.loot;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.testutil.EffectlessWorldMock;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.ArrowMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for #8: a reflected projectile must not also hurt the player. */
class ReflectProjectileTest {

    private ServerMock server;
    private WorldMock world;
    private NexusMobsPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = EffectlessWorldMock.addTo(server, "world");
        plugin = MockBukkit.load(NexusMobsPlugin.class);
        // no bundled item has REFLECT_PROJECTILES; define one like an admin would (config.yml override)
        plugin.getConfig().set("custom-items.MirrorPlate.material", "DIAMOND_CHESTPLATE");
        plugin.getConfig().set("custom-items.MirrorPlate.abilities.REFLECT_PROJECTILES.chance", 1.0);
        plugin.getCustomItemManager().reload();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reflectedArrowDealsNoDamageAndChangesOwner() {
        PlayerMock player = playerWearing("MirrorPlate");
        LivingEntity skeleton = (LivingEntity) world.spawnEntity(new Location(world, 10, 65, 0), EntityType.SKELETON);
        Arrow arrow = arrowFrom(skeleton, player);

        EntityDamageByEntityEvent event = hit(arrow, player);

        assertTrue(event.isCancelled(), "the reflected arrow must not damage the player");
        assertEquals(player, arrow.getShooter(), "reflected arrow belongs to the player now");
    }

    @Test
    void withoutTheItemTheArrowStillHurts() {
        PlayerMock player = server.addPlayer();
        player.teleport(new Location(world, 0, 65, 0));
        LivingEntity skeleton = (LivingEntity) world.spawnEntity(new Location(world, 10, 65, 0), EntityType.SKELETON);
        Arrow arrow = arrowFrom(skeleton, player);

        EntityDamageByEntityEvent event = hit(arrow, player);

        assertFalse(event.isCancelled());
        assertEquals(skeleton, arrow.getShooter());
    }

    // ---------------------------------------------------------------- helpers

    private PlayerMock playerWearing(String itemId) {
        PlayerMock player = server.addPlayer();
        player.teleport(new Location(world, 0, 65, 0));
        ItemStack item = plugin.getCustomItemManager().createItemStack(itemId);
        assertNotNull(item, itemId);
        player.getInventory().setChestplate(item);
        return player;
    }

    private Arrow arrowFrom(LivingEntity shooter, PlayerMock target) {
        Arrow arrow = new ShootableArrowMock(server);
        arrow.setShooter(shooter);
        arrow.setVelocity(target.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize());
        return arrow;
    }

    /** MockBukkit 4.20 does not implement Projectile#setShooter; keep the shooter in a field. */
    private static final class ShootableArrowMock extends ArrowMock {
        private ProjectileSource shooter;

        ShootableArrowMock(ServerMock server) {
            super(server, UUID.randomUUID());
        }

        @Override
        public ProjectileSource getShooter() {
            return shooter;
        }

        @Override
        public void setShooter(ProjectileSource source) {
            this.shooter = source;
        }
    }

    @SuppressWarnings({"deprecation", "removal"})
    private EntityDamageByEntityEvent hit(Arrow arrow, PlayerMock player) {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(arrow, player, DamageCause.PROJECTILE, 6.0);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
