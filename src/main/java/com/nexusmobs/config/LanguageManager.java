package com.nexusmobs.config;

import com.nexusmobs.NexusMobsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple language loader that reads bundled language YML files and provides lookups.
 */
public class LanguageManager {

    private final NexusMobsPlugin plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private String currentLanguage;

    public LanguageManager(NexusMobsPlugin plugin) {
        this.plugin = plugin;
        loadLanguages();
        applyDefault();
    }

    /**
     * Load known language files from resources/lang/*.yml
     */
    public void loadLanguages() {
        languages.clear();

        loadLanguageFile("en_us.yml", "en_us");
        loadLanguageFile("uk_ua.yml", "uk_ua");
        loadLanguageFile("ru_ru.yml", "ru_ru");
    }

    private void loadLanguageFile(String resourceName, String key) {
        try (InputStream in = plugin.getResource("lang/" + resourceName)) {
            if (in == null) return;
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            languages.put(key, cfg);
            plugin.getLogger().info("Loaded language: " + key);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load language " + resourceName + ": " + e.getMessage());
        }
    }

    public void applyDefault() {
        String def = plugin.getConfig().getString("language.default", "en_us");
        if (languages.containsKey(def)) {
            currentLanguage = def;
        } else if (!languages.isEmpty()) {
            currentLanguage = languages.keySet().iterator().next();
        } else {
            currentLanguage = "en_us";
        }
        plugin.getLogger().info("Active language: " + currentLanguage);
    }

    public boolean hasKey(String key) {
        FileConfiguration cfg = languages.get(currentLanguage);
        return cfg != null && cfg.contains(key);
    }

    public String getString(String key, String def) {
        FileConfiguration cfg = languages.get(currentLanguage);
        if (cfg == null) return def;
        String val = cfg.getString(key);
        return val != null ? val : def;
    }

    public List<String> getStringList(String key) {
        FileConfiguration cfg = languages.get(currentLanguage);
        if (cfg == null) return Collections.emptyList();
        return cfg.getStringList(key);
    }

    public String formatMessage(String key, Map<String, String> placeholders, String def) {
        String raw = getString(key, null);
        if (raw == null) raw = def == null ? "" : def;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                raw = raw.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return raw.replace("&", "§");
    }

}
