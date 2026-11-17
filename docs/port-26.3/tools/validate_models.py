"""Check the Forestry item model definitions, item models and block models against the rules of 26.3.

Usage: python -I validate_models.py <port root> <minecraft 26.3 jar> <neoforge 26.3 universal jar>

- every assets/<ns>/items/<id>.json parses as a known item model type and names models that exist
- every minecraft:model resolves: unbroken parent chain, every face texture resolves to a sprite that exists
  in the item or block atlas, and all sprites of one model come from one atlas
- every blockstate names models that resolve and use block atlas sprites only (26.3 rejects other models)
- no Forestry model names a loader that 26.3 does not have
- every item with a lang key has an item model definition

A texture is looked up the way MaterialBaker does it: item atlas first, then block atlas. The atlas contents
come from the atlases/*.json sources of the jars and of the mod.
"""
import json
import os
import re
import sys
import zipfile

ROOTS = ['main/resources', 'generated/resources', 'generated/resources_butterflies', 'generated/resources_farms',
	'generated/resources_mail', 'farms/resources', 'mail/resources', 'butterflies/resources']
ITEM_TYPES = {'minecraft:model', 'minecraft:composite', 'minecraft:range_dispatch', 'minecraft:select',
	'minecraft:condition', 'minecraft:special', 'minecraft:empty', 'minecraft:bundle/selected_item',
	'forestry:bee_ge', 'forestry:sapling_ge', 'forestry:butterfly_ge', 'forestry:fluid_container',
	'neoforge:fluid_container'}
LOADERS = {'neoforge:composite', 'neoforge:empty', 'neoforge:obj', 'neoforge:conditional'}
BLOCK_STATE_TYPES = {'forestry:sapling_ge'}


def norm(identifier):
	return identifier if ':' in identifier else 'minecraft:' + identifier


class Assets:
	def __init__(self, src, jars):
		self.files = {}  # (namespace, path below assets/<ns>/) -> loader of bytes
		self.copies = {}  # the same, with every pack that has the file
		for jar in jars:
			z = zipfile.ZipFile(jar)
			for n in z.namelist():
				m = re.match(r'assets/([^/]+)/(.+)$', n)
				if m and not n.endswith('/'):
					self.files[(m.group(1), m.group(2))] = (lambda z=z, n=n: z.read(n))
					self.copies.setdefault((m.group(1), m.group(2)), []).append(self.files[(m.group(1), m.group(2))])
		self.mod = set()
		self.duplicates = []
		for root in ROOTS:
			base = os.path.join(src, root, 'assets')
			if not os.path.isdir(base):
				continue
			for ns in os.listdir(base):
				for dirpath, _, names in os.walk(os.path.join(base, ns)):
					for name in names:
						path = os.path.join(dirpath, name)
						rel = os.path.relpath(path, os.path.join(base, ns)).replace(os.sep, '/')
						key = (ns, rel)
						if key in self.mod and re.match(r'(models|items|blockstates)/', rel):
							self.duplicates.append('%s:%s' % key)
						self.mod.add(key)
						self.files[key] = (lambda path=path: open(path, 'rb').read())
						self.copies.setdefault(key, []).append(self.files[key])

	def json(self, ns, path):
		loader = self.files.get((ns, path))
		return json.loads(loader()) if loader else None

	def texture_exists(self, identifier):
		ns, path = identifier.split(':', 1)
		return (ns, 'textures/%s.png' % path) in self.files


class Atlas:
	def __init__(self, assets, name):
		self.dirs = []
		self.singles = set()
		# atlas files of several packs merge
		for loader in assets.copies.get(('minecraft', 'atlases/%s.json' % name), []):
			for source in json.loads(loader())['sources']:
				if source['type'] == 'minecraft:directory':
					self.dirs.append((source['source'], source['prefix']))
				elif source['type'] == 'minecraft:single':
					self.singles.add(norm(source.get('sprite', source['resource'])))
		self.assets = assets

	def has(self, sprite):
		if sprite in self.singles:
			return True
		ns, path = sprite.split(':', 1)
		for source, prefix in self.dirs:
			if path.startswith(prefix) and self.assets.texture_exists('%s:%s/%s' % (ns, source, path[len(prefix):])):
				return True
		return False


