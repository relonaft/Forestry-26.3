"""Write the villager trades of the beekeeper and the arborist as 26.3 datapack files.

Usage: python -I gen_villager_trades.py <port root>

Until 26.1 these were ItemListing records in ApicultureVillagers, ArboricultureVillagers and
VillagerTrade. Every trade below repeats one listing of the 1.21.1 code: same items, counts, prices,
uses, xp and price multiplier. A villager still offers two trades per level.

Where a listing picked one of several cost items at random (the apiary trade takes a random log),
there is one trade per item with a forestry:trader_choice predicate, so exactly one of them is
valid for a given villager and the chance of the listing is unchanged.
"""
import json
import os
import sys

BEE = 'forestry:bee_species'
TREE = 'forestry:tree_species'
MUNDANE = ['forest', 'meadows', 'modest', 'wintry', 'tropical', 'marshy']
LOGS = ['acacia_log', 'birch_log', 'dark_oak_log', 'jungle_log', 'oak_log', 'spruce_log', 'mangrove_log']


def amount(lo, hi):
    return lo if lo == hi else {'type': 'minecraft:uniform', 'min': lo, 'max': hi}


def cost(item, lo, hi):
    stack = {'id': item}
    if (lo, hi) != (1, 1):
        stack['count'] = amount(lo, hi)
    return stack


def set_count(lo, hi):
    return {'type': 'minecraft:set_count', 'count': amount(lo, hi)}


def trade(wants, gives, uses, xp, mult, extra=None, modifiers=(), predicate=None):
    out = {'wants': wants}
    if extra:
        out['additional_wants'] = extra
    out['gives'] = {'id': gives}
    if modifiers:
        out['given_item_modifier'] = list(modifiers)
    out['max_uses'] = uses
    out['reputation_discount'] = mult
    out['xp'] = xp
    if predicate:
        out['merchant_predicate'] = predicate
    return out


def sells(item, sell, emeralds, uses, xp, mult=0.05, modifiers=()):
    mods = list(modifiers)
    if sell != (1, 1):
        mods.append(set_count(*sell))
    return trade(cost('minecraft:emerald', *emeralds), item, uses, xp, mult, modifiers=mods)


def species(type_id, name):
    return {'type': 'forestry:set_species_nbt', 'type_id': type_id, 'species_id': 'forestry:' + name}


def beekeeper():
    t = {}
    for crop in ('wheat', 'carrot', 'potato'):
        t['1/%s_comb' % crop] = trade(cost('minecraft:' + crop, 8, 12), 'forestry:honey_comb', 8, 2, 0.0, modifiers=[
            {'type': 'forestry:random_item', 'items': '#forestry:village_combs'}, set_count(2, 4)])
    t['2/emerald_smoker'] = sells('forestry:bee_smoker', (1, 1), (1, 4), 8, 6)
    t['2/emerald_proven_scoop'] = sells('forestry:proven_scoop', (1, 1), (1, 4), 8, 6)
    t['2/propolis_drone'] = trade(cost('forestry:propolis', 2, 4), 'forestry:drone_bee', 8, 6, 0.0, modifiers=[
        {'type': 'forestry:random_organism', 'type_id': BEE, 'species': ['forestry:' + s for s in MUNDANE]}])
    t['3/princess_emerald'] = trade(cost('forestry:princess_bee', 1, 1), 'minecraft:emerald', 8, 10, 0.05)
    t['3/emerald_proven_frame'] = sells('forestry:proven_frame', (1, 2), (1, 6), 8, 10)
    for i, log in enumerate(LOGS):
        t['3/%s_apiary' % log] = trade(
            cost('minecraft:' + log, 32, 64), 'forestry:apiary', 8, 10, 0.05, extra=cost('minecraft:emerald', 16, 32),
            predicate={'type': 'forestry:trader_choice', 'choice': i, 'choices': len(LOGS)})
    t['4/princess_monastic_drone'] = trade(cost('forestry:princess_bee', 1, 1), 'forestry:drone_bee', 8, 15, 0.05,
                                           extra=cost('minecraft:emerald', 10, 64), modifiers=[species(BEE, 'monastic')])
    t['4/drone_ended_drone'] = trade(cost('forestry:drone_bee', 1, 1), 'forestry:drone_bee', 8, 15, 0.05,
                                     extra=cost('minecraft:ender_eye', 12, 16), modifiers=[species(BEE, 'ended')])
    return t


def arborist():
    def wood(kind):
        return {'type': 'forestry:random_wood', 'kind': kind}

    def tree(max_complexity):
        return {'type': 'forestry:random_organism', 'type_id': TREE, 'max_complexity': max_complexity}

    t = {}
    t['1/emerald_planks'] = sells('forestry:larch_planks', (10, 32), (1, 4), 8, 2, 0.0, [wood('planks')])
    t['1/emerald_sapling'] = sells('forestry:tree_sapling', (1, 3), (1, 1), 8, 2, 0.0, [tree(4)])
    t['2/emerald_planks'] = sells('forestry:larch_planks', (10, 32), (1, 4), 8, 6, 0.0, [wood('planks')])
    t['2/emerald_pollen'] = sells('forestry:tree_pollen', (1, 1), (2, 3), 8, 6, 0.0, [tree(6)])
    t['2/emerald_proven_grafter'] = sells('forestry:proven_grafter', (1, 1), (1, 4), 8, 6)
    # the 1.21.1 list held this listing twice
    t['3/emerald_logs_1'] = sells('forestry:larch_log', (6, 18), (2, 5), 8, 2, 0.0, [wood('log')])
    t['3/emerald_logs_2'] = sells('forestry:larch_log', (6, 18), (2, 5), 8, 2, 0.0, [wood('log')])
    t['4/emerald_pollen'] = sells('forestry:tree_pollen', (1, 1), (5, 20), 8, 15, 0.0, [tree(10)])
    t['4/emerald_sapling'] = sells('forestry:tree_sapling', (1, 1), (5, 20), 8, 15, 0.0, [tree(10)])
    return t


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    data = os.path.join(sys.argv[1], 'src', 'main', 'resources', 'data', 'forestry')
    count = 0
    for profession, trades in (('beekeeper', beekeeper()), ('arborist', arborist())):
        levels = {}
        for name, body in trades.items():
            write(os.path.join(data, 'villager_trade', profession, name + '.json'), body)
            levels.setdefault(name.split('/')[0], []).append('forestry:%s/%s' % (profession, name))
            count += 1
        for level, ids in sorted(levels.items()):
            write(os.path.join(data, 'trade_set', profession, 'level_%s.json' % level), {
                'amount': 2,
                'random_sequence': 'forestry:trade_set/%s/level_%s' % (profession, level),
                'trades': ids,
            })
    print('trades written: %d' % count)


if __name__ == '__main__':
    main()
