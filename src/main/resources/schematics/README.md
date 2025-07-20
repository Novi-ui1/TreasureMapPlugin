# Schematics Directory

Place your WorldEdit schematic files (.schem) in this directory.

## Required Schematics

For each skill configured in `config.yml`, you need:

1. **Bell Schematic** - Small structure containing a bell
   - Example: `bell-mining.schem`
   - Should contain a bell block that players can interact with

2. **Dungeon Schematic** - Main dungeon structure
   - Example: `mining-dungeon.schem`
   - Can include spawn points for mobs and boss
   - Should be appropriately sized for combat

## Naming Convention

Follow the naming pattern defined in your config.yml:
- `skills.{skill-name}.dungeon.schematic-bell`
- `skills.{skill-name}.dungeon.schematic-dungeon`

## Creating Schematics

1. Build your structure in-game
2. Use WorldEdit to select the area: `//wand`
3. Select with `//pos1` and `//pos2`
4. Copy with `//copy`
5. Save with `//schem save {filename}`
6. Place the file in this directory

## Tips

- Keep bell schematics small (5x5x5 or smaller)
- Dungeon schematics can be larger but consider performance
- Include air blocks around structures for proper placement
- Test schematics thoroughly before deploying to production