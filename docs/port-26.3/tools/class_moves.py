"""Find the classes that moved between two Minecraft/NeoForge jars and keep their simple name.

Usage: python -I class_moves.py <old jar>[,<old jar>...] <new jar>[,<new jar>...] <out.json>

Writes {"moved": {old fqcn: new fqcn}, "gone": [...], "ambiguous": {old fqcn: [candidates]}}.
Only top-level classes under net/minecraft, com/mojang and net/neoforged are compared. A class is
"moved" when its old name is absent from the new jar and exactly one new class has its simple name.
"""
import collections
import json
import sys
import zipfile

ROOTS = ('net/minecraft/', 'com/mojang/', 'net/neoforged/')


def top_level(jars):
    names = set()
    for jar in jars.split(','):
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                if n.endswith('.class') and n.startswith(ROOTS) and '$' not in n and not n.endswith('package-info.class'):
                    names.add(n[:-6].replace('/', '.'))
    return names


def main():
    old, new = top_level(sys.argv[1]), top_level(sys.argv[2])
    by_simple = collections.defaultdict(list)
    for fqcn in new:
        by_simple[fqcn.rsplit('.', 1)[1]].append(fqcn)
    moved, gone, ambiguous = {}, [], {}
    for fqcn in sorted(old - new):
        candidates = by_simple.get(fqcn.rsplit('.', 1)[1], [])
        if len(candidates) == 1:
            moved[fqcn] = candidates[0]
        elif candidates:
            ambiguous[fqcn] = sorted(candidates)
        else:
            gone.append(fqcn)
    with open(sys.argv[3], 'w', encoding='utf-8') as f:
        json.dump({'moved': moved, 'gone': gone, 'ambiguous': ambiguous}, f, indent=1, sort_keys=True)
    print('old %d, new %d, unchanged %d, moved %d, ambiguous %d, gone %d' % (
        len(old), len(new), len(old & new), len(moved), len(ambiguous), len(gone)))


if __name__ == '__main__':
    main()
