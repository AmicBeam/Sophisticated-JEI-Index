# AGENTS.md

This repository is a one-`main`-branch, multi-version Minecraft mod. Each Minecraft/loader target is an independent Gradle project under `versions/`. The repository root has no default-version build.

## Version coverage

Unless a request is explicitly limited to one Minecraft/loader version, cover all three projects:

- `versions/1.20.1` (Forge, JDK 17)
- `versions/1.21.1` (NeoForge, JDK 21)
- `versions/26.1.2` (NeoForge, JDK 25)

Do not treat the current checkout, a historical branch, or the most recently edited directory as the only target.

## Independent builds

Each `versions/<mc>/` tree is a complete Gradle project: wrapper, `build.gradle`, `settings.gradle`, `gradle.properties`, and `src/`. Build inside that directory. Do not add a root Gradle project that would pick a default version.

Preferred JDK homes:

- `SJI_JAVA_17_HOME` for `versions/1.20.1`
- `SJI_JAVA_21_HOME` for `versions/1.21.1`
- `SJI_JAVA_25_HOME` for `versions/26.1.2`

If a matching `SJI_JAVA_*_HOME` is unset, that version may fall back to `JAVA_HOME` only when `JAVA_HOME` already matches the required major. Use `scripts/build_all_versions.sh` for the portable multi-version build path.

Keep `gradlew` executable in every version directory.

## Config / feature matrix

Preserve current version-specific behavior unless a later change explicitly ports a feature.

Shared on every version:

- `maxEnabledBackpacksScanned`
- `enableTransferWithoutUpgrade`

Nested backpack / Inception support currently exists only on `versions/26.1.2`, together with `backpackSlotIdStride`. Keep that support on 26.1.2. Do not silently add or remove it on 1.20.1 or 1.21.1.

EMI recipe-fill integration is packaged on 1.20.1 and 1.21.1. 26.1.2 keeps `include_emi_compat=false` until a compatible EMI build exists.

## Source layout

- Root tracked files that belong here: `LICENSE`, `.gitignore`, `README.md`, `README_CN.md`, `AGENTS.md`, `docs/`, `scripts/`, and `versions/`.
- Version-specific source, resources, wrappers, and Gradle files live only under `versions/<mc>/`.
- Historical branch refs (`forge-1.20.1`, `neoforge-1.21.1`, `neoforge-26.1.2`) are source snapshots. Do not rewrite them as part of ordinary work.
- After edits and validation, commit and push the task changes unless the user explicitly asks otherwise. Keep historical version branches unchanged; ongoing development belongs on main or a task branch based on main.

See [docs/multiversion-migration.md](docs/multiversion-migration.md) for the extracted source commits and remaining known differences.
