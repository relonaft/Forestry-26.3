# Checks that every block and item of the port gets its registry key before the vanilla constructor runs.
# Reads the compiled classes with javap, so it sees what the JVM will run. Nothing is launched.
#
# usage: python -I check_setid.py <worktree> [-v]      (compile the four source sets first)
#
# "Game constructor" below: a constructor of a class outside forestry/ that takes Item.Properties or
# BlockBehaviour.Properties. Every block and item ends in one, through super(...) or through new.
#
#  R1 a call to a game constructor gets its properties from PendingId.block / item / blockItem
#     (blockItem when the constructor also takes a Block). The stamp is the last PendingId call before it.
#  R2 the one exception is handed properties: a constructor reference, or a lambda that takes properties as
#     a parameter (not captured) and passes a parameter straight on, inside a features class or FeatureRegistry.
#  R3 FeatureRegistry stamps the properties it hands out (block, block item, item, item group).
#  R4 blocks and items are built only in features classes, the registration package or a listed factory,
#     and a listed factory method is called only from there. PendingId knows the key only while
#     FeatureBlock / FeatureItem run the constructor.
#  R5 only FeatureBlock and FeatureItem open the block and item registries, and they register through
#     PendingId.construct.
import os
import re
import subprocess
import sys

SETS = ['main', 'farms', 'mail', 'butterflies']
ITEM_P = 'Lnet/minecraft/world/item/Item$Properties;'
BLOCK_P = 'Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;'
BLOCK_ARG = 'Lnet/minecraft/world/level/block/Block;'
REG_PKG = 'forestry/core/platform/registration/'
PENDING = REG_PKG + 'PendingId'
REGISTRY = REG_PKG + 'FeatureRegistry'
# classes outside features and registration that build blocks or items: other owners their methods are called through
FACTORIES = {
    # called by FeatureRegistry.backpack and naturalistBackpack
    'forestry/core/content/backpacks/BackpackInterface': ['forestry/api/core/backpacks/IBackpackInterface'],
    # static create(type), used as a constructor reference by FactoryBlocks
    'forestry/core/content/machines/blocks/FactoryBlock': [],
    # static create(type, material), used as a constructor reference by FarmingBlocks
    'forestry/agriculture/multifarm/blocks/MultifarmBlock': [],
    # the bucket of each fluid, a lambda given to registry.item in the enum constructor
    'forestry/core/platform/fluids/ForestryFluids': [],
}
R3_MIN = {'block': 1, 'blockItem': 1, 'item': 2}

RE_INSN = re.compile(r'^\s+(\d+): (\w+)\s*(.*)$')
RE_REF = re.compile(r'// (?:Interface)?Method (\S+?)\.("<init>"|[\w$]+):(\S+)')
RE_CLASS = re.compile(r'// class (\S+)')
RE_HANDLE = re.compile(r'^\s+#\d+ REF_(\w+) (\S+?)\.("<init>"|[\w$]+):(\S+)')
RE_FIELD = re.compile(r'// Field (\S+?)\.(\w+):')
RE_METHOD = re.compile(r'^  [^ ].*\(.*\).*;$|^  static \{\};$')


def javap(root):
    files = []
    for s in SETS:
        for dp, _, fn in os.walk(os.path.join(root, 'build', 'classes', 'java', s)):
            files += [os.path.join(dp, f) for f in fn if f.endswith('.class')]
    if not files:
        sys.exit('no compiled classes under build/classes/java, compile first')
    out = []
    for i in range(0, len(files), 60):
        res = subprocess.run(['javap', '-v', '-p'] + files[i:i + 60], capture_output=True, text=True, encoding='utf-8', errors='replace')
        if res.returncode != 0:
            sys.exit('javap failed: ' + res.stderr[:500])
        out.append(res.stdout)
    return len(files), '\n'.join(out)


