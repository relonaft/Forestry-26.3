"""Summarize a javac error log from Gradle: totals, error kinds, packages, missing symbols.

Usage: python -I compile_stats.py <log> [top N]
"""
import collections
import re
import sys

ERROR = re.compile(r'^\s*(.*?\.java):(\d+): error: (.*)$')
SYMBOL = re.compile(r'^\s+symbol:\s+(class|variable|method|constructor|static)\s+(.*)$')
PACKAGE = re.compile(r'^package ([\w.]+) does not exist$')


def module(path):
    p = path.replace('\\', '/')
    m = re.search(r'/src/(\w+)/java/forestry/(.*)$', p)
    if not m:
        return '?'
    parts = m.group(2).split('/')
    return m.group(1) + ':' + '.'.join(parts[:2] if len(parts) > 2 else parts[:1])


def kind(msg):
    msg = re.sub(r'\b[A-Z][\w.<>?,\[\] ]*\b', 'X', msg)
    return re.sub(r'\s+', ' ', msg)[:70]


def main():
    top = int(sys.argv[2]) if len(sys.argv) > 2 else 25
    kinds, modules, files = collections.Counter(), collections.Counter(), set()
    symbols, packages = collections.Counter(), collections.Counter()
    total = 0
    with open(sys.argv[1], encoding='utf-8', errors='replace') as f:
        for line in f:
            line = line.rstrip('\n')
            if line.startswith('* What went wrong'):
                break  # gradle repeats the whole compiler output after this line
            m = ERROR.match(line)
            if m:
                total += 1
                files.add(m.group(1))
                modules[module(m.group(1))] += 1
                p = PACKAGE.match(m.group(3))
                if p:
                    packages[p.group(1)] += 1
                    kinds['package X does not exist'] += 1
                else:
                    kinds[kind(m.group(3))] += 1
                continue
            s = SYMBOL.match(line)
            if s:
                symbols[s.group(1) + ' ' + s.group(2).split('(')[0].strip()] += 1
    print('errors: %d in %d files' % (total, len(files)))
    for title, counter in (('kinds', kinds), ('modules', modules), ('missing packages', packages),
                           ('missing symbols', symbols)):
        print('--- %s' % title)
        for key, n in counter.most_common(top):
            print('  %6d  %s' % (n, key))


if __name__ == '__main__':
    main()
