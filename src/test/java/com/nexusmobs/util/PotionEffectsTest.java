package com.nexusmobs.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Regression tests for #27. */
class PotionEffectsTest {

    @BeforeAll
    static void setUp() {
        MockBukkit.mock(); // provides Registry.POTION_EFFECT_TYPE
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void everyRegisteredEffectResolvesByItsOwnName() {
        List<String> broken = new ArrayList<>();
        int checked = 0;
        for (PotionEffectType type : Registry.POTION_EFFECT_TYPE) {
            String key = type.getKey().getKey(); // e.g. "slow_falling", "health_boost", "jump_boost"
            for (String name : List.of(key, key.toUpperCase(Locale.ROOT), "minecraft:" + key)) {
                if (PotionEffects.resolve(name) != type) {
                    broken.add(name);
                }
            }
            checked++;
        }
        assertFalse(checked == 0, "registry is empty, test would be vacuous");
        assertEquals(List.of(), broken);
    }

    @ParameterizedTest
    @CsvSource({
            "INCREASE_DAMAGE, strength",
            "DAMAGE_RESISTANCE, resistance",
            "SLOW, slowness",
            "slow, slowness",
            "FAST_DIGGING, haste",
            "SLOW_DIGGING, mining_fatigue",
            "JUMP, jump_boost",
            "CONFUSION, nausea",
            "HARM, instant_damage",
            "HEAL, instant_health",
    })
    void legacyNamesMapToCurrentEffects(String legacy, String expectedKey) {
        assertEquals(Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(expectedKey)), PotionEffects.resolve(legacy));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "not_an_effect", "slownessness", "has spaces", "other:slowness"})
    void unknownNamesResolveToNull(String name) {
        assertNull(PotionEffects.resolve(name));
    }
}
