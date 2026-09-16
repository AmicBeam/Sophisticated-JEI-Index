# Multi-version migration

This document records the one-main-branch layout for Sophisticated JEI Index. Each Minecraft/loader target is an independent Gradle project under versions/. The historical branch refs were not rewritten.

## Source mapping

| Version directory | Historical branch | Extracted commit | Full SHA | Loader | JDK |
| --- | --- | --- | --- | --- | --- |
| versions/1.20.1 | forge-1.20.1 | b5653c3 | b5653c37e4a8a006bb41f820f84131ea1988be5b | Forge 47.x | 17 |
| versions/1.21.1 | neoforge-1.21.1 | c302040 | c302040618c7d03d68c15d4834f8a6cc0e14a6df | NeoForge 21.1+ | 21 |
| versions/26.1.2 | neoforge-26.1.2 | 4d246dc | 4d246dcb12073c18129e38e6c63fbc15d6d60048 | NeoForge 26.1.2.71+ | 25 |

main was created from neoforge-1.21.1 (c302040) before this layout change. Tracked version-specific root files from that snapshot were moved into versions/1.21.1. Root LICENSE and .gitignore stayed at the repository root; those files are also present inside each version tree because they were part of the extracted commits.

Extraction used read-only git archive <commit> into each version directory. Caches, build outputs, IDE files, run/, .gradle/, and other untracked local artifacts were not copied.

## Layout

    LICENSE
    .gitignore
    README.md
    README_CN.md
    AGENTS.md
    docs/multiversion-migration.md
    scripts/build_all_versions.sh
    versions/1.20.1/   # independent Forge 1.20.1 Gradle project
    versions/1.21.1/   # independent NeoForge 1.21.1 Gradle project
    versions/26.1.2/   # independent NeoForge 26.1.2 Gradle project

The repository root has no Gradle project and no default-version build. Build from versions/<mc>/ or scripts/build_all_versions.sh.

## Known feature differences

These differences already existed on the source commits. This migration copied them as-is and did not change gameplay or JEI behavior.

### Nested backpacks / Inception

- 1.20.1 and 1.21.1: nested backpacks (for example via Inception Upgrade) are not ingredient sources.
- 26.1.2: nested backpacks exposed by Inception are supported when the outer indexed backpack can access them. Preserve this.
- backpackSlotIdStride exists only on 26.1.2.

### Shared config

- All three versions have maxEnabledBackpacksScanned and enableTransferWithoutUpgrade.

### EMI

- 1.20.1 and 1.21.1 package EMI recipe-fill / recipe-tree integration.
- 26.1.2 keeps include_emi_compat=false and does not package EMI mixins or the EMI plugin until a compatible EMI 26.1.2 NeoForge build exists. EMI source still exists in that tree and is excluded by the Gradle source set when the flag is false.

### Loader / JEI / recipe transfer

- 1.20.1 is Forge, uses src/main/resources/META-INF/mods.toml, JEI 15.58.0.209+, and Forge-era recipe/tag paths (recipes/, tags/items/).
- 1.21.1 is NeoForge, uses generated META-INF/neoforge.mods.toml, JEI 19.53.0.426+, and includes JEI packet-compat helpers plus linked-backpack client sync (JeiRecipeTransferPacketCompat, JeiRecipeTransferResultSender, LinkedBackpackClientCompat, ClientBackpackContentsPrimer).
- 26.1.2 is NeoForge 26.1.2, Java 25, JEI 29.37.0.98+, and uses the 26.1 item-model path assets/sophisticated_jei_index/items/jei_index_upgrade.json. It does not include the 1.21.1 JEI packet-compat / linked-backpack helper set listed above.

### Optional integrations

- AE2, Refined Storage, Tom's Storage, and Beyond Dimensions remain optional integrations on all three versions when a matching build is present.
- Tom's Storage is JEI-only; EMI is not supported there.

## Intentionally changed files

Commit `f1c1e3b` contains byte-for-byte copies of the extracted commits under versions/<mc>/. Later commits may adapt JEI compatibility; see [jei-compatibility.md](jei-compatibility.md). Files that are supposed to differ from those snapshots are limited to repository-root layout, documentation, and scripts:

- Root README.md and README_CN.md now describe the version matrix and independent builds.
- New AGENTS.md, docs/multiversion-migration.md, and scripts/build_all_versions.sh.
- Root version-specific Gradle/source files were relocated into versions/1.21.1 rather than remaining at the repository root.

Historical branch refs were left untouched.

## Validation of the migration

All 215 tracked source-snapshot files matched their historical blobs before compatibility edits. All three independent projects built successfully with JDK 17/21/25 using cached dependencies. Local build outputs were not copied between projects or committed.
