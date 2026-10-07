#!/usr/bin/env python3
"""Check an existing SJI jar's SB/Core symbols and explicit reflective contracts.

Usage: check_sophisticated_binary_compat.py minecraft sji.jar sb.jar core.jar loader.jar
This does not run Mixin or perform inventory transactions.
"""
import argparse
import re
import zipfile

from check_jei_binary_compat import ClassFile


PREFIXES = ('net/p3pp3rf1y/sophisticatedbackpacks/', 'net/p3pp3rf1y/sophisticatedcore/')
SB = PREFIXES[0]
CORE = PREFIXES[1]


def classes(jar):
    with zipfile.ZipFile(jar) as archive:
        return {name[:-6]: ClassFile(archive.read(name)) for name in archive.namelist()
                if name.endswith('.class') and not name.startswith('META-INF/')}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('minecraft', choices=['1.20.1', '1.21.1', '26.1.2'])
    for name in ('sji', 'backpacks', 'core', 'loader'):
        parser.add_argument(name)
    args = parser.parse_args()
    dependency = {}
    for jar in (args.backpacks, args.core, args.loader):
        dependency.update(classes(jar))
    compiled = classes(args.sji)
    missing, references = set(), set()

    def member(owner, signature, field=False, seen=None):
        seen = set() if seen is None else seen
        if owner in seen or owner not in dependency:
            return False
        seen.add(owner)
        cls = dependency[owner]
        if signature in (cls.fields if field else cls.methods):
            return True
        return any(member(parent, signature, field, seen) for parent in cls.parents)

    for cls in compiled.values():
        for entry in cls.cp[1:]:
            if entry is None:
                continue
            tag, value = entry
            if tag == 7:
                owner = cls.utf(value)
                if owner.startswith(PREFIXES):
                    references.add(('class', owner))
                    if owner not in dependency:
                        missing.add(f'{cls.name}: absent class {owner}')
            elif tag in (9, 10, 11):
                owner = cls.classname(value[0])
                if owner.startswith(PREFIXES):
                    nat = cls.cp[value[1]][1]
                    signature = (cls.utf(nat[0]), cls.utf(nat[1]))
                    references.add(('member', owner, signature))
                    if not member(owner, signature, tag == 9):
                        missing.add(f'{cls.name}: absent member {owner}.{signature}')
            elif tag == 1:
                for owner in re.findall(r'L(net/p3pp3rf1y/sophisticated(?:backpacks|core)/[^;<]+)', value):
                    if owner not in dependency:
                        missing.add(f'{cls.name}: absent descriptor type {owner}')

    contracts = set()

    def require_class(owner):
        if owner not in dependency:
            contracts.add(f'absent reflective class {owner}')

    def require_method(owner, signature):
        require_class(owner)
        if owner in dependency and not member(owner, signature):
            contracts.add(f'absent reflective/injection method {owner}.{signature}')

    require_class(CORE + 'common/gui/StorageContainerMenuBase')
    provider = SB + 'util/PlayerInventoryProvider'
    params = '(Lnet/minecraft/world/entity/player/Player;L' + provider + '$BackpackInventorySlotConsumer;)'
    require_class(provider)
    if provider in dependency and not any(member(provider, ('runOnBackpacks', params + ret)) for ret in ('V', 'Z')):
        contracts.add('absent reflective PlayerInventoryProvider.runOnBackpacks(Player, consumer)')

    if args.minecraft != '1.20.1':
        payload = SB + 'network/BackpackContentsPayload'
        require_method(payload, ('handlePayload', '(L' + payload + ';Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V'))

    # Check only the bridge actually present in the artifact. It is optional on
    # older SB releases, but disappearing on a newer SB breaks linked support.
    if 'com/sbjeiindex/util/LinkedBackpackClientCompat' in compiled:
        component = CORE + 'init/ModCoreDataComponents'
        require_class(component)
        if component in dependency and not member(component, ('LINKED_STORAGE_ENDPOINT', 'Ljava/util/function/Supplier;'), True):
            contracts.add('absent reflective field ModCoreDataComponents.LINKED_STORAGE_ENDPOINT')
        require_method(CORE + 'linkedstorage/LinkedStorageEndpointData', ('groupId', '()Ljava/util/UUID;'))
        require_method(SB + 'backpack/wrapper/BackpackLinkedStorageResolver',
                       ('resolve', '(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;'))
        require_method(SB + 'backpack/wrapper/ClientLinkedStorageBackpackContents',
                       ('getRevision', '(Ljava/util/UUID;)Ljava/util/Optional;'))
        require_method(SB + 'network/RequestLinkedStorageBackpackContentsPayload', ('<init>', '(Ljava/util/UUID;J)V'))

    for failure in sorted(missing):
        print('LINKAGE:', failure)
    for failure in sorted(contracts):
        print('CONTRACT:', failure)
    print(f'{args.minecraft}: {len(compiled)} artifact classes, {len(references)} SB/Core references, '
          f'{len(missing)} missing direct symbols, {len(contracts)} missing reflective/injection contracts')
    if not compiled or not references:
        raise SystemExit('No SJI classes or SB/Core references found')
    raise SystemExit(bool(missing or contracts))


if __name__ == '__main__':
    main()
