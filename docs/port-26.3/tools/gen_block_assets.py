"""Write the 26.3 block assets of Forestry saplings and signs.

Usage: python -I gen_block_assets.py <port root> <minecraft 26.3 jar> <minecraft 1.21.1 resources jar>
Needs Pillow.

Saplings: a 26.3 block model may only use sprites of the block atlas, and the species sapling block models
used their item textures. Each such texture is added to the block atlas under a block/ name, and the block
model points at that name. The item model keeps the item texture.

Chains: 26.3 has no block/chain model any more. Its replacement is block/template_chain, which names the
texture slot "texture" instead of "all".

Signs: until 1.21.x a sign block entity renderer drew the sign from an entity texture
(textures/entity/signs/<wood>.png, 64x32). In 26.3 the sign is a block model on the vanilla templates and
the renderer draws only the text. The block texture (32x32) and the edit screen texture (24x26) of every
Forestry wood are cut from its entity texture with the same rectangles vanilla uses. The script checks
those rectangles against every vanilla wood that exists in both jars before it writes anything.
"""
import io
import json
import os
import re
import sys
import zipfile

from PIL import Image

# (new x0, y0, x1, y1) <- (old x, old y, transform), transform bits: 1 flip x, 2 flip y, 4 swap axes
STANDING = [
	((0, 0, 24, 2), (2, 0, 0)), ((0, 2, 24, 14), (2, 2, 0)), ((0, 16, 24, 28), (28, 2, 0)),
	((0, 28, 24, 30), (26, 0, 2)), ((24, 2, 26, 14), (26, 2, 0)), ((24, 16, 26, 28), (0, 2, 0)),
	((28, 0, 30, 14), (2, 16, 0)), ((30, 0, 32, 14), (4, 16, 0)), ((28, 16, 30, 30), (6, 16, 0)),
	((30, 16, 32, 30), (0, 16, 0)), ((28, 30, 30, 32), (4, 14, 2)),
]
HANGING = [
	((0, 0, 16, 4), (4, 0, 0)), ((0, 4, 16, 6), (4, 4, 0)), ((16, 4, 20, 6), (20, 4, 0)),
	((0, 7, 16, 9), (24, 4, 0)), ((16, 7, 20, 9), (0, 4, 0)), ((0, 9, 16, 13), (20, 0, 3)),
	((20, 0, 32, 6), (14, 6, 0)), ((22, 7, 25, 13), (0, 6, 0)), ((28, 8, 31, 12), (6, 7, 0)),
	((2, 14, 16, 16), (2, 12, 0)), ((0, 16, 2, 26), (0, 14, 0)), ((2, 16, 16, 26), (2, 14, 0)),
	((16, 16, 18, 26), (16, 14, 0)), ((18, 16, 32, 26), (18, 14, 0)), ((2, 26, 16, 28), (16, 12, 2)),
]
# the edit screen shows the front of the board and of the post
GUI = [((0, 0, 24, 12), (2, 2, 0)), ((11, 12, 13, 26), (2, 16, 0))]

# woods whose sign textures kept the names of original Forestry
TEXTURE_NAMES = {'camelthorn': 'acacia_desert', 'sour_cherry': 'hill_cherry', 'giant_sequoia': 'giganteum',
	'zebrano': 'zebrawood', 'lemon': 'citrus'}


def cut(old, size, rects):
	new = Image.new('RGBA', size, (0, 0, 0, 0))
	src = old.load()
	dst = new.load()
	for (x0, y0, x1, y1), (ox, oy, t) in rects:
		w, h = x1 - x0, y1 - y0
		sw, sh = (h, w) if t & 4 else (w, h)
		for j in range(h):
			for i in range(w):
				a, b = (j, i) if t & 4 else (i, j)
				if t & 1:
					a = sw - 1 - a
				if t & 2:
					b = sh - 1 - b
				dst[x0 + i, y0 + j] = src[ox + a, oy + b]
	return new


def image(data):
	return Image.open(io.BytesIO(data)).convert('RGBA')


