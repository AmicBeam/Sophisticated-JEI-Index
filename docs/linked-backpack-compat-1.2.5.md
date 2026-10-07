# Minecraft 1.21.1 linked-backpack compatibility — 1.2.5

## Fix

Newer Sophisticated Backpacks moved its client linked-storage snapshot cache and
request payload into Sophisticated Core. The 1.2.4 bridge stopped detecting
linked backpacks after its two SB classes disappeared.

The 1.21.1 1.2.5 bridge resolves the cache and request class as a pair:

1. If the original SB pair exists, use it. This also keeps older SB resolvers on
   their own cache when a newer Core is installed.
2. Otherwise, use Core's `ClientLinkedStorageContents` and
   `RequestLinkedStorageContentsPayload`.

The revision lookup and `(UUID, long)` request constructor remain reflective,
preserving compatibility with earlier SB/Core installations. Existing snapshot
polling, pending resolution, and request throttling behavior are unchanged.

This release changes only the Minecraft 1.21.1 implementation/version. Artifact:
`sophisticated_jei_index-1.2.5+1.21.1.jar`. The release build uses the repository's
original dependency pins to avoid introducing a direct link to newer Core APIs.

## Validation — 2026-10-07

| Matrix | JEI | Sophisticated Backpacks | Sophisticated Core | Result |
| --- | --- | --- | --- | --- |
| Legacy | 19.53.0.426 | 1.21.1-3.26.2.2141 | 1.21.1-1.5.1.2333 | Artifact checks and functional client probe pass |
| Latest, including Beta | 19.57.0.451 | 1.21.1-3.26.9.2195 | 1.21.1-1.5.7.2381 | Artifact checks and functional client probe pass |

Both development clients use NeoForge 21.1.238 and JDK 21. Latest dependency
URLs/hashes are in the [2026-10-07 input manifest](compatibility-1.2.4-2026-10-07-inputs.json).

- The actual release jar passes direct JEI linkage and injection-selector checks
  against JEI 19.44.0.403, 19.53.0.426, and 19.57.0.451.
- The actual release jar passes direct SB/Core and reflective-contract checks
  with both complete dependency pairs. A mixed legacy-SB/latest-Core input also
  passes these static checks; it was not runtime-tested as a complete mod stack.
- The unchanged original 1.2.4 artifact still fails the latest-SB contract check
  with two absent classes. The checker does not accept the new API for a jar
  that lacks the corresponding reflective lookup strings.
- JDK 21 Gradle `build` passes with the original pinned dependencies.
- The release jar contains neither the probe nor its temporary lifecycle hook.

## Functional client probe

[LinkedBackpackCompatProbe.java](../tests/probes/LinkedBackpackCompatProbe.java)
is a development-only fixture. It runs in isolated copies of the 1.21.1 project,
after client setup, on a client tick. It verifies:

1. JEI recipe-handler/packet and SB contents-payload classes transform and load.
2. The bridge selects the expected legacy SB or relocated Core API.
3. A new linked endpoint without a snapshot remains linked and pending.
4. Installing a synthetic snapshot at revision 7 resolves a real linked wrapper
   with a 27-slot inventory.
5. Updating the snapshot to revision 8 is visible through the selected API.
6. Requests constructed for revisions -1, 7, and 8 carry the correct group UUID,
   known revision, and SB/Core payload namespace.

Successful output:

```text
SJI_LINKED_PROBE PASS api=sophisticatedcore pending/cached/revision/request/mixins
SJI_LINKED_PROBE PASS api=sophisticatedbackpacks pending/cached/revision/request/mixins
```

The clients have no world or network connection. The fixture loads default
server configuration via `ConfigTracker.loadDefaultServerConfigs()` and injects
synthetic snapshots. It suppresses network sending by prepopulating the bridge's
request throttle for the test group, then constructs and inspects the real
payloads separately. It exits automatically after the checks. This tests linked
snapshot resolution and request construction, not real network transport.

To reproduce, copy the probe into `src/main/java/com/sbjeiindex/` in a temporary
project copy and register it from the existing client-setup callback:

```java
NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> LinkedBackpackCompatProbe.run());
```

Set `neoForge.runs.client.systemProperty "sji.test.expectedLinkedApi", "sophisticatedcore"`
for the latest matrix, or `"sophisticatedbackpacks"` for the legacy matrix, then
run the client's Gradle task with the corresponding dependencies. Keep this
fixture and hook out of the release source set.

Ordinary/Shift JEI recipe-fill transactions, actual server acknowledgements,
multiplayer synchronization, and optional terminal integrations were not
exercised by this test. Those broader gameplay claims remain outside this fix's
verified scope.
