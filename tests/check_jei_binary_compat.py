#!/usr/bin/env python3
"""Check compiled SJI's direct JEI JVM symbols against one real JEI jar.

This is a linkage check, not a Minecraft/Mixin runtime or gameplay test.
Usage: python3 tests/check_jei_binary_compat.py versions/1.21.1 /path/to/jei.jar
"""
import re
import struct
import sys
import zipfile
from pathlib import Path


class ClassFile:
    def __init__(self, data):
        self.data, self.pos = data, 8
        self.cp = [None]
        count = self.u2()
        while len(self.cp) < count:
            tag = self.u1()
            if tag == 1:
                n = self.u2()
                value = self.read(n).decode('utf-8', errors='replace')
            elif tag in (7, 8, 16, 19, 20):
                value = self.u2()
            elif tag in (9, 10, 11, 12, 17, 18):
                value = (self.u2(), self.u2())
            elif tag in (3, 4):
                value = self.read(4)
            elif tag in (5, 6):
                value = self.read(8)
            elif tag == 15:
                value = (self.u1(), self.u2())
            else:
                raise ValueError(f'unknown constant pool tag {tag}')
            self.cp.append((tag, value))
            if tag in (5, 6):
                self.cp.append(None)
        self.u2()
        self.name = self.classname(self.u2())
        parent = self.u2()
        self.parents = [self.classname(parent)] if parent else []
        self.parents += [self.classname(self.u2()) for _ in range(self.u2())]
        self.fields = self.members()
        self.methods = self.members()

    def read(self, n):
        value = self.data[self.pos:self.pos+n]
        self.pos += n
        return value

    def u1(self):
        return self.read(1)[0]

    def u2(self):
        return struct.unpack('>H', self.read(2))[0]

    def u4(self):
        return struct.unpack('>I', self.read(4))[0]

    def utf(self, i):
        return self.cp[i][1]

    def classname(self, i):
        return self.utf(self.cp[i][1])

    def members(self):
        result = set()
        for _ in range(self.u2()):
            self.u2()
            result.add((self.utf(self.u2()), self.utf(self.u2())))
            for _ in range(self.u2()):
                self.u2()
                self.read(self.u4())
        return result


