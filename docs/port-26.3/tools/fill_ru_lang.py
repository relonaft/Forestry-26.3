"""Fill the Russian translation of the port up to the English key set.

Usage: python -I fill_ru_lang.py <port root>

Reuses Community Edition's Russian strings stored under keys that were renamed, builds wood-type
names from CE's own templates, and adds hand translations for the rest. Existing Russian strings
are never changed; missing keys are appended at the end of ru_ru.json.
"""
import json
import os
import re
import sys

root = sys.argv[1]
LANG = os.path.join(root, 'src/main/resources/assets/forestry/lang')
GEN = os.path.join(root, 'src/generated/resources/assets/forestry/lang')


def load(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


en = {}
for p in (os.path.join(LANG, 'en_us.json'), os.path.join(GEN, 'en_us.json')):
    if os.path.exists(p):
        en.update(load(p))
ru_path = os.path.join(LANG, 'ru_ru.json')
ru = load(ru_path)
added = {}


def put(key, value):
    if key in en and key not in ru and key not in added:
        added[key] = value


# 1. renamed keys: CE's Russian text under the old key
def num_key(old):
    # 10i, 10id -> 10; 0.25f, 0.01fd -> 0_25, 0_01
    m = re.fullmatch(r'(\d+)(?:i|id)', old)
    if m:
        return m.group(1)
    m = re.fullmatch(r'(\d+)\.(\d+)f?d?', old)
    if m:
        return m.group(1) + '_' + m.group(2)
    return None


for key, value in ru.items():
    m = re.fullmatch(r'allele\.forestry\.([a-z_]+)\.([0-9.]+[a-z]*)', key)
    if m:
        new = num_key(m.group(2))
        # fertility 0 is "Infertile" now, not the bare number
        if new is not None and not (m.group(1) == 'fertility' and new == '0'):
            put('allele.forestry.%s.%s' % (m.group(1), new), value)
    m = re.fullmatch(r'allele\.forestry\.(temperature|humidity)_tolerance\.tolerance_([a-z]+)(?:_(\d)d?)?', key)
    if m:
        new = m.group(2) if m.group(3) is None else '%s_%s' % (m.group(2), m.group(3))
        put('allele.forestry.%s_tolerance.%s' % (m.group(1), new), value)

WOOD_RENAMES = {'acacia_desert': 'camelthorn', 'citrus': 'lemon', 'giganteum': 'giant_sequoia',
                'hill_cherry': 'sour_cherry', 'zebrawood': 'zebrano'}
for key, value in ru.items():
    for old, new in WOOD_RENAMES.items():
        for prefix in ('block.forestry.', 'item.forestry.'):
            if key.startswith(prefix + old + '_'):
                put(prefix + new + key[len(prefix + old):], value)

LEAF_RENAMES = {'desert_acacia': 'camelthorn', 'hill_cherry': 'sour_cherry', 'zebrawood': 'zebrano',
                'sipiri': 'greenheart'}
for key, value in ru.items():
    m = re.fullmatch(r'block\.forestry\.tree_([a-z_]+)_decorative_leaves', key)
    if m:
        put('block.forestry.%s_decorative_leaves' % LEAF_RENAMES.get(m.group(1), m.group(1)), value)
    m = re.fullmatch(r'item\.forestry\.crated_bee_comb_([a-z]+)', key)
    if m:
        name = 'spongy' if m.group(1) == 'sponge' else m.group(1)
        put('item.forestry.crated_%s_comb' % name, value)
    m = re.fullmatch(r'item\.forestry\.([a-z]+)_bag(_woven)?', key)
    if m:
        put('item.forestry.%s%s_backpack' % ('woven_' if m.group(2) else '', m.group(1)), value)

RENAMED = {
    'block.forestry.fluid_bio_ethanol': 'block.forestry.fluid_ethanol',
    'block.forestry.fluid_glass': 'block.forestry.fluid_liquid_glass',
    'block.forestry.fluid_ice': 'block.forestry.fluid_crushed_ice',
    'block.forestry.fluid_juice': 'block.forestry.fluid_fruit_juice',
    'item.forestry.bucket_bio_ethanol': 'item.forestry.bucket_ethanol',
    'item.forestry.bucket_glass': 'item.forestry.bucket_liquid_glass',
    'item.forestry.bucket_ice': 'item.forestry.bucket_crushed_ice',
    'item.forestry.bucket_juice': 'item.forestry.bucket_fruit_juice',
    'for.config.beekeeping.hivedamage.onlyOnAttack': 'for.config.beekeeping.hivedamage.onlyAfterAttack',
    'for.config.beekeeping.hivedamage.onlyOnAttack.comment': 'for.config.beekeeping.hivedamage.onlyAfterAttack.comment',
    'for.jei.charcoal.pile.name': 'for.jei.charcoal.pile',
    'for.description.treeOak': 'allele.forestry.tree_species.oak.desc',
    'for.description.treeCherry': 'allele.forestry.tree_species.sour_cherry.desc',
}
for old, new in RENAMED.items():
    if old in ru:
        put(new, ru[old])

# 2. wood types, by CE's own templates (genitive after "из")
WOOD_GEN = {
    'beech': 'пурпурного бука', 'camelthorn': 'пустынной акации', 'coconut': 'кокосовой пальмы',
    'dogwood': 'цветущего кизила', 'elm': 'золотого вяза', 'feijoa': 'фейхоа', 'fir': 'бальзамической пихты',
    'giant_sequoia': 'секвойи гигантской', 'ginkgo': 'гинкго', 'jacaranda': 'джакаранды', 'kauri': 'каури',
    'lemon': 'лимонного дерева', 'macrocarpa': 'макрокарпы', 'olive': 'оливы', 'orange': 'апельсинового дерева',
    'pear': 'груши', 'pewen': 'араукарии', 'sour_cherry': 'вишнёвого дерева', 'zebrano': 'зебрано',
}
for wood, gen in WOOD_GEN.items():
    for ff, tag in (('', ''), ('fireproof_', ' (огнестойкость)')):
        put('block.forestry.%s_%slog' % (wood, ff), 'Бревно %s%s' % (gen, tag))
        put('block.forestry.%s_%swood' % (wood, ff), 'Древесина %s%s' % (gen, tag))
        put('block.forestry.%s_%sstripped_log' % (wood, ff), 'Обтёсанное бревно %s%s' % (gen, tag))
        put('block.forestry.%s_%sstripped_wood' % (wood, ff), 'Обтёсанная древесина %s%s' % (gen, tag))
        for part, word in (('planks', 'Доски'), ('slab', 'Плита'), ('stairs', 'Ступеньки'), ('fence', 'Забор'),
                           ('fence_gate', 'Калитка')):
            put('block.forestry.%s_%s%s' % (wood, ff, part), '%s из %s%s' % (word, gen, tag))
    for part, word in (('button', 'Кнопка'), ('door', 'Дверь'), ('trapdoor', 'Люк'), ('pressure_plate', 'Нажимная плита'),
                       ('sign', 'Табличка'), ('hanging_sign', 'Подвесная табличка')):
        put('block.forestry.%s_%s' % (wood, part), '%s из %s' % (word, gen))
    put('block.forestry.%s_decorative_leaves' % wood, 'Декоративные листья %s' % gen)
    put('item.forestry.%s_boat' % wood, 'Лодка из %s' % gen)
    put('item.forestry.%s_chest_boat' % wood, 'Грузовая лодка из %s' % gen)
    put('for.trees.woodType.%s' % wood, gen)

# 3. hand translations
COLORS = {'black': 'чёрная', 'blue': 'синяя', 'brown': 'коричневая', 'cyan': 'бирюзовая', 'gray': 'серая',
          'green': 'зелёная', 'light_blue': 'голубая', 'light_gray': 'светло-серая', 'lime': 'лаймовая',
          'magenta': 'пурпурная', 'orange': 'оранжевая', 'pink': 'розовая', 'purple': 'фиолетовая', 'red': 'красная',
          'white': 'белая', 'yellow': 'жёлтая', 'rainbow': 'радужная', 'refractory': 'огнеупорная'}
for color, adj in COLORS.items():
    put('block.forestry.big_candle_' + color, 'Большая %s свеча' % adj)
    put('block.forestry.jumbo_candle_' + color, 'Огромная %s свеча' % adj)
    if color not in ('rainbow', 'refractory'):
        put('block.forestry.metal_plating_' + color, '%s лакированная металлическая обшивка' % adj.capitalize())
put('block.forestry.big_candle_normal', 'Большая свеча')
put('block.forestry.jumbo_candle_normal', 'Огромная свеча')
put('block.forestry.rainbow_candle', 'Радужная свеча')
put('block.forestry.refractory_candle', 'Огнеупорная свеча')
for metal, adj in (('bronze', 'Бронзовая'), ('copper', 'Медная'), ('gold', 'Золотая'), ('iron', 'Железная'),
                   ('netherite', 'Незеритовая'), ('tin', 'Оловянная')):
    put('block.forestry.metal_plating_' + metal, '%s металлическая обшивка' % adj)

# stone-like families: (key prefix, nominative, genitive)
STONES = [
    ('waxstone', 'Восковой камень', 'воскового камня'),
    ('cobbled_waxstone', 'Колотый восковой камень', 'колотого воскового камня'),
    ('polished_waxstone', 'Полированный восковой камень', 'полированного воскового камня'),
    ('refractory_waxstone', 'Огнеупорный восковой камень', 'огнеупорного воскового камня'),
    ('cobbled_refractory_waxstone', 'Колотый огнеупорный восковой камень', 'колотого огнеупорного воскового камня'),
    ('polished_refractory_waxstone', 'Полированный огнеупорный восковой камень', 'полированного огнеупорного воскового камня'),
    ('honeystone', 'Медовый камень', 'медового камня'),
    ('cobbled_honeystone', 'Колотый медовый камень', 'колотого медового камня'),
    ('polished_honeystone', 'Полированный медовый камень', 'полированного медового камня'),
]
for prefix, nom, gen in STONES:
    put('block.forestry.' + prefix, nom)
    for part, word in (('slab', 'Плита'), ('stairs', 'Ступеньки'), ('wall', 'Стена')):
        put('block.forestry.%s_%s' % (prefix, part), '%s из %s' % (word, gen))
BRICKS = [
    ('waxstone_brick', 'Кирпичи из воскового камня', 'кирпичей воскового камня'),
    ('refractory_waxstone_brick', 'Кирпичи из огнеупорного воскового камня', 'кирпичей огнеупорного воскового камня'),
    ('honeystone_brick', 'Кирпичи из медового камня', 'кирпичей медового камня'),
    ('wax_brick', 'Восковые кирпичи', 'восковых кирпичей'),
    ('refractory_wax_brick', 'Огнеупорные восковые кирпичи', 'огнеупорных восковых кирпичей'),
    ('ash_brick', 'Зольные кирпичи', 'зольных кирпичей'),
]
for prefix, nom, gen in BRICKS:
    put('block.forestry.%ss' % prefix, nom)
    for part, word in (('slab', 'Плита'), ('stairs', 'Ступеньки'), ('wall', 'Стена')):
        put('block.forestry.%s_%s' % (prefix, part), '%s из %s' % (word, gen))

manual = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'ru_lang_manual.json')
if os.path.exists(manual):
    with open(manual, encoding='utf-8') as f:
        for key, value in json.load(f).items():
            put(key, value)

# write: appended as text before the closing brace, since the file repeats "_comment" keys as section
# headers and a JSON round trip would drop them
missing_after = [k for k in en if k not in ru and k not in added]
with open(ru_path, encoding='utf-8', newline='') as f:
    text = f.read()
body = text.rstrip()
assert body.endswith('}')
body = body[:-1].rstrip()
HEADER = '  "_comment": "Port to 26.3: keys missing from Community Edition"'
lines = [] if HEADER in text else [HEADER]
lines += ['  %s: %s' % (json.dumps(k, ensure_ascii=False), json.dumps(added[k], ensure_ascii=False)) for k in en if k in added]
if added:
    body += ',\n' + ',\n'.join(lines)
if '--dry' not in sys.argv:
    with open(ru_path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(body + '\n}\n')
print('added', len(added), 'still missing', len(missing_after))
for key in missing_after:
    print('  ', key, '=', en[key])