class Checker:
	def __init__(self, src, jars):
		self.assets = Assets(src, jars)
		self.items = Atlas(self.assets, 'items')
		self.blocks = Atlas(self.assets, 'blocks')
		self.errors = []
		self.models_checked = set()

	def error(self, text):
		self.errors.append(text)

	def model(self, model_id):
		ns, path = model_id.split(':', 1)
		return self.assets.json(ns, 'models/%s.json' % path)

	def atlas_of(self, sprite):
		if self.items.has(sprite):
			return 'items'
		if self.blocks.has(sprite):
			return 'blocks'
		return None

	def sprites(self, model_id, where, inline=None):
		# the sprites the geometry of a model uses. An inline model (composite child) has no id of its own
		chain = []
		current = model_id
		if inline is not None:
			chain.append((model_id, inline))
			current = norm(inline['parent']) if 'parent' in inline else None
		while current:
			model = self.model(current)
			if model is None:
				if current not in ('minecraft:builtin/generated',):
					self.error('%s: model %s does not exist (parent chain of %s)' % (where, current, model_id))
				chain.append((current, None))
				break
			chain.append((current, model))
			if model.get('loader') and model['loader'] not in LOADERS:
				self.error('%s: %s names the loader %s, which 26.3 does not have' % (where, current, model['loader']))
			parent = model.get('parent')
			current = norm(parent) if parent else None
		slots = {}
		for _, model in chain:
			if model:
				for slot, value in model.get('textures', {}).items():
					slots.setdefault(slot, value)

		def resolve(name):
			seen = 0
			name = name[1:] if name.startswith('#') else name
			value = slots.get(name)
			while isinstance(value, str) and value.startswith('#') and seen < 16:
				value = slots.get(value[1:])
				seen += 1
			if isinstance(value, dict):
				value = value.get('sprite')
			return norm(value) if value else None
		used = []
		elements = next((m['elements'] for _, m in chain if m and 'elements' in m), None)
		composite = next((m for _, m in chain if m and m.get('loader') == 'neoforge:composite'), None)
		if composite is not None:
			for child in composite.get('children', {}).values():
				if isinstance(child, str):
					used += self.sprites(norm(child), where)
				else:
					used += self.sprites(model_id + '#child', where, child)
			return used
		if elements is not None:
			for element in elements:
				for face in element.get('faces', {}).values():
					sprite = resolve(face['texture'])
					if sprite is None:
						self.error('%s: %s has no texture for %s' % (where, model_id, face['texture']))
					else:
						used.append(sprite)
		elif chain and chain[-1][0] == 'minecraft:builtin/generated':
			for layer in range(5):
				sprite = resolve('layer%d' % layer)
				if sprite:
					used.append(sprite)
		return used

	def check_model(self, model_id, where, block):
		key = (model_id, block)
		if key in self.models_checked:
			return
		self.models_checked.add(key)
		atlases = set()
		for sprite in self.sprites(model_id, where):
			atlas = self.atlas_of(sprite)
			if atlas is None:
				self.error('%s: %s uses %s, which is in no atlas or has no texture' % (where, model_id, sprite))
			atlases.add(atlas)
		atlases.discard(None)
		if block and atlases - {'blocks'}:
			self.error('%s: block model %s uses sprites outside the block atlas' % (where, model_id))
		if not block and len(atlases) > 1:
			self.error('%s: item model %s mixes the item and the block atlas' % (where, model_id))

	def check_item_model(self, node, where):
		kind = node.get('type')
		if kind not in ITEM_TYPES:
			self.error('%s: unknown item model type %s' % (where, kind))
			return
		if kind == 'minecraft:model':
			self.check_model(norm(node['model']), where, False)
		elif kind == 'minecraft:special':
			if self.model(norm(node['base'])) is None:
				self.error('%s: special base %s does not exist' % (where, node['base']))
		elif kind in ('forestry:fluid_container', 'neoforge:fluid_container'):
			for slot, sprite in node['textures'].items():
				if self.atlas_of(norm(sprite)) is None:
					self.error('%s: fluid container texture %s is in no atlas' % (where, sprite))
		for child in node.get('models', []):
			self.check_item_model(child, where)
		for entry in node.get('entries', []) + node.get('cases', []):
			self.check_item_model(entry['model'], where)
		for key in ('fallback', 'on_true', 'on_false'):
			if key in node:
				self.check_item_model(node[key], where)

	def check_blockstate(self, data, where):
		variants = list(data.get('variants', {}).values())
		for part in data.get('multipart', []):
			variants.append(part['apply'])
		for variant in variants:
			for entry in variant if isinstance(variant, list) else [variant]:
				if 'type' in entry:
					if entry['type'] not in BLOCK_STATE_TYPES:
						self.error('%s: unknown block state model type %s' % (where, entry['type']))
					continue
				self.check_model(norm(entry['model']), where, True)


