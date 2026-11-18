"""Write the 26.3 item model definitions (assets/<ns>/items/<id>.json) of every Forestry item.

Usage: python -I gen_item_models.py <port root> <minecraft 26.3 jar> <neoforge 26.3 universal jar>

Since 1.21.4 an item finds its model through assets/<ns>/items/<id>.json. Without one it renders as the
missing model. Item colours, item property overrides and custom model loaders moved into that file:

- a plain item is {"type": "minecraft:model"} on its existing item model
- an item that 1.21.1 coloured with ClientManager.FORESTRY_ITEM_COLOR lists one forestry:item_tint per
  tint index its model uses, so ITintedItem#getColorFromItemStack still decides every layer
- turf takes minecraft:grass, the constant 1.21.1 passed (GrassColor.getDefaultColor())
- a crate is a minecraft:composite of the crate and a contents model. The contents model repeats the
  geometry of the 1.21.1 FilledCrateModel loader and replaces the loader file of the same name
- bees, the GE sapling, the butterfly and the Forestry fluid containers use the item model types the
  Java code registers; the buckets use neoforge:fluid_container
- backpacks and the cocoon replace their "overrides" with minecraft:range_dispatch
- the block entity items use minecraft:special with the texture their block entity renderer gets

Running it again regenerates the same files. Model files whose loader no longer exists are deleted.
"""
import json
import os
import re
import sys
import zipfile

ROOTS = ['main/resources', 'generated/resources', 'generated/resources_butterflies', 'generated/resources_farms',
	'generated/resources_mail', 'farms/resources', 'mail/resources', 'butterflies/resources']

# ClientManager.FORESTRY_ITEM_COLOR registrations of the 1.21.1 client handlers, as item id patterns
COMBS = ['honey', 'cocoa', 'simmering', 'stringy', 'frozen', 'dripping', 'silky', 'parched', 'mysterious',
	'powdery', 'wheaten', 'mossy', 'mellow', 'kaolin', 'vintage', 'spongy', 'sculken']
TINTED = set(
	[c + '_comb_block' for c in COMBS] + [c + '_comb' for c in COMBS] + ['crated_' + c + '_comb' for c in COMBS]
	+ ['queen_bee', 'drone_bee', 'princess_bee', 'larvae_bee']
	+ ['propolis', 'pulsating_propolis', 'silky_propolis', 'pollen_cluster', 'crystalline_pollen_cluster']
	+ ['crated_pollen_cluster_normal', 'crated_pollen_cluster_crystalline', 'crated_propolis', 'crated_grass_block']
	+ ['leaves', 'tree_sapling', 'tree_pollen', 'can', 'wax_capsule', 'refractory_capsule', 'pipette', 'honey_drop']
	+ ['caterpillar', 'butterfly_serum'])
TINTED_PATTERNS = [re.compile(p) for p in [r'.+_default_leaves$', r'.+_default_leaves_fruit$', r'.+_decorative_leaves$',
	r'^stamp_.+$', r'.+_backpack$']]
GRASS = {'turf', 'turf_block'}

# texture strings exactly as CoreClientHandler.clientSetupRenderers passes them
SPECIAL = {
	'apiarists_chest': ('forestry:naturalist_chest', 'apiaristchest'),
	'arborists_chest': ('forestry:naturalist_chest', 'arbchest'),
	'lepidopterists_chest': ('forestry:naturalist_chest', 'lepichest'),
	'clockwork_engine': ('forestry:engine', 'textures/block/engine_clock_'),
	'biogas_engine': ('forestry:engine', 'textures/block/engine_bronze_'),
	'peat_engine': ('forestry:engine', 'textures/block/engine_copper_'),
	'combustion_engine': ('forestry:engine', 'textures/block/engine_iron_'),
	'solar_engine': ('forestry:engine', 'textures/block/engine_tin_'),
	'rainmaker': ('forestry:rainmaker', 'textures/block/rainmaker_'),
}
BEES = {'queen_bee': 'queen', 'drone_bee': 'drone', 'princess_bee': 'princess', 'larvae_bee': 'larvae'}
FLUID_CONTAINERS = ['can', 'wax_capsule', 'refractory_capsule']
# models that name a loader which 26.3 no longer has, or that only existed for such a model
REMOVED_LOADERS = {'forestry:bee_ge', 'forestry:butterfly_ge', 'forestry:sapling_ge', 'forestry:fluid_container',
	'forestry:filled_crate', 'neoforge:fluid_container'}
