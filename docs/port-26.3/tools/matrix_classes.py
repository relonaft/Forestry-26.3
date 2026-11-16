"""Compare class families of original Forestry 5.8.2 (1.12.2) with Forestry: CE 1.21.1 by name.

Usage: python -I matrix_classes.py <original-1.12 root> <port root>

A name match proves nothing about behavior. The output is a list of candidates to read, not a verdict.
"""
import os
import re
import sys


def classes(root):
    out = {}
    for base, _, files in os.walk(os.path.join(root, 'src')):
        if os.sep + 'test' + os.sep in base + os.sep:
            continue
        for f in files:
            if f.endswith('.java'):
                out[f[:-5]] = os.path.relpath(os.path.join(base, f), root).replace(os.sep, '/')
    return out


def family(names, pattern):
    rx = re.compile(pattern)
    found = {}
    for n in names:
        m = rx.match(n)
        if m and m.group(1):
            found[m.group(1).lower()] = n
    return found


FAMILIES = [
    ('block entities', r'^Tile(?:Entity)?([A-Z]\w*)$', r'^(?:Tile)?([A-Z]\w*?)BlockEntity$'),
    ('screens', r'^Gui([A-Z]\w*)$', r'^([A-Z]\w*?)Screen$'),
    ('containers', r'^Container([A-Z]\w*)$', r'^([A-Z]\w*?)Menu$'),
    ('items', r'^Item([A-Z]\w*)$', r'^(?:Item)?([A-Z]\w*?)(?:Item)?$'),
    ('blocks', r'^Block([A-Z]\w*)$', r'^(?:Block)?([A-Z]\w*?)(?:Block)?$'),
]


def main():
    orig = classes(sys.argv[1])
    port = classes(sys.argv[2])
    for title, orig_rx, port_rx in FAMILIES:
        a = family(orig, orig_rx)
        b = family(port, port_rx)
        missing = sorted(k for k in a if k not in b)
        print('== %s: original %d, matched by name %d, unmatched %d' % (
            title, len(a), len(a) - len(missing), len(missing)))
        print('   ' + ', '.join(a[k] for k in missing))


if __name__ == '__main__':
    main()
