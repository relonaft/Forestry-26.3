"""26.1: RecipeSerializer is a record of a MapCodec and a StreamCodec, the nested Serializer classes go.

Usage: python -I codemod_recipe_classes.py <recipe class file>...

For every recipe class with a nested `Serializer implements RecipeSerializer<X>`:
- the codec() and streamCodec() overrides are dropped and the fromNetwork/toNetwork helpers move to
  the recipe class
- a public SERIALIZER constant is added after the stream codec
- getSerializer() returns it, getResultItem() loses its lookup parameter
- Ingredient.CODEC_NONEMPTY is Ingredient.CODEC (an ingredient cannot be empty since 1.21.2) and
  ItemStack.STRICT_CODEC is ItemStackTemplate.CODEC mapped back to a stack
What a class needs beyond that is edited by hand.
"""
import re
import sys

STACK_CODEC = 'ItemStackTemplate.CODEC.xmap(ItemStackTemplate::create, ItemStackTemplate::fromNonEmptyStack)'


def add_import(src, fq):
    line = 'import %s;' % fq
    if line in src:
        return src
    imports = list(re.finditer(r'^import [\w.]+;$', src, re.M))
    target = next((m for m in imports if m.group(0) > line), None)
    pos = target.start() if target else imports[-1].end() + 1
    return src[:pos] + line + '\n' + src[pos:]


def convert(path):
    src = open(path, encoding='utf-8', newline='').read()
    m = re.search(r'public class (\w+) implements (\w+)', src)
    name = m.group(1)
    start = src.find('\tpublic static class Serializer implements RecipeSerializer<%s> {' % name)
    if start < 0:
        return False
    end = src.rindex('\t}\n', start, src.rindex('}'))
    inner = src[start:end].split('\n', 1)[1]
    # drop the two accessor overrides
    inner = re.sub(r'\t\t@Override\n\t\tpublic MapCodec<%s> codec\(\) \{\n\t\t\treturn CODEC;\n\t\t\}\n\n?' % name, '', inner)
    inner = re.sub(r'\t\t@Override\n\t\tpublic StreamCodec<RegistryFriendlyByteBuf, %s> streamCodec\(\) \{\n\t\t\treturn STREAM_CODEC;\n\t\t\}\n\n?' % name, '', inner)
    inner = '\n'.join(line[1:] if line.startswith('\t') else line for line in inner.split('\n'))
    src = src[:start] + inner.rstrip('\n') + '\n' + src[end + len('\t}\n'):]
    src = src.replace('Serializer::toNetwork', name + '::toNetwork').replace('Serializer::fromNetwork', name + '::fromNetwork')
    # the constant goes right after the stream codec statement
    sc = src.index('STREAM_CODEC = ')
    stmt_end = src.index(';\n', sc) + 2
    src = src[:stmt_end] + '\tpublic static final RecipeSerializer<%s> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);\n' % name + src[stmt_end:]
    src = re.sub(r'public RecipeSerializer<\?> getSerializer\(\) \{\n\t\treturn [\w.]+\.serializer\(\);',
                 'public RecipeSerializer<%s> getSerializer() {\n\t\treturn SERIALIZER;' % name, src)
    src = re.sub(r'public ItemStack getResultItem\(HolderLookup\.Provider \w+\)', 'public ItemStack getResultItem()', src)
    src = src.replace('Ingredient.CODEC_NONEMPTY', 'Ingredient.CODEC')
    if 'ItemStack.STRICT_CODEC' in src:
        src = src.replace('ItemStack.STRICT_CODEC', STACK_CODEC)
        src = add_import(src, 'net.minecraft.world.item.ItemStackTemplate')
    open(path, 'w', encoding='utf-8', newline='').write(src)
    return True


def main():
    for path in sys.argv[1:]:
        print('%-60s %s' % (path.replace(chr(92), '/').split('/')[-1], 'converted' if convert(path) else 'no nested Serializer'))


if __name__ == '__main__':
    main()
