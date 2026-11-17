"""Sum the token usage recorded in Claude Code transcripts.

Usage: python -I token_usage.py <projects dir of the session>

Reads every .jsonl under the directory (the main transcript and the helper transcripts). A reply is
written to the log once per content block with the same usage, so replies are counted once by id.
"""
import collections
import glob
import json
import os
import sys


def main():
    root = sys.argv[1]
    seen = set()
    total = collections.defaultdict(lambda: collections.Counter())
    for path in glob.glob(os.path.join(root, '**', '*.jsonl'), recursive=True):
        kind = 'main' if os.path.dirname(path) == root.rstrip('/\\') else 'helpers'
        for line in open(path, encoding='utf-8', errors='replace'):
            try:
                rec = json.loads(line)
            except ValueError:
                continue
            msg = rec.get('message')
            if not isinstance(msg, dict) or not isinstance(msg.get('usage'), dict):
                continue
            key = msg.get('id') or rec.get('uuid')
            if key in seen:
                continue
            seen.add(key)
            u = msg['usage']
            c = total[(kind, msg.get('model', '?'))]
            c['replies'] += 1
            c['input'] += u.get('input_tokens', 0)
            c['cache_write'] += u.get('cache_creation_input_tokens', 0)
            c['cache_read'] += u.get('cache_read_input_tokens', 0)
            c['output'] += u.get('output_tokens', 0)
    grand = collections.Counter()
    for (kind, model), c in sorted(total.items()):
        grand.update(c)
        print('%-8s %-22s replies %6d  input %11d  cache write %11d  cache read %13d  output %10d'
              % (kind, model, c['replies'], c['input'], c['cache_write'], c['cache_read'], c['output']))
    print('%-31s replies %6d  input %11d  cache write %11d  cache read %13d  output %10d'
          % ('total', grand['replies'], grand['input'], grand['cache_write'], grand['cache_read'], grand['output']))


if __name__ == '__main__':
    main()
