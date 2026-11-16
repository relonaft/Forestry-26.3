"""1.21.2: InteractionResultHolder and ItemInteractionResult were merged into InteractionResult.

Usage: python -I codemod_interaction_result.py <source root>...

Every call site in Forestry hands back the stack that is already in the hand, so the holder forms
map to the plain constants. sidedSuccess(...) becomes SUCCESS, as vanilla did.
"""
import os
import re
import sys

HOLDER_CALL = re.compile(r'InteractionResultHolder\.(success|consume|pass|fail|sidedSuccess)\(')
HOLDER_NEW = re.compile(r'new InteractionResultHolder<>(\()\s*InteractionResult\.(\w+)\s*,')
ITEM_SIDED = re.compile(r'ItemInteractionResult\.sidedSuccess\(')
NAMES = {'success': 'SUCCESS', 'consume': 'CONSUME', 'pass': 'PASS', 'fail': 'FAIL', 'sidedSuccess': 'SUCCESS'}
IMPORT = 'import net.minecraft.world.InteractionResult;'


def close_paren(text, start):
    depth = 0
    for i in range(start, len(text)):
        if text[i] == '(':
            depth += 1
        elif text[i] == ')':
            depth -= 1
            if depth == 0:
                return i
    raise ValueError('unbalanced')


def replace_calls(text, rx, open_index, name_of):
    out, pos = [], 0
    while True:
        m = rx.search(text, pos)
        if not m:
            out.append(text[pos:])
            return ''.join(out)
        out.append(text[pos:m.start()])
        out.append('InteractionResult.' + name_of(m))
        pos = close_paren(text, open_index(m)) + 1


def fix_imports(text):
    eol = '\r\n' if '\r\n' in text else '\n'
    lines = text.split(eol)
    seen, out = False, []
    for line in lines:
        if line == 'import net.minecraft.world.InteractionResultHolder;':
            continue
        if line == IMPORT:
            if seen:
                continue
            seen = True
        out.append(line)
    if not seen:
        first = next(i for i, line in enumerate(out) if line.startswith('import '))
        out.insert(first, IMPORT)
    return eol.join(out)


def main():
    files = 0
    for root in sys.argv[1:]:
        for base, _, names in os.walk(root):
            for n in names:
                if not n.endswith('.java'):
                    continue
                path = os.path.join(base, n)
                src = open(path, encoding='utf-8', newline='').read()
                if 'InteractionResultHolder' not in src and 'ItemInteractionResult' not in src:
                    continue
                res = replace_calls(src, HOLDER_CALL, lambda m: m.end() - 1, lambda m: NAMES[m.group(1)])
                res = replace_calls(res, HOLDER_NEW, lambda m: m.start(1), lambda m: m.group(2))
                res = replace_calls(res, ITEM_SIDED, lambda m: m.end() - 1, lambda m: 'SUCCESS')
                res = res.replace('ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION',
                                  'InteractionResult.TRY_WITH_EMPTY_HAND')
                res = res.replace('ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION', 'InteractionResult.PASS')
                res = re.sub(r'InteractionResultHolder<\s*ItemStack\s*>', 'InteractionResult', res)
                res = re.sub(r'\bItemInteractionResult\b', 'InteractionResult', res)
                res = fix_imports(res)
                if res != src:
                    open(path, 'w', encoding='utf-8', newline='').write(res)
                    files += 1
    print('files changed %d' % files)


if __name__ == '__main__':
    main()