def parse(text):
    classes = []
    cur = None
    method = None
    pending = None
    for line in text.split('\n'):
        if line.startswith('Classfile '):
            cur = {'name': None, 'super': None, 'methods': [], 'handles': [], 'lambda_types': {}}
            classes.append(cur)
            method = None
            pending = None
            continue
        if cur is None:
            continue
        m = re.match(r'\s+this_class: #\d+\s+// (\S+)', line)
        if m:
            cur['name'] = m.group(1)
            continue
        m = re.match(r'\s+super_class: #\d+\s+// (\S+)', line)
        if m:
            cur['super'] = m.group(1)
            continue
        if RE_METHOD.match(line):
            m = re.search(r'([\w$<>.]+)\(', line)
            name = m.group(1).split('.')[-1] if m else line.strip()
            method = (name, [])
            cur['methods'].append(method)
            continue
        m = RE_INSN.match(line)
        if m and method is not None:
            method[1].append((m.group(2), m.group(3)))
            continue
        m = RE_HANDLE.match(line)
        if m:
            cur['handles'].append((m.group(1), m.group(2), m.group(3).strip('"'), m.group(4)))
            pending = m.group(3)
            continue
        # the line after a bootstrap handle is the method type the lambda is called with, without captured values
        m = re.match(r'^\s+#\d+ (\(\S*\)\S+)$', line)
        if m and pending is not None:
            cur['lambda_types'][pending] = m.group(1)
        pending = None
    return classes


def needed(desc):
    if BLOCK_P in desc:
        return 'block'
    if ITEM_P in desc:
        return 'blockItem' if BLOCK_ARG in desc else 'item'
    return None


def allowed_place(cn):
    base = cn.split('$')[0]
    return '/features/' in base or base.startswith(REG_PKG) or base in FACTORIES


