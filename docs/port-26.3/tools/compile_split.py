"""Split javac errors into client-looking and common code, and list the worst packages.

Usage: python -I compile_split.py <log> [top N]
"""
import collections
import re
import sys

ERROR = re.compile(r'^\s*(.*?\.java):(\d+): error: (.*)$')
CLIENT = re.compile(
    r'/(client|gui|render|renderer|models?|particles?|screen|widgets?|ledgers?|tooltips?|sprite|textures?|color|jei)/'
    r'|Screen\.java$|Renderer\.java$|Model\.java$|Widget\.java$|Particle\.java$|Gui[A-Z]\w*\.java$|Ledger\.java$',
    re.I)


def main():
    top = int(sys.argv[2]) if len(sys.argv) > 2 else 40
    sides, files = collections.Counter(), collections.defaultdict(int)
    for line in open(sys.argv[1], encoding='utf-8', errors='replace'):
        if line.startswith('* What went wrong'):
            break
        m = ERROR.match(line.rstrip('\n'))
        if not m:
            continue
        rel = m.group(1).replace(chr(92), '/').split('/java/forestry/')[-1]
        side = 'client' if CLIENT.search('/' + rel) else 'common'
        sides[side] += 1
        files[(side, rel)] += 1
    print('errors by side: %s; files by side: %s' % (dict(sides), dict(collections.Counter(k[0] for k in files))))
    packages = collections.Counter()
    for (side, rel), n in files.items():
        packages[side + ' ' + '/'.join(rel.split('/')[:-1][:4])] += n
    for key, n in packages.most_common(top):
        print('%5d %s' % (n, key))


if __name__ == '__main__':
    main()
