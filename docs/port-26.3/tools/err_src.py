"""Print javac errors with the source line they point at.

Usage: python -I err_src.py <log> <source root> [--side common|client] [--path REGEX] [--msg REGEX] [--ctx N]
"""
import argparse
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from compile_detail import CLIENT, ERROR


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('log')
    ap.add_argument('root')
    ap.add_argument('--side')
    ap.add_argument('--path')
    ap.add_argument('--msg')
    ap.add_argument('--ctx', type=int, default=0)
    a = ap.parse_args()
    path_rx = re.compile(a.path) if a.path else None
    msg_rx = re.compile(a.msg) if a.msg else None
    seen = set()
    cache = {}
    for line in open(a.log, encoding='utf-8', errors='replace'):
        if line.startswith('* What went wrong'):
            break
        m = ERROR.match(line.rstrip('\n'))
        if not m:
            continue
        rel = m.group(1).replace(chr(92), '/').split('/java/forestry/')[-1]
        no = int(m.group(2))
        side = 'client' if CLIENT.search('/' + rel) else 'common'
        if a.side and side != a.side:
            continue
        if path_rx and not path_rx.search(rel):
            continue
        if msg_rx and not msg_rx.search(m.group(3)):
            continue
        if (rel, no) in seen:
            continue
        seen.add((rel, no))
        if rel not in cache:
            cache[rel] = open(a.root + '/src/main/java/forestry/' + rel, encoding='utf-8').read().split('\n')
        src = cache[rel]
        print('%s:%d: %s' % (rel, no, m.group(3)[:110]))
        for i in range(max(0, no - 1 - a.ctx), min(len(src), no + a.ctx)):
            print('    %s%s' % ('>' if i == no - 1 else ' ', src[i].replace('\t', '  ')[:190]))


if __name__ == '__main__':
    main()
