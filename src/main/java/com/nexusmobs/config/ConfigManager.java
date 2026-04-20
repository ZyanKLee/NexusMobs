package com.nexusmobs.config;

import com.nexusmobs.NexusMobsPlugin;
import com.nexusmobs.models.NexusMobType;
import com.nexusmobs.models.LootDrop;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import com.nexusmobs.models.Phase;

/**
 * Manages plugin configuration and Nexus mob definitions
 */
public class ConfigManager {

    private final NexusMobsPlugin plugin;
    private final Map<String, NexusMobType> nexusMobTypes;
    private final Random random = new Random();

    public ConfigManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        this.nexusMobTypes = new HashMap<>();
        loadNexusMobTypes();
    }

    /**
     * Load all elite mob type configurations
     */
    private void loadNexusMobTypes() {
        nexusMobTypes.clear();

        ConfigurationSection NexusMobsSection = plugin.getConfig().getConfigurationSection("elite-mobs");
        if (NexusMobsSection == null) {
            plugin.getLogger().warning("No Nexus mob types configured!");
            return;
        }

        for (String key : NexusMobsSection.getKeys(false)) {
            ConfigurationSection mobSection = NexusMobsSection.getConfigurationSection(key);
            if (mobSection == null) continue;

            try {
                NexusMobType mobType = loadNexusMobType(key, mobSection);
                nexusMobTypes.put(key, mobType);
                plugin.getLogger().info("Loaded elite mob type: " + key);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load elite mob type: " + key);
                e.printStackTrace();
            }
        }
    }

    /**
     * Load a single elite mob type from configuration
     */
    private NexusMobType loadNexusMobType(String id, ConfigurationSection section) {
        String baseEntityStr = section.getString("base-entity", "ZOMBIE");
        EntityType baseEntity = EntityType.valueOf(baseEntityStr.toUpperCase());

        String displayName = section.getString("display-name", "&c&lNexus Mob");
        // Allow language files to override display name for this mob id
        if (plugin.getLanguageManager() != null) {
            // try a few key formats: original id and kebab-case (lower-hyphen) id
            String kebab = id.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
            String langKey1 = "mobs." + id + ".display-name";
            String langKey2 = "elite-mobs." + id + ".display-name";
            String langKey3 = "mobs." + kebab + ".display-name";
            String langKey4 = "elite-mobs." + kebab + ".display-name";

            if (plugin.getLanguageManager().hasKey(langKey1)) {
                displayName = plugin.getLanguageManager().getString(langKey1, displayName);
            } else if (plugin.getLanguageManager().hasKey(langKey2)) {
                displayName = plugin.getLanguageManager().getString(langKey2, displayName);
            } else if (plugin.getLanguageManager().hasKey(langKey3)) {
                displayName = plugin.getLanguageManager().getString(langKey3, displayName);
            } else if (plugin.getLanguageManager().hasKey(langKey4)) {
                displayName = plugin.getLanguageManager().getString(langKey4, displayName);
            } else {
                displayName = displayName.replace("&", "§");
            }
        } else {
            displayName = displayName.replace("&", "§");
        }

        double maxHealth = section.getDouble("max-health", 100.0);
        double attackDamage = section.getDouble("attack-damage", 10.0);
        double armor = section.getDouble("armor", 0.0);
        boolean glowing = section.getBoolean("glowing", true);

        // =======================
        // Potion effects
        // =======================
        List<PotionEffect> potionEffects = new ArrayList<>();
        if (section.contains("potion-effects")) {
            List<Map<?, ?>> effectsList = section.getMapList("potion-effects");
            for (Map<?, ?> effectMap : effectsList) {
                String typeStr = (String) effectMap.get("type");

                Object levelObj = effectMap.get("level");
                if (levelObj == null) {
                    levelObj = 1;
                }

                Object durationObj = effectMap.get("duration-ticks");
                if (durationObj == null) {
                    durationObj = Integer.MAX_VALUE;
                }

                int level = levelObj instanceof Number ? ((Number) levelObj).intValue() : 1;
                int duration = durationObj instanceof Number ? ((Number) durationObj).intValue() : Integer.MAX_VALUE;

                PotionEffectType type = getPotionEffectType(typeStr);
                if (type != null) {
                    // У PotionEffect рівень починається з 0, тому level - 1
                    potionEffects.add(new PotionEffect(type, duration, level - 1, false, false));
                } else {
                    plugin.getLogger().warning("Unknown potion effect type: " + typeStr);
                }
            }
        }

        // =======================
        // Drops
        // =======================
        List<LootDrop> drops = new ArrayList<>();
        if (section.contains("drops")) {
            List<Map<?, ?>> dropsList = section.getMapList("drops");
            for (Map<?, ?> dropMap : dropsList) {
                String typeStr = (String) dropMap.get("type");

                Object minObj = dropMap.get("min");
                if (minObj == null) {
                    minObj = 1;
                }

                Object maxObj = dropMap.get("max");
                if (maxObj == null) {
                    maxObj = 1;
                }

                Object chanceObj = dropMap.get("chance");
                if (chanceObj == null) {
                    chanceObj = 1.0;
                }

                int min = minObj instanceof Number ? ((Number) minObj).intValue() : 1;
                int max = maxObj instanceof Number ? ((Number) maxObj).intValue() : 1;
                double chance = chanceObj instanceof Number ? ((Number) chanceObj).doubleValue() : 1.0;

                try {
                    Material material = Material.valueOf(typeStr.toUpperCase(Locale.ROOT));
                    drops.add(new LootDrop(material, min, max, chance));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material type in drops: " + typeStr);
                }
            }
        }

        // Abilities (raw section, обробляєш вже в іншому місці)
        ConfigurationSection abilitiesSection = section.getConfigurationSection("abilities");

        // =======================
        // Phases (optional)
        // =======================
        List<Phase> phases = new ArrayList<>();
        if (section.contains("phases")) {
            List<Map<?, ?>> phaseMaps = section.getMapList("phases");
            for (Map<?, ?> pm : phaseMaps) {
                // Support either percent threshold ("threshold") or absolute HP ("threshold-hp")
                double threshold = -1.0;
                double thresholdHp = -1.0;
                if (pm.containsKey("threshold-hp")) {
                    Object hpObj = pm.get("threshold-hp");
                    thresholdHp = hpObj instanceof Number ? ((Number) hpObj).doubleValue() : -1.0;
                } else {
                    Object thresholdObj = pm.get("threshold");
                    threshold = thresholdObj instanceof Number ? ((Number) thresholdObj).doubleValue() : 50.0;
                }

                double attackMult = 1.0;
                Object atObj = pm.get("attack-multiplier");
                if (atObj instanceof Number) attackMult = ((Number) atObj).doubleValue();

                double armorBonus = 0.0;
                Object abObj = pm.get("armor-bonus");
                if (abObj instanceof Number) armorBonus = ((Number) abObj).doubleValue();

                List<PotionEffect> phaseEffects = new ArrayList<>();
                Object effectsObj = pm.get("potion-effects");
                if (effectsObj instanceof List) {
                    List<Map<?, ?>> peList = (List<Map<?, ?>>) effectsObj;
                    for (Map<?, ?> em : peList) {
                        String typeStr = (String) em.get("type");
                        Object lvlObj = em.get("level");
                        Object durObj = em.get("duration-ticks");
                        int level = lvlObj instanceof Number ? ((Number) lvlObj).intValue() : 1;
                        int duration = durObj instanceof Number ? ((Number) durObj).intValue() : Integer.MAX_VALUE;
                        PotionEffectType pet = getPotionEffectType(typeStr);
                        if (pet != null) {
                            phaseEffects.add(new PotionEffect(pet, duration, Math.max(0, level - 1), false, false));
                        }
                    }
                }

                if (thresholdHp >= 0) {
                    phases.add(new com.nexusmobs.models.Phase(thresholdHp, true, attackMult, armorBonus, phaseEffects));
                } else {
                    phases.add(new com.nexusmobs.models.Phase(threshold, attackMult, armorBonus, phaseEffects));
                }
            }
        }

        // If no phases configured, inject a sensible default Next Phase that activates at 50% HP
        if (phases.isEmpty()) {
            // default: at 50% HP, +25% attack, +2 armor, no potion effects
            phases.add(new Phase(50.0, 1.25, 2.0, Collections.emptyList()));
        }

        return new NexusMobType(
            id,
            baseEntity,
            displayName,
            maxHealth,
            attackDamage,
            armor,
            glowing,
            potionEffects,
            drops,
            abilitiesSection,
            phases
        );
    }

    /**
     * Get PotionEffectType by name (compatible with Paper 1.21+)
     */
    private PotionEffectType getPotionEffectType(String name) {
        if (name == null) return null;

        // Normalize legacy names
        String normalizedName = name.toUpperCase(Locale.ROOT)
                .replace("INCREASE_DAMAGE", "STRENGTH")
                .replace("DAMAGE_RESISTANCE", "RESISTANCE")
                .replace("SLOW", "SLOWNESS")
                .replace("FAST_DIGGING", "HASTE")
                .replace("SLOW_DIGGING", "MINING_FATIGUE")
                .replace("JUMP", "JUMP_BOOST")
                .replace("CONFUSION", "NAUSEA")
                .replace("HARM", "INSTANT_DAMAGE")
                .replace("HEAL", "INSTANT_HEALTH");

        try {
            NamespacedKey key = NamespacedKey.minecraft(normalizedName.toLowerCase(Locale.ROOT));
            return Registry.POTION_EFFECT_TYPE.get(key);
        } catch (Exception e) {
            try {
                return PotionEffectType.getByName(name);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    /**
     * Get all configured elite mob types
     */
    public Map<String, NexusMobType> getNexusMobTypes() {
        return Collections.unmodifiableMap(nexusMobTypes);
    }

    /**
     * Get a random elite mob type
     */
    public NexusMobType getRandomNexusMobType() {
        if (nexusMobTypes.isEmpty()) {
            return null;
        }

        List<NexusMobType> types = new ArrayList<>(nexusMobTypes.values());
        return types.get(random.nextInt(types.size()));
    }

    /**
     * Get an elite mob type by ID
     */
    public NexusMobType getNexusMobType(String id) {
        return nexusMobTypes.get(id);
    }

    /**
     * Get message from config with placeholders replaced
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        // Prefer language file messages when available
        String def = plugin.getConfig().getString("messages." + key, "");
        if (plugin.getLanguageManager() != null) {
            return plugin.getLanguageManager().formatMessage("messages." + key, placeholders, def);
        }

        String message = def.replace("&", "§");
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }

    /**
     * Get list of allowed spawn worlds
     */
    public List<String> getAllowedWorlds() {
        return plugin.getConfig().getStringList("spawn.worlds");
    }

    /**
     * Get minimum spawn distance
     */
    public int getMinSpawnDistance() {
        return plugin.getConfig().getInt("spawn.min-distance", 800);
    }

    /**
     * Get maximum spawn distance
     */
    public int getMaxSpawnDistance() {
        return plugin.getConfig().getInt("spawn.max-distance", 1200);
    }

    /**
     * Get minimum spawn interval in hours
     */
    public double getMinSpawnIntervalHours() {
        return plugin.getConfig().getDouble("spawn.min-spawn-interval-hours", 3.0);
    }

    /**
     * Get maximum spawn interval in hours
     */
    public double getMaxSpawnIntervalHours() {
        return plugin.getConfig().getDouble("spawn.max-spawn-interval-hours", 24.0);
    }

    /**
     * Get maximum spawn attempts
     */
    public int getMaxSpawnAttempts() {
        return plugin.getConfig().getInt("spawn.max-spawn-attempts", 10);
    }

    /**
     * Get maximum concurrent elite mobs
     */
    public int getMaxConcurrentElites() {
        return plugin.getConfig().getInt("spawn.max-concurrent-elites", 3);
    }
}


