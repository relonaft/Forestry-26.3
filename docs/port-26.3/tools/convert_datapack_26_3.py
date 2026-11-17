"""Convert Forestry's 1.21.1 datapack files to the 26.3 formats, in place.

Usage: python -I convert_datapack_26_3.py <resources dir>...
Pass every resources root (src/generated/resources, src/generated/resources_farms, src/main/resources, ...).

Changes, each checked against the 26.3 codecs:
- recipes: an ingredient is an item ID, "#tag" or a list of item IDs (vanilla Ingredient.CODEC is a holder
  set). A NeoForge custom ingredient names its type under "neoforge:ingredient_type" instead of "type".
  An ingredient cannot be empty: the empty "box" of a carpenter recipe is left out. The input and reagent
  of minecraft:brewing are potion ingredients, objects with "item". Inside a components ingredient
  minecraft:enchantments is the level map itself and minecraft:attribute_modifiers the modifier list
- IDs renamed since 1.21.1: minecraft:chain is minecraft:iron_chain, the tag c:slimeballs is c:slime_balls
  (recipes, advancements, loot tables, tags)
- loot tables and global loot modifiers: one "condition" instead of the "conditions" list (several become
  minecraft:all_of), one "modifier" instead of the "functions" list (several stay a list, the inline sequence),
  conditions and functions name their type under "type", minecraft:block_state_property is
  minecraft:match_block with "blocks" and "state"
- loot tables: a minecraft:tag entry names its tag as "items": "#tag" instead of "name"
- advancements: minecraft:recipe_unlocked takes "recipes", an entity condition such as "player" is one loot
  condition instead of a list
- worldgen: configured features are worldgen/feature with the config fields at the top level, a block
  state without properties is written as the block ID
- bee effects: a block state is the block ID, or "id" and "properties" instead of "Name" and "Properties"
- global loot modifiers: every file under loot_modifiers is a modifier, the list file
  data/neoforge/loot_modifiers/global_loot_modifiers.json is removed once every entry has its file
The script is idempotent and prints every value it could not convert.
"""
import glob
import json
import os
import sys

# lists whose elements are separate ingredients, not alternatives of one
SEPARATE = {'ingredients', 'resources', 'inputs'}
CUSTOM_INGREDIENTS = {'neoforge:components', 'neoforge:compound', 'neoforge:difference', 'neoforge:intersection', 'neoforge:block_tag'}
RENAMED = {'minecraft:chain': 'minecraft:iron_chain', 'c:slimeballs': 'c:slime_balls', '#c:slimeballs': '#c:slime_balls'}
problems = []


def rename(node):
    if isinstance(node, dict):
        return {k: rename(v) for k, v in node.items()}
    if isinstance(node, list):
        return [rename(v) for v in node]
    if isinstance(node, str):
        return RENAMED.get(node, node)
    return node


def components(patch):
    enchantments = patch.get('minecraft:enchantments')
    if isinstance(enchantments, dict) and set(enchantments) == {'levels'}:
        patch['minecraft:enchantments'] = enchantments['levels']
    modifiers = patch.get('minecraft:attribute_modifiers')
    if isinstance(modifiers, dict) and set(modifiers) == {'modifiers'}:
        patch['minecraft:attribute_modifiers'] = modifiers['modifiers']
    return patch


def ingredient(value, where):
    if isinstance(value, dict):
        if set(value) == {'item'} and isinstance(value['item'], str):
            return value['item']
        if set(value) == {'tag'} and isinstance(value['tag'], str):
            return '#' + value['tag']
        if value.get('type') in CUSTOM_INGREDIENTS:
            out = {'neoforge:ingredient_type': value['type']}
            out.update({k: recipe(v, where) for k, v in value.items() if k != 'type'})
            return out
        if value.get('neoforge:ingredient_type') == 'neoforge:components' and isinstance(value.get('components'), dict):
            components(value['components'])
    if isinstance(value, list) and value and all(isinstance(v, dict) and set(v) in ({'item'}, {'tag'}) for v in value):
        converted = [ingredient(v, where) for v in value]
        if len(converted) == 1:
            return converted[0]
        if any(v.startswith('#') for v in converted):
            # a vanilla ingredient lists items only, alternatives with a tag need the NeoForge compound ingredient
            return {'neoforge:ingredient_type': 'neoforge:compound', 'children': converted}
        return converted
    return None


def recipe(node, where):
    if isinstance(node, dict) and node.get('type') == 'minecraft:brewing':
        for key in ('input', 'reagent'):
            if isinstance(node.get(key), str):
                node[key] = {'item': node[key]}
        return node
    if isinstance(node, dict) and node.get('box') == []:
        node = {k: v for k, v in node.items() if k != 'box'}
    converted = ingredient(node, where)
    if converted is not None:
        return converted
    if isinstance(node, dict):
        return {k: [recipe(e, where) for e in v] if k in SEPARATE and isinstance(v, list) else recipe(v, where) for k, v in node.items()}
    if isinstance(node, list):
        return [recipe(v, where) for v in node]
    return node