def main():
	root, mc_jar, nf_jar = sys.argv[1:4]
	src = os.path.join(root, 'src')
	checker = Checker(src, [mc_jar, nf_jar])
	assets = checker.assets
	for name in assets.duplicates:
		checker.error('two resource roots define %s' % name)
	counts = {'items': 0, 'blockstates': 0, 'item models': 0}
	defined = set()
	for (ns, path) in sorted(assets.mod):
		if ns == 'minecraft':
			continue
		if re.fullmatch(r'items/[^/]+\.json', path):
			counts['items'] += 1
			defined.add(path[len('items/'):-5])
			data = assets.json(ns, path)
			checker.check_item_model(data['model'], '%s:%s' % (ns, path))
		elif re.fullmatch(r'blockstates/[^/]+\.json', path):
			counts['blockstates'] += 1
			checker.check_blockstate(assets.json(ns, path), '%s:%s' % (ns, path))
		elif path.startswith('models/') and path.endswith('.json'):
			model = assets.json(ns, path)
			if model.get('loader') and model['loader'] not in LOADERS:
				checker.error('%s:%s names the loader %s, which 26.3 does not have' % (ns, path, model['loader']))
			if path.startswith('models/item/'):
				counts['item models'] += 1
	# the sapling block state model bakes these
	for (ns, path) in sorted(assets.mod):
		m = re.fullmatch(r'models/block/([a-z_]+_sapling)\.json', path)
		if ns == 'forestry' and m:
			checker.check_model('forestry:block/' + m.group(1), 'sapling_ge', True)
			if ('forestry', 'models/item/%s.json' % m.group(1)) not in assets.files:
				checker.error('sapling_ge: no item model for %s' % m.group(1))
	lang = assets.json('forestry', 'lang/en_us.json') or {}
	for root_ in ROOTS:
		path = os.path.join(src, root_, 'assets', 'forestry', 'lang', 'en_us.json')
		if os.path.exists(path):
			with open(path, encoding='utf-8') as f:
				lang.update(json.load(f))
	missing = sorted(k.split('.')[2] for k in lang if re.fullmatch(r'item\.forestry\.[a-z0-9_]+', k) and k.split('.')[2] not in defined)
	for key, value in counts.items():
		print('%-12s %d' % (key, value))
	print('item lang keys without an item model definition: %d' % len(missing))
	for name in missing:
		print('  ' + name)
	print('errors: %d' % len(checker.errors))
	for text in checker.errors:
		print('  ' + text)
	sys.exit(1 if checker.errors else 0)


if __name__ == '__main__':
	main()
