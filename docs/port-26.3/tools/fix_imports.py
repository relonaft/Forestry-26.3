"""Rewrite imports of classes that moved or were renamed between 1.21.1 and 26.3.

Usage: python -I fix_imports.py <moves.json> <renames.json> <source root>...

moves.json comes from class_moves.py. renames.json is hand-written:
  {"classes": {old fqcn: new fqcn}, "text": {old token: new token}}
A class rename also replaces the old simple name in every file that imported it. "text" entries are
whole-word replacements applied to every file, for member renames that are safe to do blindly.
"""
import json
import os
import re
import sys

IMPORT = re.compile(r'^(import\s+(?:static\s+)?)([\w.]+?)((?:\.\*)?;.*)$')


def longest_prefix(name, table):
    parts = name.split('.')
    for i in range(len(parts), 0, -1):
        head = '.'.join(parts[:i])
        if head in table:
            return head, '.'.join(parts[i:])
    return None, None


def main():
    moved = json.load(open(sys.argv[1], encoding='utf-8'))['moved']
    renames = json.load(open(sys.argv[2], encoding='utf-8'))
    classes = dict(moved)
    classes.update(renames.get('classes', {}))
    text = [(re.compile(r'\b%s\b' % re.escape(k)), v) for k, v in renames.get('text', {}).items()]
    files = imports = renamed = 0
    for root in sys.argv[3:]:
        for base, _, names in os.walk(root):
            for n in names:
                if not n.endswith('.java'):
                    continue
                path = os.path.join(base, n)
                src = open(path, encoding='utf-8', newline='').read()
                out, simple = [], {}
                for line in src.split('\n'):
                    m = IMPORT.match(line.rstrip('\r'))
                    if m:
                        head, rest = longest_prefix(m.group(2), classes)
                        if head:
                            new = classes[head]
                            old_simple, new_simple = head.rsplit('.', 1)[1], new.rsplit('.', 1)[1]
                            if old_simple != new_simple:
                                simple[old_simple] = new_simple
                            cr = '\r' if line.endswith('\r') else ''
                            line = m.group(1) + new + ('.' + rest if rest else '') + m.group(3) + cr
                            imports += 1
                    out.append(line)
                res = '\n'.join(out)
                for old_simple, new_simple in simple.items():
                    res, k = re.subn(r'\b%s\b' % re.escape(old_simple), new_simple, res)
                    renamed += k
                for rx, repl in text:
                    res = rx.sub(repl, res)
                if res != src:
                    open(path, 'w', encoding='utf-8', newline='').write(res)
                    files += 1
    print('files changed %d, imports rewritten %d, simple names replaced %d' % (files, imports, renamed))


if __name__ == '__main__':
    main()
