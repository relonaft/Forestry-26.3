"""Rewrite the signatures of the block methods that changed between 1.21.1 and 26.3.

Usage: python -I codemod_block_methods.py <source root>...

Only the parameter lists and the matching super calls change. Bodies that need more than that are
edited by hand afterwards.
- 1.21.2: updateShape (new order, ScheduledTickAccess, RandomSource), neighborChanged (Orientation
  instead of the changed position)
- 1.21.5: entityInside (InsideBlockEffectApplier, isPrecise), getCloneItemStack, playerDestroy
- 26.3: BonemealableBlock methods take a BonemealSource, fallOn takes a double
"""
import os
import re
import sys

W = r'(\w+)'
RULES = [
    # (pattern, replacement, imports)
    (re.compile(r'BlockState updateShape\(BlockState %s, Direction %s, BlockState %s, LevelAccessor %s, BlockPos %s, BlockPos %s\) \{' % (W, W, W, W, W, W)),
     r'BlockState updateShape(BlockState \1, LevelReader \4, ScheduledTickAccess scheduledTicks, BlockPos \5, Direction \2, BlockPos \6, BlockState \3, RandomSource random) {',
     ['net.minecraft.world.level.LevelReader', 'net.minecraft.world.level.ScheduledTickAccess', 'net.minecraft.util.RandomSource']),
    (re.compile(r'super\.updateShape\(%s, %s, %s, %s, %s, %s\)' % (W, W, W, W, W, W)),
     r'super.updateShape(\1, \4, scheduledTicks, \5, \2, \6, \3, random)', []),
    (re.compile(r'ItemStack getCloneItemStack\(BlockState %s, HitResult %s, LevelReader %s, BlockPos %s, Player %s\) \{' % (W, W, W, W, W)),
     r'ItemStack getCloneItemStack(LevelReader \3, BlockPos \4, BlockState \1, boolean includeData, Player \5) {', []),
    (re.compile(r'void neighborChanged\(BlockState %s, Level %s, BlockPos %s, Block %s, BlockPos %s, boolean %s\) \{' % (W, W, W, W, W, W)),
     r'void neighborChanged(BlockState \1, Level \2, BlockPos \3, Block \4, @Nullable Orientation orientation, boolean \6) {',
     ['net.minecraft.world.level.redstone.Orientation', 'javax.annotation.Nullable']),
    (re.compile(r'void entityInside\(BlockState %s, Level %s, BlockPos %s, Entity %s\) \{' % (W, W, W, W)),
     r'void entityInside(BlockState \1, Level \2, BlockPos \3, Entity \4, InsideBlockEffectApplier effectApplier, boolean isPrecise) {',
     ['net.minecraft.world.entity.InsideBlockEffectApplier']),
    (re.compile(r'super\.entityInside\(%s, %s, %s, %s\)' % (W, W, W, W)),
     r'super.entityInside(\1, \2, \3, \4, effectApplier, isPrecise)', []),
    (re.compile(r'boolean isValidBonemealTarget\(LevelReader %s, BlockPos %s, BlockState %s\) \{' % (W, W, W)),
     r'boolean isValidBonemealTarget(LevelReader \1, BlockPos \2, BlockState \3, BonemealSource source) {',
     ['net.minecraft.world.level.block.BonemealSource']),
    (re.compile(r'boolean isBonemealSuccess\(Level %s, RandomSource %s, BlockPos %s, BlockState %s\) \{' % (W, W, W, W)),
     r'boolean isBonemealSuccess(Level \1, RandomSource \2, BlockPos \3, BlockState \4, BonemealSource source) {',
     ['net.minecraft.world.level.block.BonemealSource']),
    (re.compile(r'void performBonemeal\(ServerLevel %s, RandomSource %s, BlockPos %s, BlockState %s\) \{' % (W, W, W, W)),
     r'void performBonemeal(ServerLevel \1, RandomSource \2, BlockPos \3, BlockState \4, BonemealSource source) {',
     ['net.minecraft.world.level.block.BonemealSource']),
    (re.compile(r'void playerDestroy\(Level %s, Player %s, BlockPos %s, BlockState %s, @Nullable BlockEntity %s, ItemStack %s\) \{' % (W, W, W, W, W, W)),
     r'void playerDestroy(ServerLevel \1, ServerPlayer \2, BlockPos \3, BlockState \4, @Nullable BlockEntity \5, ItemStack \6) {',
     ['net.minecraft.server.level.ServerLevel', 'net.minecraft.server.level.ServerPlayer']),
    (re.compile(r'void fallOn\(Level %s, BlockState %s, BlockPos %s, Entity %s, float %s\) \{' % (W, W, W, W, W)),
     r'void fallOn(Level \1, BlockState \2, BlockPos \3, Entity \4, double \5) {', []),
]


def add_import(src, fq):
    line = 'import %s;' % fq
    if line in src:
        return src
    imports = list(re.finditer(r'^import [\w.]+;\r?$', src, re.M))
    target = next((m for m in imports if m.group(0).rstrip('\r') > line), None)
    eol = '\r\n' if '\r\n' in src else '\n'
    pos = target.start() if target else imports[-1].end() + len(eol)
    return src[:pos] + line + eol + src[pos:]


def main():
    files = hits = 0
    for root in sys.argv[1:]:
        for base, _, names in os.walk(root):
            for n in names:
                if not n.endswith('.java'):
                    continue
                path = os.path.join(base, n)
                src = open(path, encoding='utf-8', newline='').read()
                res = src
                for rx, repl, imports in RULES:
                    res, k = rx.subn(repl, res)
                    if k:
                        hits += k
                        for fq in imports:
                            res = add_import(res, fq)
                if res != src:
                    open(path, 'w', encoding='utf-8', newline='').write(res)
                    files += 1
    print('files changed %d, replacements %d' % (files, hits))


if __name__ == '__main__':
    main()
