# Sophisticated JEI Index

**Read this in other languages: [简体中文](README_CN.md)**

Adds a JEI Index Upgrade for Sophisticated Backpacks. When installed, JEI recipe transfer can pull ingredients from equipped backpacks that have this upgrade enabled (in selection order), in addition to the player inventory.

This repository now lives on one `main` branch. Each Minecraft/loader target is an independent Gradle project under `versions/`. There is no default-version build at the repository root.

## Version matrix

| Directory | Minecraft | Loader | JDK | Source commit | Nested backpacks | EMI packaged by default |
| --- | --- | --- | --- | --- | --- | --- |
| [`versions/1.20.1`](versions/1.20.1) | 1.20.1 | Forge 47.x | 17 | `b5653c3` (`forge-1.20.1`) | No | Yes |
| [`versions/1.21.1`](versions/1.21.1) | 1.21.1 | NeoForge 21.1+ | 21 | `c302040` (`neoforge-1.21.1`) | No | Yes |
| [`versions/26.1.2`](versions/26.1.2) | 26.1.2 | NeoForge 26.1.2.71+ | 25 | `4d246dc` (`neoforge-26.1.2`) | Yes (Inception, preserve this) | No (`include_emi_compat=false`) |

Shared config on every version:

- `maxEnabledBackpacksScanned` (common): limits how many enabled backpacks are scanned per player. `0` means unlimited.
- `enableTransferWithoutUpgrade` (common): when enabled, all equipped backpacks can provide recipe-transfer ingredients without installing the upgrade. The upgrade item stays registered for existing storages but is hidden from the creative tab and JEI ingredient list.

`26.1.2` only:

- Nested backpacks exposed by Sophisticated Backpacks' Inception Upgrade are supported when the outer indexed backpack can access them.
- `backpackSlotIdStride` (common): virtual slot-id space reserved for each indexed backpack source. Increase it if an Inception setup exposes more slots than the default. Multiplayer clients and servers must use the same value.

See [docs/multiversion-migration.md](docs/multiversion-migration.md) for the exact extracted commits and remaining version-specific differences. Gameplay/JEI behavior was not changed as part of this layout migration.

## Features

- Compatible with JEI native recipe transfer scenarios
- Uses enabled backpacks in the selection order as extra ingredient sources
- Also supports the crafting terminals of AE2, Refined Storage, Tom's Storage, and Beyond Dimensions (priority: network → player inventory → enabled backpacks in order)
- Supports shift-click max transfer and JEI complete-set semantics
- EMI recipe fill and recipe tree quick-craft are packaged on 1.20.1 and 1.21.1 (requires EMI on the server for multiplayer). 26.1.2 keeps EMI integration disabled until a compatible EMI build exists.

## Requirements

### `versions/1.20.1`

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- Sophisticated Core 1.20.1-1.3.6+ (required by Sophisticated Backpacks)
- Sophisticated Backpacks 3.24+
- JEI 15.58.0.209+ (Just Enough Items, Forge)
- EMI (optional)
- Curios (optional, only needed if you want Curios slot support)

### `versions/1.21.1`

- Minecraft 1.21.1
- NeoForge 21.1+
- Java 21
- Sophisticated Core 1.21.1-1.4.89+
- Sophisticated Backpacks 1.21.1-3.25.78+
- JEI 19.53.0.426+ (Just Enough Items, NeoForge)
- EMI (optional)
- Curios (optional, only needed if you want Curios slot support)

### `versions/26.1.2`

- Minecraft 26.1.2
- NeoForge 26.1.2.71+
- Java 25
- Sophisticated Core 26.1.2-1.4.76+
- Sophisticated Backpacks 26.1.2-3.25.76+
- JEI 29.37.0.98+ (Just Enough Items, NeoForge)
- AE2 26.1.x, Refined Storage 3.2.x, Beyond Dimensions 0.7.24+, and Tom's Storage 26.1 are optional integrations
- EMI integration is disabled on this version until EMI publishes a compatible 26.1.2 NeoForge build

## Build

Each version directory is a complete Gradle project with its own wrapper, `build.gradle`, `settings.gradle`, `gradle.properties`, and `src/`. Build inside that directory. Do not run Gradle from the repository root; the root has no project.

```sh
cd versions/1.20.1 && ./gradlew build
cd versions/1.21.1 && ./gradlew build
cd versions/26.1.2 && ./gradlew build
```

To build every version in one pass, use [scripts/build_all_versions.sh](scripts/build_all_versions.sh). Preferred JDK homes are:

| Variable | JDK | Version directory |
| --- | --- | --- |
| `SJI_JAVA_17_HOME` | 17 | `versions/1.20.1` |
| `SJI_JAVA_21_HOME` | 21 | `versions/1.21.1` |
| `SJI_JAVA_25_HOME` | 25 | `versions/26.1.2` |

If a matching `SJI_JAVA_*_HOME` is unset, that version falls back to `JAVA_HOME`. The fallback is only valid when `JAVA_HOME` is already the required major for that version. Set all three `SJI_JAVA_*_HOME` variables when building every version from one shell.

```sh
SJI_JAVA_17_HOME=/path/to/jdk-17 \
SJI_JAVA_21_HOME=/path/to/jdk-21 \
SJI_JAVA_25_HOME=/path/to/jdk-25 \
./scripts/build_all_versions.sh
```

Build only one version:

```sh
SJI_JAVA_21_HOME=/path/to/jdk-21 ./scripts/build_all_versions.sh 1.21.1
```

The script verifies `bin/java` and the reported major version before invoking that version's Gradle wrapper.

## Installation

1. Put the built jar from the matching `versions/<mc>/build/libs` directory into the `mods` folder
2. Ensure required dependencies are installed
3. Start the game

## Usage

1. Equip a Sophisticated Backpack in any slot supported by Sophisticated Backpacks (armor/offhand/main or compat slots)
2. Put a JEI Index Upgrade into that backpack
3. Open any crafting container that supports JEI recipe transfer
4. Click the `+` button in JEI to transfer ingredients

## Notes

- JEI recipe transfer is a client action that sends a request to the server. For full functionality in multiplayer, JEI must be present on the server as well.
- EMI recipe fill is a client action that sends a request to the server. For full functionality in multiplayer, EMI must be present on the server as well. The 26.1.2 project does not package EMI integration until EMI publishes a compatible build.
- The backpack selection order follows Sophisticated Backpacks' B-key logic, but only backpacks with this upgrade enabled are considered.
- Nested backpacks (for example via Inception Upgrade) are not ingredient sources on 1.20.1 or 1.21.1. Keep that limitation unless a later, explicit port is requested. 26.1.2 already supports them; preserve that behavior.
- Optional mod integrations are enabled only when the matching mod (and compatible version) is present.
- Tom's Storage is supported via JEI recipe transfer only. EMI is not supported.
- Historical branch refs (`forge-1.20.1`, `neoforge-1.21.1`, `neoforge-26.1.2`) remain as source snapshots. New work belongs on `main` under `versions/`.

## License

MIT. See [LICENSE](LICENSE).