def main():
    root = sys.argv[1]
    verbose = '-v' in sys.argv
    count, text = javap(root)
    classes = parse(text)
    by_name = {c['name']: c for c in classes}
    fails = []
    stamped = []
    handed = []
    roots = set()      # classes of the mod with a super(...) into a game constructor
    sites = []         # (class, method, built class) for every place that builds a block or item

    # R1, R2
    for c in classes:
        cn = c['name']
        for ms, insns in c['methods']:
            last = None
            news = []
            for i, (op, arg) in enumerate(insns):
                if op == 'new':
                    r = RE_CLASS.search(arg)
                    news.append(r.group(1) if r else '?')
                    continue
                r = RE_REF.search(arg)
                if not r:
                    continue
                owner, name, desc = r.group(1), r.group(2).strip('"'), r.group(3)
                if op == 'invokestatic' and owner == PENDING and name in ('block', 'item', 'blockItem'):
                    last = name
                    continue
                if op != 'invokespecial' or name != '<init>' or owner.startswith('forestry/'):
                    continue
                need = needed(desc)
                if need is None:
                    continue
                if owner in news:
                    sites.append((cn, ms, owner))
                else:
                    roots.add(cn)
                prev = insns[i - 1][0] if i > 0 else ''
                if last == need:
                    stamped.append((cn, ms, owner, need))
                elif ms.startswith('lambda$') and prev.startswith('aload') and desc.endswith('Properties;)V') and needed(c['lambda_types'].get(ms, '').split(')')[0]):
                    handed.append((cn, ms, owner, need))
                else:
                    fails.append('R1 %s.%s calls %s.<init> without PendingId.%s' % (cn, ms, owner, need))
                last = None
        for kind, owner, name, desc in c['handles']:
            if kind == 'newInvokeSpecial' and not owner.startswith('forestry/') and needed(desc):
                handed.append((cn, '::new', owner, needed(desc)))
                sites.append((cn, '::new', owner))
    for cn, ms, owner, need in handed:
        base = cn.split('$')[0]
        if '/features/' not in base and base != REGISTRY:
            fails.append('R2 %s.%s hands unstamped properties to %s.<init> outside a features class' % (cn, ms, owner))

    # R3
    got = {}
    reg = by_name.get(REGISTRY)
    for ms, insns in (reg['methods'] if reg else []):
        for op, arg in insns:
            r = RE_REF.search(arg)
            if r and op == 'invokestatic' and r.group(1) == PENDING:
                got[r.group(2)] = got.get(r.group(2), 0) + 1
    for name, least in R3_MIN.items():
        if got.get(name, 0) < least:
            fails.append('R3 FeatureRegistry calls PendingId.%s %d times, expected at least %d' % (name, got.get(name, 0), least))

    # R4
    family = set(roots)
    changed = True
    while changed:
        changed = False
        for c in classes:
            if c['name'] not in family and c['super'] in family:
                family.add(c['name'])
                changed = True
    for c in classes:
        cn = c['name']
        for ms, insns in c['methods']:
            for op, arg in insns:
                r = RE_CLASS.search(arg)
                if op == 'new' and r and r.group(1) in family:
                    sites.append((cn, ms, r.group(1)))
        for kind, owner, name, desc in c['handles']:
            if kind == 'newInvokeSpecial' and owner in family:
                sites.append((cn, '::new', owner))
    factory_methods = {}
    for cn, ms, built in sites:
        base = cn.split('$')[0]
        if not allowed_place(cn):
            fails.append('R4 %s.%s builds %s outside features, registration and the listed factories' % (cn, ms, built))
        elif base in FACTORIES and not ms.startswith('lambda$'):
            factory_methods.setdefault(base, set()).add(ms)
    factory_calls = 0
    for c in classes:
        cn = c['name']
        base = cn.split('$')[0]
        refs = []
        for ms, insns in c['methods']:
            for op, arg in insns:
                r = RE_REF.search(arg)
                if r and op.startswith('invoke'):
                    refs.append((r.group(1), r.group(2).strip('"')))
        refs += [(owner, name) for kind, owner, name, desc in c['handles']]
        for owner, name in refs:
            for fac, names in factory_methods.items():
                if name in names and (owner == fac or owner in FACTORIES[fac]) and base != fac:
                    factory_calls += 1
                    if '/features/' not in base and not base.startswith(REG_PKG):
                        fails.append('R4 %s calls the factory %s.%s outside features and registration' % (cn, fac, name))
    built = set(b for _, _, b in sites)

    # R5
    users = set()
    for c in classes:
        cn = c['name']
        for ms, insns in c['methods']:
            for i, (op, arg) in enumerate(insns):
                f = RE_FIELD.search(arg)
                if op == 'getstatic' and f and f.group(1) == 'net/minecraft/core/registries/Registries' and f.group(2) in ('ITEM', 'BLOCK'):
                    nxt = insns[i + 1][1] if i + 1 < len(insns) else ''
                    if 'getRegistry:' in nxt:
                        users.add(cn)
                        if cn not in (REG_PKG + 'FeatureBlock', REG_PKG + 'FeatureItem'):
                            fails.append('R5 %s.%s opens the block or item registry' % (cn, ms))
    for cn in (REG_PKG + 'FeatureBlock', REG_PKG + 'FeatureItem'):
        c = by_name.get(cn)
        if c is None or not any(PENDING + '.construct' in arg for ms, insns in c['methods'] for op, arg in insns):
            fails.append('R5 %s does not register through PendingId.construct' % cn)

    kinds = {}
    for _, _, _, need in stamped:
        kinds[need] = kinds.get(need, 0) + 1
    print('classes read: %d' % count)
    print('R1 game constructor calls stamped: %d (%s)' % (len(stamped), ', '.join('%s %d' % kv for kv in sorted(kinds.items()))))
    print('R2 game constructor calls with handed properties: %d' % len(handed))
    print('R3 FeatureRegistry stamps: %s' % ', '.join('%s %d' % kv for kv in sorted(got.items())))
    print('R4 block and item classes of the mod: %d, of them built: %d, build sites: %d in %d classes, factory calls: %d'
          % (len(family), len(family & built), len(sites), len(set(s[0].split('$')[0] for s in sites)), factory_calls))
    print('R5 block and item registry users: %s' % ', '.join(sorted(x.split('/')[-1] for x in users)))
    print('--- handed')
    for row in sorted(set(handed)):
        print('  %s.%s -> %s [%s]' % row)
    print('--- classes of the mod never built (abstract bases)')
    for cn in sorted(family - built):
        print('  ' + cn)
    if verbose:
        print('--- stamped')
        for row in sorted(set(stamped)):
            print('  %s.%s -> %s [%s]' % row)
        print('--- build sites')
        for row in sorted(set(sites)):
            print('  %s.%s builds %s' % row)
    if fails:
        print('--- FAIL: %d' % len(set(fails)))
        for f in sorted(set(fails)):
            print('  ' + f)
        sys.exit(1)
    print('OK')


main()
