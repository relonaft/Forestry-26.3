"""1.21.6: BlockEntity saves to ValueOutput and loads from ValueInput instead of CompoundTag.

Usage: python -I codemod_block_entity_roots.py <port root>

Forestry keeps its CompoundTag methods. Only the six classes that extend BlockEntity directly get
the new vanilla overrides, which convert and delegate. Their old methods stop being overrides, so
the @Override and the super call are dropped there; subclasses stay untouched.
"""
import re
import sys

ROOTS = [
    'src/main/java/forestry/core/platform/tile/ForestryBlockEntity.java',
    'src/main/java/forestry/apiculture/hives/HiveBlockEntity.java',
    'src/main/java/forestry/arboriculture/fruit/FruitPodBlockEntity.java',
    'src/main/java/forestry/arboriculture/trees/TileTreeContainer.java',
    'src/butterflies/java/forestry/lepidopterology/cocoons/TileCocoon.java',
    'src/main/java/forestry/api/core/multiblock/MultiblockTileEntityBase.java',
]

BRIDGE = '''
	@Override
	protected final void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		loadAdditional(ValueIoHelper.toTag(input), input.lookup());
	}

	@Override
	protected final void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		CompoundTag data = new CompoundTag();
		saveAdditional(data, ValueIoHelper.registries(this));
		output.store(data);
	}
'''

HANDLE = '''
	@Override
	public final void handleUpdateTag(ValueInput input) {
		super.handleUpdateTag(input);
		handleUpdateTag(ValueIoHelper.toTag(input), input.lookup());
	}
'''

PACKET = '''	@Override
	public void onDataPacket(Connection net, ValueInput input) {
		CompoundTag nbt = ValueIoHelper.toTag(input);
		// an empty tag was not loaded before 1.21.6 either
		if (!nbt.isEmpty()) {
			super.onDataPacket(net, input);
		}
		handleUpdateTag(nbt, input.lookup());
	}
'''

EMPTY_METHODS = '''
	public void loadAdditional(CompoundTag data, HolderLookup.Provider registries) {
	}

	public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
	}
'''


def demote(src, name):
    """Drop @Override and the super call of the CompoundTag method `name`. Returns (src, found)."""
    rx = re.compile(
        r'(\t(?:@Nullable\n\t)?)@Override\n(\t(?:@OnlyIn\(Dist\.CLIENT\)\n\t)?public (?:final )?void %s\(CompoundTag (\w+), HolderLookup\.Provider registries\) \{\n)'
        r'\t\tsuper\.%s\(\3, registries\);\n(?:\n)?' % (name, name))
    new, n = rx.subn(lambda m: m.group(1)[:-1] + m.group(2), src)
    return new, n


def add_imports(src, names):
    for fq in names:
        line = 'import %s;' % fq
        if line in src:
            continue
        # keep the block sorted enough: put it before the first import that sorts after it
        imports = [m for m in re.finditer(r'^import [\w.]+;$', src, re.M)]
        target = next((m for m in imports if m.group(0) > line), None)
        pos = target.start() if target else imports[-1].end() + 1
        src = src[:pos] + line + '\n' + src[pos:]
    return src


def last_brace(src):
    return src.rstrip().rfind('}')


def main():
    root = sys.argv[1].rstrip('/\\') + '/'
    for rel in ROOTS:
        path = root + rel
        src = open(path, encoding='utf-8', newline='').read()
        assert '\r' not in src, rel
        is_api = '/api/' in rel
        src, a = demote(src, 'loadAdditional')
        src, b = demote(src, 'saveAdditional')
        src, c = demote(src, 'handleUpdateTag')
        extra = ''
        if a == 0 and b == 0:
            assert is_api, rel
            extra += EMPTY_METHODS
        else:
            assert a == 1 and b == 1, (rel, a, b)
        extra += BRIDGE
        if c:
            extra += HANDLE
        # the packet overrides all had the same body apart from the multiblock base
        packet = re.compile(
            r'\t@Override\n\tpublic void onDataPacket\(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup\.Provider registries\) \{\n'
            r'\t\tsuper\.onDataPacket\(net, pkt, registries\);\n\t\tCompoundTag nbt = pkt\.getTag\(\);\n\t\thandleUpdateTag\(nbt, registries\);\n\t\}\n')
        src, d = packet.subn(lambda m: PACKET, src)
        if is_api:
            old = ('\tpublic final void onDataPacket(Connection network, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {\n'
                   '\t\tsuper.onDataPacket(network, packet, registries);\n'
                   '\t\tCompoundTag nbtData = packet.getTag();\n'
                   '\t\tif (nbtData != null) {\n'
                   '\t\t\tthis.decodeDescriptionPacket(nbtData);\n'
                   '\t\t}\n'
                   '\t}\n')
            assert old in src, rel
            src = src.replace(old,
                              '\tpublic final void onDataPacket(Connection network, ValueInput input) {\n'
                              '\t\tCompoundTag nbtData = ValueIoHelper.toTag(input);\n'
                              '\t\t// an empty tag was not loaded before 1.21.6 either\n'
                              '\t\tif (!nbtData.isEmpty()) {\n'
                              '\t\t\tsuper.onDataPacket(network, input);\n'
                              '\t\t}\n'
                              '\t\tthis.decodeDescriptionPacket(nbtData);\n'
                              '\t}\n')
            d = 1
        end = last_brace(src)
        src = src[:end].rstrip('\n') + '\n' + extra + src[end:]
        src = add_imports(src, ['forestry.api.core.ValueIoHelper', 'net.minecraft.world.level.storage.ValueInput',
                                'net.minecraft.world.level.storage.ValueOutput', 'net.minecraft.nbt.CompoundTag',
                                'net.minecraft.core.HolderLookup'])
        open(path, 'w', encoding='utf-8', newline='').write(src)
        print('%-70s load %d save %d handle %d packet %d' % (rel.split('/')[-1], a, b, c, d))


if __name__ == '__main__':
    main()
