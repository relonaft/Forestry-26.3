"""List javac errors with their detail lines, filtered by side, path and message.

Usage: python -I compile_detail.py <log> [--side common|client] [--path REGEX] [--msg REGEX] [--top N] [--list N]

Prints the most frequent (message + symbol/location) groups, then optionally the first N raw errors.
"""
import argparse
import collections
import re

ERROR = re.compile(r'^\s*(.*?\.java):(\d+): error: (.*)$')
CLIENT = re.compile(
    r'/(client|gui|render|renderer|models?|particles?|screen|widgets?|ledgers?|tooltips?|sprite|textures?|color|jei)/'
    r'|Screen\.java$|Renderer\.java$|Model\.java$|Widget\.java$|Particle\.java$|Gui[A-Z]\w*\.java$|Ledger\.java$',
    re.I)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('log')
    ap.add_argument('--side')
    ap.add_argument('--path')
    ap.add_argument('--msg')
    ap.add_argument('--top', type=int, default=40)
    ap.add_argument('--list', type=int, default=0)
    a = ap.parse_args()
    path_rx = re.compile(a.path) if a.path else None
    msg_rx = re.compile(a.msg) if a.msg else None
    errors, cur = [], None
    for line in open(a.log, encoding='utf-8', errors='replace'):
        line = line.rstrip('\n')
        if line.startswith('* What went wrong'):
            break
        m = ERROR.match(line)
        if m:
            rel = m.group(1).replace(chr(92), '/').split('/java/forestry/')[-1]
            cur = {'file': rel, 'line': int(m.group(2)), 'msg': m.group(3), 'detail': []}
            errors.append(cur)
        elif cur is not None and line.startswith('  ') and len(cur['detail']) < 6:
            cur['detail'].append(line.strip())
    sel = []
    for e in errors:
        side = 'client' if CLIENT.search('/' + e['file']) else 'common'
        if a.side and side != a.side:
            continue
        if path_rx and not path_rx.search(e['file']):
            continue
        full = e['msg'] + ' | ' + ' | '.join(d for d in e['detail'] if d.startswith(('symbol', 'location', 'required', 'found')))
        if msg_rx and not msg_rx.search(full):
            continue
        e['full'] = full
        sel.append(e)
    print('selected %d of %d errors in %d files' % (len(sel), len(errors), len({e['file'] for e in sel})))
    groups = collections.Counter()
    for e in sel:
        key = re.sub(r'location: .*', '', e['full'])
        groups[key.strip(' |')[:150]] += 1
    for key, n in groups.most_common(a.top):
        print('%5d  %s' % (n, key))
    for e in sel[:a.list]:
        print('%s:%d: %s' % (e['file'], e['line'], e['full'][:220]))


if __name__ == '__main__':
    main()
