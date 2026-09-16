# JEI backward compatibility

SJI keeps the current compile dependencies while allowing a small selection of
older JEI releases. The minimums follow SJI's previous dependency updates rather
than attempting to support every JEI 15.x, 19.x, or 29.x release.

| Minecraft / loader | Compile JEI | Minimum allowed JEI | Binary verification samples |
| --- | --- | --- | --- |
| 1.20.1 Forge | 15.58.0.209 | 15.49.0.188 | 15.49.0.188, 15.49.0.191, 15.58.0.209 |
| 1.21.1 NeoForge | 19.53.0.426 | 19.44.0.403 | 19.44.0.403, 19.53.0.426 |
| 26.1.2 NeoForge | 29.37.0.98 | 29.20.0.60 | 29.20.0.60, 29.29.0.77, 29.37.0.98 |

Dependency ranges are minimum bounds, not a claim that every intermediate or
future release has been tested. Use the same JEI release on client and server.

## History and adaptation

Historical pins include Forge 15.49.0.188 (`c18dc37`), NeoForge 19.44.0.403
(`c1a59ac`), and 26.1.2 JEI 29.20.0.60 (`0edd080` / `4b8c9a8`). Much older pins
(15.20/15.21, 19.21/19.42, 29.6) are outside this compatibility scope.

- Both legacy `transferRecipe` and the new context-based entry point delegate to
  the existing backpack transfer planner. An injection group requires at least
  one valid entry point.
- Packet receivers cover original classes, relocated `legacy` classes, and new
  result-bearing classes. The Mixin plugin checks bytecode availability without
  prematurely loading targets.
- Optional packet factories, result registration, and result sending use narrow
  reflective adapters so old runtimes do not link absent result classes.
- Counted operations retain the counted protocol. NeoForge refuses a transfer
  when the required counted packet is unavailable instead of falling back to a
  basic packet that would lose quantities. Forge always uses counted packets.
- New JEI retains pending-transfer registration and server acknowledgements.
  Old JEI uses its legacy protocol and `setItems` path.
- Forge decoding shares the validated virtual-slot resolver and schedules
  inventory resolution/mutation on the server thread.
- Existing 1.21.1 linked-backpack synchronization and 26.1.2 Inception/slot-stride
  behavior remain unchanged.

## Validation and limits

- All three projects build with their current compile JEI and JDK 17/21/25.
- The same compiled output for each Minecraft version passes direct JEI JVM
  class/member linkage checks against every sample in the table, plus checks for
  applicable injection entry points, packet receivers, and shadow fields.
- Reflection signatures were compared with `javap` output from the real jars.
- These checks do **not** start Minecraft or exercise full Mixin transformation,
  inventory transactions, client/server packet round trips, or controller mods.
  Runtime recipe-fill tests are still needed before a release compatibility
  guarantee; see [tests/README.md](../tests/README.md).
- No claim is made that Controllable itself has been fixed. This change allows
  selecting an older JEI without SJI requiring the newest dependency baseline.
