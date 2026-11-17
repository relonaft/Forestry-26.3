"""Rewrite item methods that changed between 1.21.1 and 26.3.

Usage: python -I codemod_item_methods.py <source root>...

- 1.21.5: Item.appendHoverText appends to a Consumer<Component> and gets a TooltipDisplay. The body
  keeps its list through forestry.core.platform.util.TooltipList. Block.appendHoverText is gone,
  those two overrides are skipped here and handled by hand
- 1.21.2: UseAnim is ItemUseAnimation
- 26.1: ItemStack.getItemHolder() is typeHolder()
"""
import os
import re
import sys

W = r'(\w+)'
HOVER = re.compile(
    r'(\tpublic void appendHoverText\(ItemStack %s, ((?:Item\.)?TooltipContext) %s, )List<Component> %s(, TooltipFlag %s\) \{\n)' % (W, W, W, W))
SKIP = ('AlvearyBlock.java', 'WorktableBlock.java')


def add_import(src, fq):
    line = 'import %s;' % fq
    if line in src:
        return src
    imports = list(re.finditer(r'^import [\w.]+;$', src, re.M))
    target = next((m for m in imports if m.group(0) > line), None)
    pos = target.start() if target else imports[-1].end() + 1
    return src[:pos] + line + '\n' + src[pos:]


def main():
    files = 0
    for root in sys.argv[1:]:
        for base, _, names in os.walk(root):
            for n in names:
                if not n.endswith('.java'):
                    continue
                path = os.path.join(base, n)
                src = open(path, encoding='utf-8', newline='').read()
                res = src
                m = HOVER.search(res)
                if m and n not in SKIP:
                    stack, ctx, lst, flag = m.group(2), m.group(4), m.group(5), m.group(7)
                    res = HOVER.sub(lambda k: k.group(1) + 'TooltipDisplay display, Consumer<Component> adder' + k.group(6)
                                    + '\t\tList<Component> %s = new TooltipList(adder);\n' % lst, res)
                    res = res.replace('super.appendHoverText(%s, %s, %s, %s);' % (stack, ctx, lst, flag),
                                      'super.appendHoverText(%s, %s, display, adder, %s);' % (stack, ctx, flag))
                    for fq in ('net.minecraft.world.item.component.TooltipDisplay', 'java.util.function.Consumer',
                               'forestry.core.platform.util.TooltipList', 'java.util.List'):
                        if fq.startswith('forestry.core.platform.util') and 'package forestry.core.platform.util;' in res:
                            continue
                        res = add_import(res, fq)
                if 'UseAnim' in res:
                    res = re.sub(r'\bUseAnim\b', 'ItemUseAnimation', res)
                res = res.replace('.getItemHolder()', '.typeHolder()')
                if res != src:
                    open(path, 'w', encoding='utf-8', newline='').write(res)
                    files += 1
    print('files changed %d' % files)


if __name__ == '__main__':
    main()
