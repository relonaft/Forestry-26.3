"""Save the final report of every helper transcript of a session as a text file.

Usage: python -I save_helper_reports.py <session dir in ~/.claude/projects/...> <output dir>
"""
import glob
import json
import os
import sys


def texts(node):
    if isinstance(node, str):
        yield node
    elif isinstance(node, dict):
        for v in node.values():
            yield from texts(v)
    elif isinstance(node, list):
        for v in node:
            yield from texts(v)


def main():
    root, out = sys.argv[1], sys.argv[2]
    for path in sorted(glob.glob(os.path.join(root, '**', '*.jsonl'), recursive=True)):
        best = ''
        for line in open(path, encoding='utf-8', errors='replace'):
            try:
                rec = json.loads(line)
            except ValueError:
                continue
            msg = rec.get('message')
            if not isinstance(msg, dict) or msg.get('role') != 'assistant':
                continue
            for t in texts(msg.get('content')):
                if len(t) > 1500:
                    best = t
        if best:
            name = 'helper-report-%s.md' % os.path.basename(path)[:-6][-12:]
            open(os.path.join(out, name), 'w', encoding='utf-8').write(best)
            print(name, len(best), best[:70].replace('\n', ' '))


if __name__ == '__main__':
    main()