def main():
    project, jar = Path(sys.argv[1]), Path(sys.argv[2])
    with zipfile.ZipFile(jar) as archive:
        jei = {name[:-6]: ClassFile(archive.read(name)) for name in archive.namelist()
               if name.startswith('mezz/jei/') and name.endswith('.class')}
    failures, checked, inactive = [], 0, 0

    def has_member(owner, signature, is_field, seen=None):
        seen = set() if seen is None else seen
        if owner in seen or owner not in jei:
            return False
        seen.add(owner)
        cls = jei[owner]
        if signature in (cls.fields if is_field else cls.methods):
            return True
        return any(has_member(p, signature, is_field, seen) for p in cls.parents)

    for path in sorted((project / 'build/classes/java/main').rglob('*.class')):
        cls = ClassFile(path.read_bytes())
        source = project / 'src/main/java' / (cls.name.split('$')[0] + '.java')
        text = source.read_text() if source.exists() else ''
        target = re.search(r'@Mixin\(targets\s*=\s*"(mezz\.jei\.[^"]+)"', text)
        if target and target[1].replace('.', '/') not in jei:
            inactive += 1
            continue
        checked += 1
        for entry in cls.cp[1:]:
            if entry is None:
                continue
            tag, value = entry
            if tag == 7:
                name = cls.utf(value)
                if name.startswith('mezz/jei/') and name not in jei:
                    failures.append(f'{cls.name}: absent class {name}')
            elif tag in (9, 10, 11):
                owner = cls.classname(value[0])
                if not owner.startswith('mezz/jei/'):
                    continue
                nat = cls.cp[value[1]][1]
                signature = (cls.utf(nat[0]), cls.utf(nat[1]))
                if not has_member(owner, signature, tag == 9):
                    failures.append(f'{cls.name}: absent member {owner}.{signature[0]}{signature[1]}')
    # Check JEI-targeting injections against actual target methods. Optional
    # entry points are allowed to be absent, but the recipe handler must retain
    # at least one applicable entry point on every supported JEI build.
    packet_receivers = 0
    for source in sorted((project / 'src/main/java/com/sbjeiindex/mixin').glob('*.java')):
        text = source.read_text()
        target = re.search(r'@Mixin\(targets\s*=\s*"(mezz\.jei\.[^"]+)"', text)
        if target:
            target_name = target[1].replace('.', '/')
        else:
            value = re.search(r'@Mixin\(value\s*=\s*(\w+)\.class', text)
            imported = re.search(r'import (mezz\.jei\.[\w.]*\.' + value[1] + r');', text) if value else None
            if not imported:
                continue
            target_name = imported[1].replace('.', '/')
        if target_name not in jei:
            continue
        methods = jei[target_name].methods
        compiled_path = project / 'build/classes/java/main/com/sbjeiindex/mixin' / (source.stem + '.class')
        if compiled_path.exists():
            mixin = ClassFile(compiled_path.read_bytes())
            shadows = re.findall(r'@Shadow[^;{}]+?\b(\w+)\s*;', text, re.S)
            for field in mixin.fields:
                if field[0] in shadows and field not in jei[target_name].fields:
                    failures.append(f'{source.name}: missing shadow field {field} in {target_name}')
        active = 0
        for annotation in re.findall(r'@Inject\((.*?)\)\s*(?:private|public|protected)', text, re.S):
            selector = re.search(r'method\s*=\s*(?:"([^"]+)"|\{(.*?)\})', annotation, re.S)
            if not selector:
                continue
            selectors = [selector[1]] if selector[1] else re.findall(r'"([^"]+)"', selector[2])
            matches = any(any((name + desc == sel if '(' in sel else name == sel)
                              for name, desc in methods) for sel in selectors)
            active += bool(matches)
            if not matches and not re.search(r'require\s*=\s*0', annotation):
                failures.append(f'{source.name}: missing injection target {selectors} in {target_name}')
        if '/network/packets/' in target_name and 'PacketRecipeTransfer' in target_name and active:
            packet_receivers += 1
        if target_name.endswith('/BasicRecipeTransferHandler') and not active:
            failures.append(f'{source.name}: no applicable recipe transfer entry point')
    if not packet_receivers:
        failures.append('No applicable JEI recipe transfer packet receiver')
    # Reflective adapters cannot be checked through JVM member references, so
    # also verify their explicit constructor/factory/context contracts.
    is_forge = project.name == '1.20.1'
    packet_prefix = 'mezz/jei/common/network/packets/'
    lists = 'Ljava/util/Collection;' if is_forge else 'Ljava/util/List;'
    for owner, cls in jei.items():
        if not owner.startswith(packet_prefix):
            continue
        short = owner.rsplit('/', 1)[-1]
        if short not in {'PacketRecipeTransfer', 'PacketRecipeTransferCounted',
                         'PacketRecipeTransferWithResult', 'PacketRecipeTransferCountedWithResult'}:
            continue
        result = short.endswith('WithResult')
        params = lists * 3 + 'ZZ' + ('I' if result else '')
        signature = ('<init>', '(' + params + ')V') if is_forge else ('fromSlots', '(' + params + ')L' + owner + ';')
        if signature not in cls.methods:
            failures.append(f'Reflection contract absent: {owner}.{signature}')
        if not is_forge and ('TYPE', 'Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;') not in cls.fields:
            failures.append(f'Reflection packet TYPE absent: {owner}')
    context_name = 'mezz/jei/api/recipe/transfer/IRecipeTransferContext'
    if context_name in jei:
        for signature in [('getContainer', '()Lnet/minecraft/world/inventory/AbstractContainerMenu;'),
                          ('getRecipe', '()Ljava/lang/Object;'),
                          ('getRecipeSlots', '()Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;'),
                          ('getPlayer', '()Lnet/minecraft/world/entity/player/Player;'),
                          ('isMaxTransfer', '()Z'), ('getTransferId', '()I')]:
            if signature not in jei[context_name].methods:
                failures.append(f'Reflection context method absent: {signature}')
        result_name = packet_prefix + 'PacketRecipeTransferResult'
        for signature in [('<init>', '(IZ)V'), ('registerPendingRecipeTransfer', '(L' + context_name + ';)V')]:
            if result_name not in jei or signature not in jei[result_name].methods:
                failures.append(f'Reflection result method absent: {signature}')
        if is_forge and ('supportsRecipeTransferResults', '()Z') not in jei['mezz/jei/common/network/IConnectionToServer'].methods:
            failures.append('Reflection connection result-support method absent')
    failures = sorted(set(failures))
    for failure in failures:
        print(failure)
    print(f'{project.name} / {jar.name}: {checked} classes checked, {inactive} absent-target mixin classes skipped, {len(failures)} missing JEI symbols')
    if not checked:
        raise SystemExit('No compiled classes found; build the project first')
    raise SystemExit(bool(failures))


if __name__ == '__main__':
    main()
