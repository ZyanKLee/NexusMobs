package com.nexusmobs;

import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.models.Phase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test: the plugin enables with the bundled default configuration.
 */
class PluginLoadTest {

    private NexusMobsPlugin plugin;
    private final List<String> warnings = new ArrayList<>();
    private final Handler warningCollector = new Handler() {
        @Override
        public void publish(LogRecord logRecord) {
            if (logRecord.getLevel().intValue() >= Level.WARNING.intValue()) {
                warnings.add(logRecord.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        // plugin loggers propagate to the root logger (not to the server logger); capture onEnable() warnings
        Logger.getLogger("").addHandler(warningCollector);
        plugin = MockBukkit.load(NexusMobsPlugin.class);
    }

    @AfterEach
    void tearDown() {
        Logger.getLogger("").removeHandler(warningCollector);
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

    @Test
    void warningsAreCaptured() {
        // guards the tests below against a silently broken log capture
        plugin.getLogger().warning("probe");
        assertTrue(warnings.contains("probe"), "log capture is not wired up");
    }

    // #27: modern names containing a legacy name (SLOW, HEAL, JUMP, ...) were mangled and dropped
    @Test
    void modernPotionNamesAreNotDropped() {
        assertHasEffect("FrostGiant", PotionEffectType.SLOWNESS);     // "slowness"
        assertHasEffect("GlacialWarden", PotionEffectType.SLOWNESS);  // "slowness"
        assertHasEffect("ArcaneSylph", PotionEffectType.SLOW_FALLING); // "slow_falling"
    }

    @Test
    void legacyPotionNamesAreStillMapped() {
        assertHasEffect("BoneLord", PotionEffectType.SLOWNESS); // legacy "slow"
    }

    @Test
    void bundledConfigHasNoUnknownPotionEffects() {
        List<String> unknown = warnings.stream().filter(w -> w.startsWith("Unknown potion effect type")).toList();
        assertEquals(List.of(), unknown);
    }

    private void assertHasEffect(String mobId, PotionEffectType expected) {
        NexusMobType type = plugin.getConfigManager().getNexusMobType(mobId);
        assertNotNull(type, mobId);
        List<PotionEffectType> effects = type.getPotionEffects().stream().map(PotionEffect::getType).toList();
        assertTrue(effects.contains(expected), mobId + " should have " + expected.getKey() + " but has " + effects);
    }
}
