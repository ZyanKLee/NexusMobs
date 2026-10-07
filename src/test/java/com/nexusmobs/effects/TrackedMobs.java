package com.nexusmobs.effects;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.managers.NexusMobManager;
import com.nexusmobs.models.NexusMob;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/** Registers a mock entity as an active Nexus mob without going through spawnNexusMob(). */
final class TrackedMobs {

    private TrackedMobs() {
    }

    @SuppressWarnings("unchecked")
    static void track(NexusMobsPlugin plugin, LivingEntity entity, String typeId) throws ReflectiveOperationException {
        Field active = NexusMobManager.class.getDeclaredField("activeNexusMobs");
        active.setAccessible(true);
        ((Map<UUID, NexusMob>) active.get(plugin.getNexusMobManager()))
                .put(entity.getUniqueId(), new NexusMob(entity.getUniqueId(), typeId, entity.getLocation()));
    }
}
