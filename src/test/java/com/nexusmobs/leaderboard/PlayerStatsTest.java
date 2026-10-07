package com.nexusmobs.leaderboard;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStatsTest {

    private final PlayerStats stats = new PlayerStats(UUID.randomUUID(), "Steve");

    @Test
    void killsAreCountedPerType() {
        stats.addKill("AshTitan");
        stats.addKill("AshTitan");
        stats.addKill("BoneLord");

        assertEquals(3, stats.getTotalKills());
        assertEquals(2, stats.getKillsForType("AshTitan"));
        assertEquals(1, stats.getKillsForType("BoneLord"));
        assertEquals(0, stats.getKillsForType("Unknown"));
    }

    @Test
    void kdRatioWithoutDeathsEqualsKills() {
        stats.addKill("AshTitan");
        stats.addKill("AshTitan");

        assertEquals(2.0, stats.getKDRatio(), 1e-9);
    }

    @Test
    void kdRatioDividesKillsByDeaths() {
        stats.addKill("AshTitan");
        stats.addKill("AshTitan");
        stats.addKill("AshTitan");
        stats.addDeath();
        stats.addDeath();

        assertEquals(1.5, stats.getKDRatio(), 1e-9);
    }

    @Test
    void loadedKillMapIsCopied() {
        Map<String, Integer> source = new java.util.HashMap<>(Map.of("AshTitan", 4));
        PlayerStats loaded = new PlayerStats(UUID.randomUUID(), "Alex", 4, 0, 0, 0L, source);

        source.put("AshTitan", 99);

        assertEquals(4, loaded.getKillsForType("AshTitan"));
    }
}
