# Schematics Directory

Place your WorldEdit schematic files (.schem) in this directory.

## Dungeon Type Schematics

For each dungeon type configured in `config.yml`, you need:

1. **Bell Schematic** - Small structure containing a bell
   - Example: `bell-ancient-mine.schem`
   - Should contain a bell block that players can interact with

2. **Dungeon Schematic** - Main dungeon structure
   - Example: `ancient-mine-dungeon.schem`
   - Can include spawn points for mobs and boss
   - Should be appropriately sized for combat

## Naming Convention

Follow the naming pattern defined in your config.yml under `dungeon-types`:
- `dungeon-types.{type-name}.schematic-bell`
- `dungeon-types.{type-name}.schematic-dungeon`

## Available Dungeon Types (Default Configuration)

1. **Ancient Mine** (Weight: 30)
   - `bell-ancient-mine.schem`
   - `ancient-mine-dungeon.schem`

2. **Cursed Forest** (Weight: 25)
   - `bell-cursed-forest.schem`
   - `cursed-forest-dungeon.schem`

3. **Frozen Cavern** (Weight: 20)
   - `bell-frozen-cavern.schem`
   - `frozen-cavern-dungeon.schem`

4. **Desert Tomb** (Weight: 15)
   - `bell-desert-tomb.schem`
   - `desert-tomb-dungeon.schem`

5. **Volcanic Depths** (Weight: 10)
   - `bell-volcanic-depths.schem`
   - `volcanic-depths-dungeon.schem`

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