package com.nexusmobs;

import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.models.Phase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test: the plugin enables with the bundled default configuration.
 */
class PluginLoadTest {

    private NexusMobsPlugin plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(NexusMobsPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginEnablesWithDefaultConfig() {
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getCommand("nexusmobs"), "command from plugin.yml must be registered");
        assertEquals(23, plugin.getConfigManager().getNexusMobTypes().size());
        assertFalse(plugin.getCustomItemManager().getCustomItems().isEmpty());
        assertFalse(plugin.getModelManager().getMobModels().isEmpty());
    }

    @Test
    void everyMobTypeHasAtLeastOnePhase() {
        for (NexusMobType type : plugin.getConfigManager().getNexusMobTypes().values()) {
            assertFalse(type.getPhases().isEmpty(), type.getId() + " has no phase (default should be injected)");
        }
    }

    @Test
    void configuredPhasesAreParsed() {
        NexusMobType ashTitan = plugin.getConfigManager().getNexusMobType("AshTitan");
        assertNotNull(ashTitan);

        List<Phase> phases = ashTitan.getPhases();
        assertEquals(3, phases.size());
        assertEquals(75.0, phases.get(0).getThresholdPercent(), 1e-9);
        assertEquals(50.0, phases.get(1).getThresholdPercent(), 1e-9);
        assertEquals(25.0, phases.get(2).getThresholdPercent(), 1e-9);
        assertEquals(1.5, phases.get(2).getAttackMultiplier(), 1e-9);
        assertEquals(6.0, phases.get(2).getArmorBonus(), 1e-9);
        // strength, speed, damage_resistance (legacy name, normalised to resistance)
        assertEquals(3, phases.get(2).getPotionEffects().size());
    }
}
