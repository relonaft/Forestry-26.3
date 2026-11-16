"""Apply a regex replacement only on the source lines that javac reported with a given error.

Usage: python -I fix_at_errors.py <log> <error message regex> <line pattern> <replacement>

Safer than a blind search and replace: the compiler has already said the line is wrong and why.
"""
import collections
import re
import sys

ERROR = re.compile(r'^\s*(.*?\.java):(\d+): error: (.*)$')


def main():
    log, message, pattern, repl = sys.argv[1:5]
    message, pattern = re.compile(message), re.compile(pattern)
    targets = collections.defaultdict(set)
    for line in open(log, encoding='utf-8', errors='replace'):
        if line.startswith('* What went wrong'):
            break
        m = ERROR.match(line.rstrip('\n'))
        if m and message.search(m.group(3)):
            targets[m.group(1)].add(int(m.group(2)))
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
        open(path, 'w', encoding='utf-8', newline='').write('\n'.join(lines))
    print('files %d, lines changed %d, reported lines without a match %d' % (len(targets), changed, untouched))


if __name__ == '__main__':
    main()
