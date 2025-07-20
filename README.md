# TreasureDungeon Plugin

A comprehensive Minecraft Paper 1.21.4 plugin that integrates with mcMMO to provide treasure hunting adventures through procedurally generated dungeons.

## Features

### 🎯 Core Features
- **mcMMO Integration**: Treasure maps drop when reaching configurable skill levels
- **Dynamic Dungeons**: Procedurally placed dungeons with WorldEdit schematic support
- **Wave-based Combat**: Configurable waves of MythicMobs before boss encounters
- **Damage Tracking**: Top 3 damage dealers ranking system for boss fights
- **Treasure Rewards**: Configurable loot via commands or items
- **Cooldown System**: Per-player, per-skill cooldown management
- **Multi-language Support**: English and Portuguese translations included

### 🛠️ Technical Features
- **Modular Architecture**: Clean separation of concerns for easy maintenance
- **Performance Optimized**: Efficient caching and async operations
- **Null Safety**: Comprehensive error handling throughout
- **Configuration Driven**: Everything customizable via YAML
- **Integration Ready**: Soft dependencies for WorldEdit, MythicMobs, Multiverse

## Configuration

### Skills Setup
```yaml
skills:
  mining:
    enabled: true
    level-required: 1000
    chance-to-drop: 0.3  # 30%
    cooldown: "6h"
    dungeon:
      schematic-bell: "bell-mining.schem"
      schematic-dungeon: "mining-dungeon.schem"
      waves:
        count: 3
        mobs:
          wave1: ["SKELETON_MINER", "ZOMBIE_DIGGER"]
          wave2: ["CAVE_SPIDER_ELITE", "SKELETON_MINER"]
          wave3: ["UNDEAD_DWARF", "CRYSTAL_GOLEM"]
      boss:
        id: "MINING_OVERLORD"
        spawn-delay: 8s
```

### Map Customization
```yaml
map-item:
  material: FILLED_MAP
  display-name: "&6Mapa do Tesouro [Mineração]"
  lore:
    - "&7Um velho mapa marcado por explosões."
    - "&eUsa para descobrir uma mina escondida!"
  custom-model-data: 1031
  glowing: true
```

## Commands

- `/treasure tp` - Teleport to treasure world spawn
- `/treasure reload` - Reload plugin configuration

## Permissions

- `treasure.use` - Basic plugin usage (default: true)
- `treasure.tp` - Teleport command (default: op)
- `treasure.admin` - Administrative commands (default: op)
- `treasure.reload` - Reload configuration (default: op)

## Dependencies

### Required
- **Paper 1.21.4+**
- **mcMMO 2.2.024+**

### Optional (Recommended)
- **WorldEdit 7.3.8+** - For schematic loading
- **MythicMobs 5.6.1+** - For custom mob spawning
- **Multiverse-Core** - For world management

## Installation

1. Download the plugin JAR file
2. Place in your server's `plugins/` folder
3. Restart your server
4. Configure `config.yml` to your preferences
5. Add your schematics to `plugins/TreasureDungeon/schematics/`
6. Reload with `/treasure reload`

## File Structure

```
plugins/TreasureDungeon/
├── config.yml          # Main configuration
├── data.yml            # Player data storage
├── lang/               # Language files
│   ├── en.yml         # English messages
│   └── pt.yml         # Portuguese messages
└── schematics/         # WorldEdit schematic files
    ├── bell-mining.schem
    ├── mining-dungeon.schem
    └── ...
```

## Development

### Building
```bash
mvn clean package
```

### API Usage
```java
TreasureDungeonPlugin plugin = TreasureDungeonPlugin.getInstance();
MapManager mapManager = plugin.getMapManager();
DungeonManager dungeonManager = plugin.getDungeonManager();
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support, feature requests, or bug reports, please open an issue on the GitHub repository.

---

**Author**: Novi-ui  
**Version**: 1.0.0  
**Minecraft Version**: Paper 1.21.4