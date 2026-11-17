"""Write the datapack files that replace Forestry's 1.21.1 fuel, compost, brewing and stripping code.

Usage: python -I gen_component_data.py <port root> <minecraft 26.3 jar>

26.1 moved these out of code:
- burn time and compost chance are item components that name a context_int_provider. The values Forestry
  needs that vanilla has no provider for are written here, in the same shape as the vanilla providers
  (a fast furnace halves the burn time, an empty composter always takes the item)
- brewing recipes are minecraft:brewing recipes
- axe stripping is the neoforge:transformables block data map, one entry per log and wood block

Every value repeats the 1.21.1 code: furnace_fuels.json and compostables.json of the generated data maps,
ModuleApiculture.registerBrewingRecipes and ForestryLogBlock.getToolModifiedState.
"""
import copy
import json
import os
import re
import sys
import zipfile

# burn times without a vanilla provider of the same value and meaning
FUELS = {
    'time_charcoal_block': 16000,
    'time_bituminous_peat': 4200,
    'time_peat': 2000,
    'time_log_pile': 1200,
    'time_plywood': 50,
}
# compost chances without a vanilla provider, in percent
COMPOSTABLES = {'peat': 75}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def vanilla(jar, path):
    with zipfile.ZipFile(jar) as z:
        return json.loads(z.read('data/minecraft/' + path))


def main():
    root, jar = sys.argv[1], sys.argv[2]
    data = os.path.join(root, 'src', 'main', 'resources', 'data')
    count = 0

    template = vanilla(jar, 'context_int_provider/cooking/time_coal_block.json')
    assert template['type'] == 'minecraft:div' and template['left'] == 16000, template
    for name, ticks in FUELS.items():
        provider = copy.deepcopy(template)
        provider['left'] = ticks
        write(os.path.join(data, 'forestry', 'context_int_provider', 'cooking', name + '.json'), provider)
        count += 1

    template = vanilla(jar, 'context_int_provider/compostable/low.json')
    weights = {entry['data']: entry for entry in template['default']['distribution']}
    assert weights[1]['weight'] == 30 and weights[0]['weight'] == 70, template
    for name, percent in COMPOSTABLES.items():
        provider = copy.deepcopy(template)
        for entry in provider['default']['distribution']:
            entry['weight'] = percent if entry['data'] == 1 else 100 - percent
        write(os.path.join(data, 'forestry', 'context_int_provider', 'compostable', name + '.json'), provider)
        count += 1

    # quirk kept: the input is any potion. Forge 1.12 and NeoForge 1.21.1 matched the input stack by item only,
    # so the awkward potion in the old code never narrowed it
    for potion, reagent in (('healing', 'pollen_cluster'), ('regeneration', 'crystalline_pollen_cluster')):
        write(os.path.join(data, 'forestry', 'recipe', 'brewing', 'potion_%s_from_%s.json' % (potion, reagent)), {
            'type': 'minecraft:brewing',
            'input': {'item': 'minecraft:potion'},
            'output': {'components': {'minecraft:potion_contents': {'potion': 'minecraft:' + potion}}, 'id': 'minecraft:potion'},
            'reagent': {'item': 'forestry:' + reagent},
        })
        count += 1

    axe = vanilla(jar, 'block_transformer/axe.json')[0]
    assert axe['sound'] == 'minecraft:item.axe.strip', axe
    blockstates = os.path.join(root, 'src', 'generated', 'resources', 'assets', 'forestry', 'blockstates')
    names = {n[:-5] for n in os.listdir(blockstates) if n.endswith('.json')}
    values = {}
    for stripped in sorted(names):
        m = re.fullmatch(r'(.+)_stripped_(log|wood)', stripped)
        if not m or m.group(1) + '_' + m.group(2) not in names:
            continue
        block = 'forestry:%s_%s' % (m.group(1), m.group(2))
        values[block] = {
            'transformer': 'minecraft:axe',
            'transform_data': {
                'block_state_provider': {
                    'type': 'minecraft:rule_based',
                    'rules': [{
                        'if_true': {'type': 'minecraft:matching_blocks', 'blocks': block},
                        'then': {'type': 'minecraft:copy_properties', 'source': {'id': 'forestry:' + stripped, 'properties': {'axis': 'y'}}},
                    }],
                },
                'item_damage_per_use': axe['item_damage_per_use'],
                'sound': axe['sound'],
            },
        }
    write(os.path.join(data, 'neoforge', 'data_maps', 'block', 'transformables.json'), {'values': values})
    print('files written: %d, stripping entries: %d' % (count + 1, len(values)))


if __name__ == '__main__':
    main()