BACKPACK_MODES = ['neutral', 'locked', 'receive', 'resupply']
# item models that no registered item uses: helpers the item model types bake, and leftovers of older versions
NOT_ITEMS = {'butterfly_base', 'filled_crate', 'forester', 'glass_filled', 'jar_filled', 'planks', 'stairs',
	'stairs_fireproof', 'resource_ore_apatite', 'resource_ore_tin', 'habitat_former', 'database'}
NOT_ITEMS_PATTERNS = [re.compile(p) for p in [r'^bee_(queen|drone|princess|larvae)_.+$', r'^(?!tree_).+_sapling$']]


def norm(identifier):
	return identifier if ':' in identifier else 'minecraft:' + identifier


def read_json(path):
	with open(path, encoding='utf-8') as f:
		return json.load(f)


def write_json(path, data):
	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, 'w', encoding='utf-8', newline='\n') as f:
		f.write(json.dumps(data, indent=2) + '\n')


class Models:
	def __init__(self, src, jars):
		self.src = src
		self.files = {}  # model id -> (path or None, json)
		for root in ROOTS:
			base = os.path.join(src, root, 'assets')
			if not os.path.isdir(base):
				continue
			for ns in os.listdir(base):
				mdir = os.path.join(base, ns, 'models')
				for dirpath, _, names in os.walk(mdir):
					for name in names:
						if name.endswith('.json'):
							path = os.path.join(dirpath, name)
							rel = os.path.relpath(path, mdir).replace(os.sep, '/')[:-5]
							self.files[ns + ':' + rel] = (path, read_json(path))
		for jar in jars:
			with zipfile.ZipFile(jar) as z:
				for n in z.namelist():
					m = re.match(r'assets/([^/]+)/models/(.+)\.json$', n)
					if m and m.group(1) + ':' + m.group(2) not in self.files:
						self.files[m.group(1) + ':' + m.group(2)] = (None, json.loads(z.read(n)))

	def get(self, model_id):
		entry = self.files.get(model_id)
		return entry[1] if entry else None

	def chain(self, model_id):
		out = []
		while model_id and model_id not in [m for m, _ in out]:
			model = self.get(model_id)
			out.append((model_id, model))
			if model is None:
				break
			parent = model.get('parent')
			model_id = norm(parent) if parent else None
		return out

	def tint_count(self, model_id):
		# one tint entry per tint index: layer N of a generated model has tint index N
		tex = {}
		elements = None
		generated = False
		for mid, model in self.chain(model_id):
			if model is None:
				generated = generated or mid == 'minecraft:builtin/generated'
				continue
			for key in model.get('textures', {}):
				tex.setdefault(key, True)
			if elements is None and 'elements' in model:
				elements = model['elements']
		if elements is not None:
			indices = [f.get('tintindex', -1) for e in elements for f in e.get('faces', {}).values()]
			return max(indices + [-1]) + 1
		if generated:
			layers = [int(k[5:]) for k in tex if re.fullmatch(r'layer\d', k)]
			return max(layers + [-1]) + 1
		return 0


def is_tinted(item_id):
	return item_id in TINTED or any(p.match(item_id) for p in TINTED_PATTERNS)


def plain(model_id, tints):
	model = {'type': 'minecraft:model', 'model': model_id}
	if tints:
		model['tints'] = tints
	return model


def item_tints(count):
	return [{'type': 'forestry:item_tint', 'index': i} for i in range(count)]


def crate_contents(layers):
	# FilledCrateModel.make2dElement: front and back faces only, scaled down to 3,4 - 11,12
	elements = []
	for layer in layers:
		faces = {
			'south': {'uv': [0, 0, 16, 16], 'texture': '#layer%d' % layer, 'tintindex': layer},
			'north': {'uv': [16, 0, 0, 16], 'texture': '#layer%d' % layer, 'tintindex': layer},
		}
		elements.append({'from': [3, 4, 7.498], 'to': [11, 12, 8.502], 'faces': faces})
	return elements


