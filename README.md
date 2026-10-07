# NexusMobs v3.0.0

Advanced custom mobs system with models, abilities, custom loot and multi-language support.

**Created by:** _4rubka_

---

## 🌟 Features

### Nexus Mobs
- **8 Custom Mob Types** with unique characteristics
- **Boss Bar** for each mob with custom colors
- **Ambient Particles** around mobs
- **Custom 3D Models** via CustomModelData
- **Advanced Abilities**: AOE damage, minion summoning, teleportation, debuffs
- **Smart Spawning**: 800-1200 blocks from players, ~1 hour intervals

### Custom Items
- **40+ Unique Items** including:
  - **10+ Weapons**: Swords, Axes, Bows, Tridents, Staffs/Wands
  - **4 Complete Armor Sets**: Guardian, Shadow, Inferno, Frost (16 pieces)
  - **Tools**: Crystal Pickaxe, Earth Shaker Shovel
  - **Utility Items**: Healing orbs, totems, charms
- **Active & Passive Abilities** on all items
- **Custom Model Data** for resource pack integration
- **Balanced Drop Rates**

### Multi-Language Support
- **3 Languages**: English, Ukrainian, Russian
- **Per-Player Language**: Each player can choose their language
- **Fully Localized**: All messages, GUI, mob names translated

### Statistics & Leaderboard
- Kill tracking by mob type
- K/D ratio calculation
- Top players leaderboard
- Auto-save player data

### GUI System
- Intuitive admin interface
- Mob spawning menu
- Leaderboard viewer
- Statistics dashboard
- Custom items browser

---

## 📋 Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/nexusmobs` `/nm` | Open GUI menu | nexusmobs.stats |
| `/nm stats [player]` | View statistics | nexusmobs.stats |
| `/nm top [count]` | View leaderboard | nexusmobs.leaderboard |
| `/nm help` | Show help | none |

### Admin Commands  
| Command | Description | Permission |
|---------|-------------|------------|
| `/nm reload` | Reload configuration | nexusmobs.admin |
| `/nm spawn [type]` | Spawn a nexus mob | nexusmobs.spawn |
| `/nm list <mobs\|items\|models>` | List configs | nexusmobs.list |
| `/nm info` | Plugin information | nexusmobs.admin |
| `/nm give <item> [player]` | Give custom item | nexusmobs.give |
| `/nm language <lang>` | Change language | nexusmobs.language |

**Available Languages:** `en_us`, `uk_ua`, `ru_ru`

---

## 🔧 Installation

### Requirements
- **Java**: 17+
- **Server**: Paper/Purpur 1.21+
- **Dependencies**: None (optional: PlaceholderAPI, Vault)

- Copy `NexusMobs-3.0.0.jar` to your server's `plugins` folder
- Restart server
- Configure `plugins/NexusMobs/config.yml`

# 🐛 Troubleshooting

### Mobs Not Spawning
- Check `spawn.enabled: true` in config
- Verify world names match your server
- Check console for errors
- Try manual spawn: `/nm spawn AncientBrute`

### Language Not Changing
- Check language file exists in `lang/` folder
- Verify language code is correct (en_us, uk_ua, ru_ru)
- Use command: `/nm language en_us`
- Restart server if needed

### Items Not Dropping
- Check `drop-chance` values (0.0 - 1.0)
- Ensure mob inventory isn't full
- Verify item is configured correctly

---

## 🤖 Development

This project is developed with the help of AI coding assistants, though not exclusively.

---

## 📜 License

MIT License - Free to use and modify

---

## 👤 Author

**_4rubka_**

---

## 🔗 Links

- GitHub: https://github.com/4rubka/nexusmobs
- Issues: https://github.com/4rubka/nexusmobs/issues

---

**Version:** 3.0.0  
**Release Date:** December 1, 2025  
**Minecraft Version:** 1.21+  
**API Version:** Paper/Purpur

**Enjoy NexusMobs! 🎮**
