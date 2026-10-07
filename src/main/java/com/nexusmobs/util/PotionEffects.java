package com.nexusmobs.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves potion effect names from the configuration.
 */
public final class PotionEffects {

    /** Pre-1.20.5 Bukkit names mapped to their current registry keys. Whole names only (#27). */
    private static final Map<String, String> LEGACY_NAMES = Map.of(
            "increase_damage", "strength",
            "damage_resistance", "resistance",
            "slow", "slowness",
            "fast_digging", "haste",
            "slow_digging", "mining_fatigue",
            "jump", "jump_boost",
            "confusion", "nausea",
            "harm", "instant_damage",
            "heal", "instant_health");

    private PotionEffects() {
    }

    /**
     * Resolve a potion effect by its registry key ({@code slowness}, {@code minecraft:slowness})
     * or legacy Bukkit name ({@code SLOW}), case-insensitively.
     *
     * @return the effect type, or {@code null} if the name is unknown
     */
    public static PotionEffectType resolve(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }
        key = LEGACY_NAMES.getOrDefault(key, key);

        // fromString returns null (instead of throwing) for keys with invalid characters
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        return namespacedKey == null ? null : Registry.POTION_EFFECT_TYPE.get(namespacedKey);
    }
}