def main():
	root, mc_jar, nf_jar = sys.argv[1:4]
	src = os.path.join(root, 'src')
	models = Models(src, [mc_jar, nf_jar])
	counts = {}
	deleted = []

	def count(kind):
		counts[kind] = counts.get(kind, 0) + 1

	def items_path(model_root, item_id):
		return os.path.join(model_root, 'forestry', 'items', item_id + '.json')

	def root_of(path):
		return path[:path.index(os.sep + 'forestry' + os.sep + 'models' + os.sep)]

	def delete(model_id):
		path = models.files[model_id][0]
		if path and os.path.exists(path):
			os.remove(path)
			deleted.append(model_id)

	# the crate geometry takes the transforms of the crate model, which come from item/generated
	crate_display = None
	for mid, model in models.chain('forestry:item/filled_crate'):
		if model and 'display' in model and crate_display is None:
			crate_display = model['display']

	# item models other item models build on, which are not items themselves
	referenced = set()
	for mid, (path, model) in models.files.items():
		if path is None or not mid.startswith('forestry:item/'):
			continue
		if 'parent' in model:
			referenced.add(norm(model['parent']))
		for override in model.get('overrides', []):
			referenced.add(norm(override['model']))
	lang = set()
	for r in ROOTS:
		p = os.path.join(src, r, 'assets', 'forestry', 'lang', 'en_us.json')
		if os.path.exists(p):
			for key in read_json(p):
				parts = key.split('.')
				if len(parts) == 3 and parts[0] in ('item', 'block') and parts[1] == 'forestry':
					lang.add(parts[2])

	skipped = []
	for mid, (path, model) in sorted(models.files.items()):
		if path is None or not mid.startswith('forestry:item/') or '/' in mid[len('forestry:item/'):]:
			continue
		item_id = mid[len('forestry:item/'):]
		out_root = root_of(path)
		loader = model.get('loader')

		if item_id in SPECIAL:
			kind, texture = SPECIAL[item_id]
			if model.get('parent') == 'builtin/entity':
				del model['parent']
				write_json(path, model)
			write_json(items_path(out_root, item_id), {'model': {'type': 'minecraft:special', 'base': mid, 'model': {'type': kind, 'texture': texture}}})
			count('special')
			continue
		if model.get('parent') == 'builtin/entity':
			# helpers of the special items that no item uses
			del model['parent']
			write_json(path, model)
		if item_id in BEES:
			# the bee models are minecraft:model each, so they take the tints of the default bee model
			tints = models.tint_count('forestry:item/bee_%s_default' % BEES[item_id])
			write_json(items_path(out_root, item_id), {'model': {'type': 'forestry:bee_ge', 'stage': BEES[item_id], 'tints': item_tints(tints)}})
			delete(mid)
			count('bee')
			continue
		if item_id == 'tree_sapling':
			tints = max(models.tint_count(m) for m in models.files if re.fullmatch(r'forestry:item/[a-z_]+_sapling', m) and m != mid)
			write_json(items_path(out_root, item_id), {'model': {'type': 'forestry:sapling_ge', 'tints': item_tints(tints)}})
			delete(mid)
			if 'forestry:block/sapling_ge' in models.files:
				delete('forestry:block/sapling_ge')
			# the block takes its model from the block state model type, not from a model file
			blockstate = os.path.join(src, 'generated', 'resources', 'assets', 'forestry', 'blockstates', 'sapling_ge.json')
			write_json(blockstate, {'variants': {'': {'type': 'forestry:sapling_ge'}}})
			count('sapling')
			continue
		if item_id == 'butterfly':
			write_json(items_path(out_root, item_id), {'model': {'type': 'forestry:butterfly_ge'}})
			delete(mid)
			count('butterfly')
			continue
		if item_id in FLUID_CONTAINERS:
			textures = {k: v for k, v in model['textures'].items() if k in ('particle', 'base', 'fluid', 'cover')}
			definition = {'type': 'forestry:fluid_container', 'textures': textures}
			for key in ('cover_is_mask', 'apply_fluid_luminosity'):
				if key in model:
					definition[key] = model[key]
			write_json(items_path(out_root, item_id), {'model': definition})
			delete(mid)
			count('fluid container')
			continue
		if loader == 'neoforge:fluid_container':
			bucket = models.get(norm(model['parent']))
			definition = {'type': 'neoforge:fluid_container', 'textures': {k: norm(v) for k, v in bucket['textures'].items()}, 'fluid': model['fluid']}
			for key in ('flip_gas', 'cover_is_mask', 'apply_fluid_luminosity', 'force_opaque_fluid'):
				if key in model:
					definition[key] = model[key]
			write_json(items_path(out_root, item_id), {'model': definition})
			delete(mid)
			count('bucket')
			continue
		if loader == 'forestry:filled_crate' or (item_id.startswith('crated_') and model.get('elements') and 'parent' not in model):
			textures = {k: v for k, v in model['textures'].items() if re.fullmatch(r'layer[1-4]', k)}
			layers = sorted(int(k[5:]) for k in textures)
			# particle silences the missing texture warning of 26.3, the lowest layer is the crated item itself
			contents = {'gui_light': 'front', 'textures': {'particle': textures['layer%d' % layers[0]], **textures}, 'elements': crate_contents(layers), 'display': crate_display}
			write_json(path, contents)
			tints = item_tints(max(layers) + 1) if is_tinted(item_id) else []
			write_json(items_path(out_root, item_id), {'model': {'type': 'minecraft:composite', 'models': [
				plain('forestry:item/filled_crate', []), plain(mid, tints)]}})
			count('crate')
			continue
		if loader in REMOVED_LOADERS:
			delete(mid)
			skipped.append((item_id, 'stale loader model ' + loader))
			continue
		overrides = model.get('overrides')
		if overrides is None and item_id.endswith('_backpack'):
			# an earlier run already removed the overrides, they follow the fallback model
			family = norm(model['parent'])[:-len('_neutral')]
			overrides = [{'predicate': {'minecraft:mode': float(i)}, 'model': '%s_%s' % (family, mode)} for i, mode in enumerate(BACKPACK_MODES) if i]
		elif overrides is None and item_id == 'cocoon':
			overrides = [{'predicate': {'forestry:age': i}, 'model': 'forestry:item/cocoon_' + age} for i, age in ((1, 'middle'), (2, 'late'))]
		if overrides is not None:
			if 'overrides' in model:
				del model['overrides']
				write_json(path, model)
			fallback = norm(model['parent'])
			prop, values = next(iter(overrides[0]['predicate'].items()))
			if prop == 'minecraft:mode':
				prop = 'forestry:backpack_mode'
				count('backpack')
			elif prop == 'forestry:age':
				prop = 'forestry:cocoon_age'
				count('cocoon')
			else:
				raise SystemExit('unknown override property %s in %s' % (prop, mid))

			def sub(model_id):
				return plain(model_id, item_tints(models.tint_count(model_id)) if is_tinted(item_id) else [])
			entries = [{'threshold': float(o['predicate'][next(iter(o['predicate']))]), 'model': sub(norm(o['model']))} for o in overrides]
			write_json(items_path(out_root, item_id), {'model': {'type': 'minecraft:range_dispatch', 'property': prop,
				'entries': entries, 'fallback': sub(fallback)}})
			continue
		if (mid in referenced and item_id not in lang) or item_id in NOT_ITEMS or any(p.match(item_id) for p in NOT_ITEMS_PATTERNS):
			stale = items_path(out_root, item_id)
			if os.path.exists(stale):
				os.remove(stale)
			skipped.append((item_id, 'not an item'))
			continue
		broken = [m for m, j in models.chain(mid) if j is None and m != 'minecraft:builtin/generated']
		if broken:
			skipped.append((item_id, 'missing parent ' + broken[0]))
			continue
		if item_id in GRASS:
			tints = [{'type': 'minecraft:grass', 'temperature': 0.5, 'downfall': 1.0}]
			count('turf')
		elif is_tinted(item_id):
			tints = item_tints(models.tint_count(mid))
			count('tinted')
		else:
			tints = []
			count('plain')
		write_json(items_path(out_root, item_id), {'model': plain(mid, tints)})

	for kind in sorted(counts):
		print('%-16s %d' % (kind, counts[kind]))
	print('deleted model files: %d' % len(deleted))
	for model_id in deleted:
		print('  ' + model_id)
	print('no item definition: %d' % len(skipped))
	for item_id, why in skipped:
		print('  %s (%s)' % (item_id, why))


if __name__ == '__main__':
	main()