def check(new_jar, old_jar):
	# every vanilla wood present in both versions must come out pixel for pixel
	new_z = zipfile.ZipFile(new_jar)
	old_z = zipfile.ZipFile(old_jar)
	old_names = set(old_z.namelist())
	cases = []
	for n in new_z.namelist():
		m = re.fullmatch(r'assets/minecraft/textures/block/([a-z_]+)_hanging_sign\.png', n)
		if m and 'assets/minecraft/textures/entity/signs/hanging/%s.png' % m.group(1) in old_names:
			cases.append((n, 'assets/minecraft/textures/entity/signs/hanging/%s.png' % m.group(1), (32, 32), HANGING))
			continue
		m = re.fullmatch(r'assets/minecraft/textures/block/([a-z_]+)_sign\.png', n)
		if m and 'assets/minecraft/textures/entity/signs/%s.png' % m.group(1) in old_names:
			cases.append((n, 'assets/minecraft/textures/entity/signs/%s.png' % m.group(1), (32, 32), STANDING))
		m = re.fullmatch(r'assets/minecraft/textures/gui/signs/([a-z_]+)\.png', n)
		if m and 'assets/minecraft/textures/entity/signs/%s.png' % m.group(1) in old_names:
			cases.append((n, 'assets/minecraft/textures/entity/signs/%s.png' % m.group(1), (24, 26), GUI))
	bad = 0
	for new_name, old_name, size, rects in cases:
		expected = image(new_z.read(new_name))
		got = cut(image(old_z.read(old_name)), size, rects)
		if expected.size != size or list(expected.getdata()) != list(got.getdata()):
			bad += 1
			print('mismatch: ' + new_name)
	print('sign texture rectangles checked on %d vanilla textures, %d mismatches' % (len(cases), bad))
	if not cases or bad:
		raise SystemExit('the sign rectangles do not reproduce vanilla')
	return new_z


def write_json(path, data):
	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, 'w', encoding='utf-8', newline='\n') as f:
		f.write(json.dumps(data, indent=2) + '\n')


def read_json(path):
	with open(path, encoding='utf-8') as f:
		return json.load(f)


def saplings(src):
	gen = os.path.join(src, 'generated', 'resources', 'assets')
	block_models = os.path.join(gen, 'forestry', 'models', 'block')
	sources = []
	for name in sorted(os.listdir(block_models)):
		path = os.path.join(block_models, name)
		if not name.endswith('.json'):
			continue
		model = read_json(path)
		changed = False
		for slot, texture in model.get('textures', {}).items():
			if isinstance(texture, str) and texture.startswith('forestry:block/'):
				# moved by an earlier run
				name = texture[len('forestry:block/'):]
				item_png = os.path.join(src, 'main', 'resources', 'assets', 'forestry', 'textures', 'item', name + '.png')
				block_png = os.path.join(src, 'main', 'resources', 'assets', 'forestry', 'textures', 'block', name + '.png')
				if os.path.exists(item_png) and not os.path.exists(block_png):
					sources.append({'type': 'minecraft:single', 'resource': 'forestry:item/' + name, 'sprite': texture})
			elif isinstance(texture, str) and texture.startswith('forestry:item/'):
				sprite = 'forestry:block/' + texture[len('forestry:item/'):]
				sources.append({'type': 'minecraft:single', 'resource': texture, 'sprite': sprite})
				model['textures'][slot] = sprite
				changed = True
		if changed:
			write_json(path, model)
	atlas_path = os.path.join(gen, 'minecraft', 'atlases', 'blocks.json')
	atlas = read_json(atlas_path)
	known = {s.get('sprite') for s in atlas['sources']}
	atlas['sources'] += [s for s in sources if s['sprite'] not in known]
	write_json(atlas_path, atlas)
	print('sapling block models moved to the block atlas: %d' % len(sources))


def chains(src):
	count = 0
	for root in ('main/resources', 'generated/resources'):
		block_models = os.path.join(src, root, 'assets', 'forestry', 'models', 'block')
		for name in sorted(os.listdir(block_models)):
			path = os.path.join(block_models, name)
			if not name.endswith('.json'):
				continue
			model = read_json(path)
			if model.get('parent') in ('block/chain', 'minecraft:block/chain'):
				textures = model['textures']
				write_json(path, {'parent': 'minecraft:block/template_chain', 'textures': {'particle': textures['particle'], 'texture': textures['all']}})
				count += 1
	print('chain models moved to block/template_chain: %d' % count)


