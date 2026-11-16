"""List items and blocks of original Forestry 5.8.2 whose English name is absent from Forestry: CE 1.21.1.

Usage: python -I matrix_content.py <original en_us.lang> <CE core jar>

Ids were renamed between 1.12 and 1.21, so the comparison is by display name. A missing name is a
candidate: the thing may be renamed, generated at runtime, or really gone.
"""
import json
import re
import sys
import zipfile


def norm(name):
    name = name.lower().replace("’", "'").replace("'", '')
    return re.sub(r'[^a-z0-9%]+', ' ', name).strip()


def original_names(path):
    out = {}
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n').rstrip('\r')
            if not line or line.startswith('#') or '=' not in line:
                continue
            key, value = line.split('=', 1)
            if key.startswith(('item.', 'tile.')) and key.endswith('.name'):
                out[key[:-5]] = value
    return out


def ce_names(jar):
    with zipfile.ZipFile(jar) as z:
        lang = json.loads(z.read('assets/forestry/lang/en_us.json').decode('utf-8'))
    out = {}
    for key, value in lang.items():
        if key.startswith(('item.forestry.', 'block.forestry.', 'fluid.forestry.', 'fluid_type.forestry.')):
            out[key] = value
    return out, lang


def main():
    original = original_names(sys.argv[1])
    ce, lang = ce_names(sys.argv[2])
    ce_norm = {norm(v) for v in ce.values()}
    all_norm = {norm(v) for v in lang.values() if isinstance(v, str)}
    missing = [(k, v) for k, v in sorted(original.items()) if norm(v) not in ce_norm]
    print('original item/tile names: %d, CE item/block/fluid names: %d' % (len(original), len(ce)))
    print('original names absent from CE items/blocks: %d' % len(missing))
    for key, value in missing:
        elsewhere = ' (name exists under another CE key)' if norm(value) in all_norm else ''
        print('  %-58s %s%s' % (key, value, elsewhere))

    if len(sys.argv) > 3:
        # a name in the lang file proves nothing, the creative tabs list what is really registered
        with open(sys.argv[3], encoding='utf-8') as f:
            tab_ids = {line.split()[1] for line in f if len(line.split()) >= 2}
        by_name = {}
        for key, value in ce.items():
            by_name.setdefault(norm(value), []).append(key)
        ghosts = []
        for key, value in sorted(original.items()):
            keys = by_name.get(norm(value))
            if keys and not any('forestry:' + k.split('.', 2)[2] in tab_ids for k in keys):
                ghosts.append((key, value, keys))
        print('named in CE lang but absent from the creative tabs: %d' % len(ghosts))
        for key, value, keys in ghosts:
            print('  %-50s %-28s %s' % (key, value, ', '.join(keys)))


if __name__ == '__main__':
    main()
