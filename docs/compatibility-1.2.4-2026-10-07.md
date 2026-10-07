# 1.2.4 latest-dependency verification — 2026-10-07

The 1.21.1 linked-backpack failure recorded below is fixed in the subsequent
[1.2.5 compatibility update](linked-backpack-compat-1.2.5.md). This report retains
the original 1.2.4 artifact results.

## Result

**Full compatibility across all three versions is not established.** The existing
1.2.4 artifacts pass direct JEI and SB/Core binary linkage checks, and temporary
development clients pass targeted class-loading / Mixin transformation probes.
However, the 1.21.1 linked-backpack reflection bridge is unavailable with the
latest SB/Core. A probe that requires that bridge fails reproducibly.

This verification does not change the mod implementation or replace the release
jars. The artifact baseline is commit `7a9a065d50a99d5d50c62cd21169b585559427b8`.

## Exact dependency matrix

“Latest” means the newest published Modrinth version matching the exact Minecraft
version and loader on 2026-10-07, including Beta releases. All three selected JEI
versions are Beta; the SB and Core versions are releases. Downloaded jars were
verified against the SHA-512 hashes returned by the official Modrinth API.

| Minecraft / loader | JEI | Sophisticated Backpacks | Sophisticated Core | Test loader |
| --- | --- | --- | --- | --- |
| 1.20.1 Forge | [15.62.0.219](https://modrinth.com/mod/jei/version/uyTkeINn) | [1.20.1-3.26.9.2194](https://modrinth.com/mod/sophisticated-backpacks/version/nSVHlIzZ) | [1.20.1-1.5.6.2378](https://modrinth.com/mod/sophisticated-core/version/yIBA2SnN) | Forge 47.1.3 |
| 1.21.1 NeoForge | [19.57.0.451](https://modrinth.com/mod/jei/version/RI8WCow6) | [1.21.1-3.26.9.2195](https://modrinth.com/mod/sophisticated-backpacks/version/1pYxRkKq) | [1.21.1-1.5.7.2381](https://modrinth.com/mod/sophisticated-core/version/p09oohxN) | NeoForge 21.1.238 |
| 26.1.2 NeoForge | [29.43.0.107](https://modrinth.com/mod/jei/version/bEulRYY2) | [26.1.2-3.26.9.2193](https://modrinth.com/mod/sophisticated-backpacks/version/thjFLz7b) | [26.1.2-1.5.7.2377](https://modrinth.com/mod/sophisticated-core/version/MKrSdHBA) | NeoForge 26.1.2.114 |

Latest 26.1.2 JEI declares NeoForge **26.1.2.99+**, so the repository's original
26.1.2.76 development loader is insufficient for runtime verification of this
JEI. Latest SB requires Core 1.5.5+ on 1.20.1 and 1.5.6+ on the other versions;
update Core together with SB. JEI bundles its required MezzConfig dependency.

Input filenames, upstream URLs, timestamps, hashes, and unchanged SJI artifact
hashes are recorded in [the input manifest](compatibility-1.2.4-2026-10-07-inputs.json).

## Checks performed

| Check | 1.20.1 | 1.21.1 | 26.1.2 |
| --- | --- | --- | --- |
| Compile/package business code against latest JEI/SB/Core in isolated project copies | Pass | Pass | Pass |
| Existing release jar: JEI symbols, injection selectors, packet factories | Pass, 55 active classes | Pass, 62 active classes | Pass, 45 active classes |
| Existing release jar: direct SB/Core symbols, including inherited inventory methods | Pass, 27 unique references | Pass, 32 unique references | Pass, 32 unique references |
| SB reflection and payload injection contracts | Pass | **Fail: 2 linked-storage classes missing** | Pass |
| Development-client class-loading / key Mixin probe | Pass | Base probe passes; linked bridge assertion fails | Pass |

Each JEI check skips two packet mixin classes whose optional targets are absent
from the supplied JEI. The applicable packet targets and recipe handler entry
point are still required. The release-artifact checks use the original 1.2.4 jars,
not jars recompiled against the latest dependencies.

Temporary client probes run during client setup and deliberately load:

- `mezz.jei.library.transfer.BasicRecipeTransferHandler`
- `mezz.jei.common.transfer.TransferOperation`
- `mezz.jei.common.transfer.RecipeTransferUtil`
- `mezz.jei.common.network.packets.PacketRecipeTransferCountedWithResult`
- SB `BackpackWrapper` and Core `InventoryHandler`
- Core `StorageContainerMenuBase` and reflective `PlayerInventoryProvider.runOnBackpacks`
- SB `BackpackContentsPayload` on the NeoForge versions

They exit immediately after these checks. This exercises the JEI transformation
path implicated in the supplied crash, and the NeoForge SB payload Mixin, but
does not perform a recipe transaction. The Forge development copy explicitly
registers SJI's mixin config and uses deobfuscated flat-directory dependencies
with refmap remapping; raw production Forge jars in a development classpath are
not a valid runtime compatibility test.

The 1.21.1 development server also reached `Done` with the exact latest dependency
matrix and was stopped afterwards. This only establishes server startup.

## Reproduced 1.21.1 linked-backpack failure

`LinkedBackpackClientCompat.Support.find()` in 1.2.4 expects:

- `net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.ClientLinkedStorageBackpackContents`
- `net.p3pp3rf1y.sophisticatedbackpacks.network.RequestLinkedStorageBackpackContentsPayload`

Neither class exists in SB `1.21.1-3.26.9.2195`. New corresponding classes reside
in Core under `net.p3pp3rf1y.sophisticatedcore.linkedstorage`:

- `ClientLinkedStorageContents`
- `RequestLinkedStorageContentsPayload`

At runtime, the existing bridge catches the absent class and stores `SUPPORT=null`.
`resolveOrRequest` then returns `NOT_LINKED`. This is a silent loss of the dedicated
linked-backpack snapshot/request path, not an automatic game crash. Full linked
recipe-fill behavior must be adapted and tested before claiming support.

The strict client probe output was:

```text
SJI_COMPAT_PROBE loaded mezz.jei.library.transfer.BasicRecipeTransferHandler
SJI_COMPAT_PROBE loaded mezz.jei.common.transfer.TransferOperation
SJI_COMPAT_PROBE loaded mezz.jei.common.transfer.RecipeTransferUtil
SJI_COMPAT_PROBE loaded mezz.jei.common.network.packets.PacketRecipeTransferCountedWithResult
SJI_COMPAT_PROBE loaded net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper
SJI_COMPAT_PROBE loaded net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler
SJI_COMPAT_PROBE FAIL
java.lang.AssertionError: Linked backpack reflection bridge unavailable
```

Changing only the temporary probe to report the unavailable bridge instead of
asserting allows it to finish with `SJI_COMPAT_PROBE PASS`. The release bridge
and all mod business code remain unchanged in both runs.

## Reproduction and limits

```sh
python3 tests/check_jei_binary_compat.py versions/1.21.1 /path/to/jei-1.21.1-neoforge-19.57.0.451.jar /path/to/sophisticated_jei_index-1.2.4+1.21.1.jar
python3 tests/check_sophisticated_binary_compat.py 1.21.1 /path/to/sophisticated_jei_index-1.2.4+1.21.1.jar /path/to/sophisticatedbackpacks-1.21.1-3.26.9.2195.jar /path/to/sophisticatedcore-1.21.1-1.5.7.2381.jar /path/to/neoforge-21.1.238-universal.jar
```

The JEI command exits 0. The SB/Core command intentionally exits 1 with the two
missing reflective classes. Use the corresponding version's inputs for 1.20.1
and 26.1.2; those commands exit 0.

No ordinary/Shift recipe-fill transaction, missing/full inventory case,
multiplayer packet round trip, nested/Inception extraction, or optional terminal
mod interoperability was exercised. The successful probes establish targeted
loading compatibility, not complete gameplay support. Latest release-only JEI
versions were not part of this matrix.