def loot(node, where):
    if isinstance(node, list):
        return [loot(v, where) for v in node]
    if not isinstance(node, dict):
        return node
    node = {k: loot(v, where) for k, v in node.items()}
    out = {}
    for key in ('condition', 'function'):
        if isinstance(node.get(key), str):
            out['type'] = node.pop(key)
    out.update(node)
    if 'conditions' in out:
        conditions = out.pop('conditions')
        out['condition'] = conditions[0] if len(conditions) == 1 else {'type': 'minecraft:all_of', 'terms': conditions}
    if 'functions' in out:
        functions = out.pop('functions')
        out['modifier'] = functions[0] if len(functions) == 1 else functions
    if out.get('type') == 'minecraft:block_state_property':
        out.pop('type')
        block = out.pop('block', None)
        state = out.pop('properties', None)
        out = {'type': 'minecraft:match_block', **({'blocks': block} if block else {}), **({'state': state} if state is not None else {}), **out}
    if out.get('type') == 'minecraft:tag' and isinstance(out.get('name'), str) and 'items' not in out:
        out = {k if k != 'name' else 'items': '#' + v if k == 'name' else v for k, v in out.items()}
    if isinstance(out.get('rolls'), float) and out['rolls'].is_integer():
        out['rolls'] = int(out['rolls'])
    return out


def advancement(node, where):
    for criterion in node.get('criteria', {}).values():
        conditions = criterion.get('conditions', {})
        if criterion.get('trigger') == 'minecraft:recipe_unlocked' and 'recipe' in conditions:
            conditions['recipes'] = conditions.pop('recipe')
        for key, value in conditions.items():
            if isinstance(value, list) and value and all(isinstance(v, dict) and 'condition' in v for v in value):
                terms = loot(value, where)
                conditions[key] = terms[0] if len(terms) == 1 else {'type': 'minecraft:all_of', 'terms': terms}
    return node


def states(node, where):
    if isinstance(node, list):
        return [states(v, where) for v in node]
    if not isinstance(node, dict):
        return node
    if 'Name' in node and set(node) <= {'Name', 'Properties'}:
        return {'id': node['Name'], 'properties': node['Properties']} if node.get('Properties') else node['Name']
    return {k: states(v, where) for k, v in node.items()}


def modifier_list(roots):
    for root in roots:
        path = os.path.join(root, 'data', 'neoforge', 'loot_modifiers', 'global_loot_modifiers.json')
        if not os.path.exists(path):
            continue
        with open(path, encoding='utf-8') as f:
            entries = json.load(f).get('entries', [])
        missing = [e for e in entries if not any(
            os.path.exists(os.path.join(r, 'data', e.split(':')[0], 'loot_modifiers', e.split(':')[1] + '.json')) for r in roots)]
        if missing:
            problems.append('%s lists modifiers without a file: %s' % (path, missing))
        else:
            os.remove(path)
            print('removed %s' % path)


FOLDERS = {'recipe': recipe, 'loot_table': loot, 'loot_modifiers': loot, 'advancement': advancement, 'tags': lambda node, where: node,
           'bee_effect': states}


def features(root):
    moved = 0
    for path in glob.glob(os.path.join(root, 'data', '*', 'worldgen', 'configured_feature', '**', '*.json'), recursive=True):
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
        feature = {'type': data['type'], **data.get('config', {})}
        for target in feature.get('targets', []):
            state = target.get('state')
            if isinstance(state, dict) and set(state) == {'Name'}:
                target['state'] = state['Name']
        new = path.replace(os.sep + 'configured_feature' + os.sep, os.sep + 'feature' + os.sep).replace('/configured_feature/', '/feature/')
        os.makedirs(os.path.dirname(new), exist_ok=True)
        with open(new, 'w', encoding='utf-8', newline=chr(10)) as f:
            json.dump(feature, f, indent=2)
            f.write(chr(10))
        os.remove(path)
        moved += 1
    return moved


def main():
    changed = total = 0
    print('configured features moved: %d' % sum(features(root) for root in sys.argv[1:]))
    modifier_list(sys.argv[1:])
    for root in sys.argv[1:]:
        for folder, convert in FOLDERS.items():
            for path in glob.glob(os.path.join(root, 'data', '*', folder, '**', '*.json'), recursive=True):
                if folder == 'loot_modifiers' and os.path.basename(path) == 'global_loot_modifiers.json':
                    continue
                with open(path, encoding='utf-8') as f:
                    data = json.load(f)
                new = rename(convert(json.loads(json.dumps(data)), path))
                total += 1
                if new != data:
                    with open(path, 'w', encoding='utf-8', newline='\n') as f:
                        json.dump(new, f, indent=2)
                        f.write('\n')
                    changed += 1
    print('files read: %d, changed: %d' % (total, changed))
    for p in problems:
        print('NOT CONVERTED', p)


if __name__ == '__main__':
    main()
