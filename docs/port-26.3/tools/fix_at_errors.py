"""Apply a regex replacement only on the source lines that javac reported with a given error.

Usage: python -I fix_at_errors.py <log> <error message regex> <line pattern> <replacement>

Safer than a blind search and replace: the compiler has already said the line is wrong and why.
"""
import collections
import re
import sys
import time

ERROR = re.compile(r'^\s*(.*?\.java):(\d+): error: (.*)$')


def main():
    log, message, pattern, repl = sys.argv[1:5]
    message, pattern = re.compile(message), re.compile(pattern)
    targets = collections.defaultdict(set)
    errors, cur = [], None
    for line in open(log, encoding='utf-8', errors='replace'):
        line = line.rstrip('\n')
        if line.startswith('* What went wrong'):
            break
        m = ERROR.match(line)
        if m:
            cur = [m.group(1), int(m.group(2)), m.group(3)]
            errors.append(cur)
        elif cur is not None and line.startswith('  ') and ('symbol:' in line or 'location:' in line
                                                          or 'required:' in line or 'found:' in line):
            # the message regex also sees javac's detail lines
            cur[2] += ' | ' + line.strip()
    for path, number, text in errors:
        if message.search(text):
            targets[path].add(number)
    changed = untouched = 0
    for path, numbers in targets.items():
        lines = open(path, encoding='utf-8', newline='').read().split('\n')
        for n in numbers:
            new = pattern.sub(repl, lines[n - 1])
            if new != lines[n - 1]:
                lines[n - 1] = new
                changed += 1
            else:
                untouched += 1
        for attempt in range(5):
            try:
                open(path, 'w', encoding='utf-8', newline='').write('\n'.join(lines))
                break
            except OSError:
                # a file written a moment ago can still be locked by a scanner on Windows
                time.sleep(0.5)
        else:
            raise OSError('cannot write ' + path)
    print('files %d, lines changed %d, reported lines without a match %d' % (len(targets), changed, untouched))


if __name__ == '__main__':
    main()
