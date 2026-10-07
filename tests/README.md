# JEI compatibility verification

Build each project with its configured JEI version, then inspect the same compiled
classes against each real JEI jar in the selected compatibility matrix:

```sh
python3 tests/check_jei_binary_compat.py versions/1.20.1 /path/to/jei-1.20.1-forge.jar
python3 tests/check_jei_binary_compat.py versions/1.21.1 /path/to/jei-1.21.1-neoforge.jar
python3 tests/check_jei_binary_compat.py versions/26.1.2 /path/to/jei-26.1.2-neoforge.jar
```

The checker reads class files without launching Minecraft. It checks direct JEI
class/member references and JEI Mixin injection method selectors, and requires an
applicable recipe handler entry point and packet receiver. Absent-target mixins
are excluded from symbol checks; their conditional loading must also be reviewed.
The checker also verifies explicit reflective packet-factory and result-context signatures.

To check the actual release artifact rather than local compiled classes, pass the
SJI jar as the third argument:

```sh
python3 tests/check_jei_binary_compat.py versions/1.21.1 /path/to/jei.jar /path/to/sophisticated_jei_index.jar
```

Check SB/Core references, inherited loader inventory methods, and the explicit
backpack reflection / payload injection contracts separately:

```sh
python3 tests/check_sophisticated_binary_compat.py 1.21.1 /path/to/sophisticated_jei_index.jar /path/to/backpacks.jar /path/to/core.jar /path/to/neoforge-universal.jar
```

Use the Forge universal jar for 1.20.1 and the NeoForge universal jar for the other
versions. The SB checker intentionally fails when the artifact's linked-backpack
bridge references classes absent from the supplied SB, even if ordinary backpack
references still resolve. It does not prove client synchronization or recipe-fill
behavior. See [the 2026-10-07 verification](../docs/compatibility-1.2.4-2026-10-07.md)
for a reproduced failure with the latest 1.21.1 SB.

This does **not** exercise Mixin transformation, networking, inventory mutation,
or gameplay. Before publishing compatibility claims, test client and dedicated
server with the same JEI version: ordinary and Shift recipe fill, missing items,
full inventories, split/count-sensitive ingredients, multiple backpacks, and
new-protocol result acknowledgements. Also check linked backpacks on 1.21.1 and
Inception plus custom slot stride on 26.1.2. Controllable interoperability needs
its own reproduction and is not established by these checks.
