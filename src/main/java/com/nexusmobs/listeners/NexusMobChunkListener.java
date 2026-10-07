package com.nexusmobs.listeners;

import com.nexusmobs.managers.NexusMobManager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

/**
 * Keeps the runtime of Nexus mobs in sync with chunk loading (#22).
 *
 * <p>Nexus mobs are persistent, but their abilities, phase watcher, boss bar, particles and
 * model are bound to one Entity object. When a chunk unloads that object becomes invalid,
 * and loading the chunk again creates a new one.
 */
public class NexusMobChunkListener implements Listener {

    private final NexusMobManager manager;

    public NexusMobChunkListener(NexusMobManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            manager.reattach(entity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (manager.isNexusMob(entity)) {
                // stop tasks, hide the boss bar and remove the (non-persistent) model now,
                // instead of waiting for each task to notice the invalid entity
                manager.removeNexusMob(entity.getUniqueId());
            }
        }
    }
}