def signs(src, new_z):
	gen = os.path.join(src, 'generated', 'resources', 'assets', 'forestry')
	textures = os.path.join(src, 'main', 'resources', 'assets', 'forestry', 'textures')
	woods = sorted(n[:-len('_wall_sign.json')] for n in os.listdir(os.path.join(gen, 'blockstates')) if n.endswith('_wall_sign.json') and not n.endswith('_hanging_sign.json'))
	vanilla = {kind: json.loads(new_z.read('assets/minecraft/blockstates/oak_%s.json' % kind)) for kind in ('sign', 'wall_sign', 'hanging_sign', 'wall_hanging_sign')}
	missing = []
	for wood in woods:
		tex = TEXTURE_NAMES.get(wood, wood)
		models = os.path.join(gen, 'models', 'block')
		particles = {}
		for kind in ('sign', 'hanging_sign'):
			path = os.path.join(models, '%s_%s.json' % (wood, kind))
			if os.path.exists(path):
				particles[kind] = read_json(path)['textures']['particle']
				# the particle-only model of the entity-rendered sign, replaced by the models below
				os.remove(path)
			else:
				particles[kind] = read_json(os.path.join(models, '%s_%s_rot_0.json' % (wood, kind)))['textures']['particle']

		def model(name, template, kind):
			write_json(os.path.join(models, name + '.json'), {'parent': 'minecraft:block/' + template,
				'textures': {'all': 'forestry:block/%s_%s' % (wood, kind), 'particle': particles[kind]}})
		for r in range(4):
			model('%s_sign_rot_%d' % (wood, r), 'template_sign_rot_%d' % r, 'sign')
			model('%s_hanging_sign_rot_%d' % (wood, r), 'template_hanging_sign_rot_%d' % r, 'hanging_sign')
			model('%s_hanging_sign_attached_rot_%d' % (wood, r), 'template_attached_hanging_sign_rot_%d' % r, 'hanging_sign')
		model('%s_wall_sign' % wood, 'template_wall_sign', 'sign')
		model('%s_wall_hanging_sign' % wood, 'template_wall_hanging_sign', 'hanging_sign')
		for kind, states in vanilla.items():
			text = json.dumps(states).replace('minecraft:block/oak_', 'forestry:block/%s_' % wood)
			write_json(os.path.join(gen, 'blockstates', '%s_%s.json' % (wood, kind)), json.loads(text))

		entity = os.path.join(textures, 'entity', 'signs', tex + '.png')
		hanging = os.path.join(textures, 'entity', 'signs', 'hanging', tex + '.png')
		for source, size, rects, out in ((entity, (32, 32), STANDING, os.path.join(textures, 'block', wood + '_sign.png')),
				(hanging, (32, 32), HANGING, os.path.join(textures, 'block', wood + '_hanging_sign.png')),
				(entity, (24, 26), GUI, os.path.join(textures, 'gui', 'signs', wood + '.png'))):
			if not os.path.exists(source):
				missing.append(os.path.relpath(out, src))
				continue
			os.makedirs(os.path.dirname(out), exist_ok=True)
			cut(Image.open(source).convert('RGBA'), size, rects).save(out)
		gui_hanging = os.path.join(textures, 'gui', 'hanging_signs', wood + '.png')
		old_gui_hanging = os.path.join(textures, 'gui', 'hanging_signs', tex + '.png')
		if not os.path.exists(gui_hanging):
			if os.path.exists(old_gui_hanging):
				with open(old_gui_hanging, 'rb') as f, open(gui_hanging, 'wb') as g:
					g.write(f.read())
			else:
				missing.append(os.path.relpath(gui_hanging, src))
	print('sign woods: %d' % len(woods))
	print('textures Forestry never had: %d' % len(missing))
	for path in missing:
		print('  ' + path.replace(os.sep, '/'))


def main():
	root, new_jar, old_jar = sys.argv[1:4]
	src = os.path.join(root, 'src')
	new_z = check(new_jar, old_jar)
	saplings(src)
	chains(src)
	signs(src, new_z)


if __name__ == '__main__':
	main()
