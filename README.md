# Sophisticated JEI Index

**Read this in other languages: [简体中文](README_CN.md)**

Adds a JEI Index Upgrade for Sophisticated Backpacks. When installed, JEI recipe transfer can pull ingredients from equipped backpacks that have this upgrade enabled (in selection order), in addition to the player inventory.

## Features

- ✅ Compatible with JEI native recipe transfer scenarios
- ✅ Uses enabled backpacks in the selection order as extra ingredient sources
- ✅ Also supports the crafting terminals of the following mods (priority: network → player inventory → enabled backpacks in order): AE2, Refined Storage, Tom's Storage
- ✅ Supports shift-click max transfer and JEI complete-set semantics
- ✅ Compatible with EMI recipe fill and recipe tree quick-craft on branches with a matching EMI build (requires EMI on the server for multiplayer)

## Requirements

This project maintains separate branches per Minecraft/loader version:

- **Forge (Minecraft 1.20.1)**: use branch `forge-1.20.1`
  - Minecraft 1.20.1
  - Forge 47.x
  - Sophisticated Core 1.20.1-1.3.6+ (required by Sophisticated Backpacks)
  - Sophisticated Backpacks 3.24+
  - JEI 15.x (Just Enough Items, Forge)
  - EMI (optional)
  - Curios (optional, only needed if you want Curios slot support)
- **NeoForge (Minecraft 1.21.x)**: use branch `neoforge-1.21.1`
  - Minecraft 1.21.x
  - NeoForge 21.1+
  - Sophisticated Core 1.21.1+ (required by Sophisticated Backpacks)
  - Sophisticated Backpacks 1.21.1+
  - JEI 19.x (Just Enough Items, NeoForge)
  - EMI (optional)
  - Curios (optional, only needed if you want Curios slot support)
- **NeoForge (Minecraft 26.1.2)**: use branch `neoforge-26.1.2`
  - Minecraft 26.1.2
  - NeoForge 26.1.2.71+
  - Java 25
  - Sophisticated Core 26.1.2-1.4.76+
  - Sophisticated Backpacks 26.1.2-3.25.76+
  - JEI 29.6.x (Just Enough Items, NeoForge)
  - AE2 26.1.x, Refined Storage 3.2.x, Beyond Dimensions 0.7.24+, and Tom's Storage 26.1 are optional integrations
  - EMI integration is disabled on this branch until EMI publishes a compatible 26.1.2 NeoForge build

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
- EMI recipe fill is a client action that sends a request to the server. For full functionality in multiplayer, EMI must be present on the server as well. The 26.1.2 branch does not package EMI integration until EMI publishes a compatible build.
- The backpack selection order follows Sophisticated Backpacks' B-key logic, but only backpacks with this upgrade enabled are considered.
- Config option: `maxEnabledBackpacksScanned` (common config). Limits how many enabled backpacks are scanned per player. 0 means unlimited.
- Nested backpacks exposed by Sophisticated Backpacks' Inception Upgrade are supported when the outer indexed backpack can access them.
- Config option: `backpackSlotIdStride` (common config). Controls the virtual slot id space reserved for each indexed backpack source. Increase it if an Inception setup exposes more slots than the default; multiplayer clients and servers must use the same value.
- Config option: `enableTransferWithoutUpgrade` (common config). When enabled, all equipped backpacks can provide recipe transfer ingredients without installing the upgrade; the upgrade item is hidden from the creative tab and JEI ingredient list but remains registered for existing storages.
- Optional mod integrations are enabled only when the matching mod (and compatible version) is present.
- On the 26.1.2 branch, EMI integration is not packaged by default because no compatible EMI 26.1.2 NeoForge build is available yet.
- Tom's Storage is supported via JEI recipe transfer only. EMI is not supported.

## License

MIT. See [LICENSE](LICENSE).
