# Sophisticated JEI Index

**Read this in other languages: [简体中文](README_CN.md)**

Adds a JEI Index Upgrade for Sophisticated Backpacks. When installed, JEI recipe transfer can pull ingredients from the first backpack in the selection order that has this upgrade enabled, in addition to the player inventory.

## Features

- ✅ Reuses JEI's built-in recipe transfer logic (works with any JEI transfer scenario)
- ✅ Extends ingredient source to the first backpack in the selection order with this upgrade enabled
- ✅ Priority follows Sophisticated Backpacks' inventory handler order (armor/offhand/main, plus compat handlers like Curios if installed)
- ✅ Supports shift-click max transfer and JEI complete-set semantics

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- Sophisticated Core (required by Sophisticated Backpacks)
- Sophisticated Backpacks
- JEI (Just Enough Items)
- Curios (optional, only needed if you want Curios slot support)

## Installation

1. Put this mod jar into the `mods` folder
2. Ensure required dependencies are installed
3. Start the game

## Usage

1. Equip a Sophisticated Backpack in any slot supported by Sophisticated Backpacks (armor/offhand/main or compat slots)
2. Put a JEI Index Upgrade into that backpack
3. Open any crafting container that supports JEI recipe transfer
4. Click the `+` button in JEI to transfer ingredients

## Notes

- JEI recipe transfer is a client action that sends a request to the server. For full functionality in multiplayer, JEI must be present on the server as well.
- The first backpack is resolved using the same order as Sophisticated Backpacks' B-key logic, but only backpacks with this upgrade enabled are considered.

## License

MIT. See [LICENSE](LICENSE).
