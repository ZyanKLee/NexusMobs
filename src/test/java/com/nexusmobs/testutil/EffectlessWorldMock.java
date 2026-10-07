package com.nexusmobs.testutil;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.WorldCreator;
import org.bukkit.entity.LightningStrike;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * A {@link WorldMock} whose particle methods do nothing.
 *
 * <p>MockBukkit 4.20 throws {@code UnimplementedOperationException} from every
 * {@code spawnParticle} overload. Inside a scheduled task that exception also stops
 * the task from being rescheduled, so repeating tasks (phase watcher, abilities,
 * ambient particles) would silently stop after their first visual effect.
 * {@code strikeLightningEffect} (purely visual) is unimplemented as well.
 */
public class EffectlessWorldMock extends WorldMock {

    public EffectlessWorldMock(String name) {
        super(new WorldCreator(name));
    }

    /** Create the world and register it with the server. */
    public static EffectlessWorldMock addTo(ServerMock server, String name) {
        EffectlessWorldMock world = new EffectlessWorldMock(name);
        server.addWorld(world);
        return world;
    }

    /** Visual only; the plugin ignores the returned entity. */
    @Override
    public LightningStrike strikeLightningEffect(Location location) {
        return null;
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count) {
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count,
                              double offsetX, double offsetY, double offsetZ) {
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count,
                                  double offsetX, double offsetY, double offsetZ, T data) {
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count,
                              double offsetX, double offsetY, double offsetZ, double extra) {
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count,
                                  double offsetX, double offsetY, double offsetZ, double extra, T data) {
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count,
                                  double offsetX, double offsetY, double offsetZ, double extra, T data, boolean force) {
    }
}
