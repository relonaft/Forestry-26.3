"""Compare the species lists of original Forestry 5.8.2 (1.12.2) with Forestry: CE 1.21.1.

Usage: python -I matrix_species.py <original-1.12 root> <port root>

The original keeps species as enum constants in *Definition.java, the Community Edition as one JSON
file per species. Names are normalized to snake_case before the comparison.
"""
import os
import re
import sys

CONSTANT = re.compile(r'^\t([A-Z][A-Za-z0-9_]*)\(')


def snake(name):
    if name.isupper() or '_' in name:
        return name.lower()
    return re.sub(r'(?<=[a-z0-9])(?=[A-Z])', '_', name).lower()


def enum_constants(path):
    names = []
    with open(path, encoding='utf-8') as f:
        for line in f:
            m = CONSTANT.match(line)
            if m:
                names.append(m.group(1))
    return names


def json_names(*dirs):
    names = set()
    for d in dirs:
        if os.path.isdir(d):
            names.update(n[:-5] for n in os.listdir(d) if n.endswith('.json'))
    return names


def main():
    orig, port = sys.argv[1], sys.argv[2]
    java = os.path.join(orig, 'src', 'main', 'java', 'forestry')
    gen = os.path.join(port, 'src', 'generated')
    groups = [
        ('bees', os.path.join(java, 'apiculture', 'genetics', 'BeeDefinition.java'), 'bee_species'),
        ('trees', os.path.join(java, 'arboriculture', 'genetics', 'TreeDefinition.java'), 'tree_species'),
        ('butterflies', os.path.join(java, 'lepidopterology', 'genetics', 'ButterflyDefinition.java'),
         'butterfly_species'),
    ]
    roots = [os.path.join(gen, d, 'data', 'forestry') for d in os.listdir(gen)]
    for title, definition, folder in groups:
        original = {snake(n): n for n in enum_constants(definition)}
        ce = json_names(*[os.path.join(r, folder) for r in roots])
        only_orig = sorted(set(original) - ce)
        only_ce = sorted(ce - set(original))
        print('== %s: original %d, CE %d, common %d' % (title, len(original), len(ce), len(set(original) & ce)))
        print('   only in original (%d): %s' % (len(only_orig), ', '.join(only_orig)))
        print('   only in CE (%d): %s' % (len(only_ce), ', '.join(only_ce)))


if __name__ == '__main__':
    main()
