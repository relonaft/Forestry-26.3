package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

// since 1.21.2 a block or item reads its registry key from its properties inside the vanilla constructor
public final class PendingId {
	// a thread local because many constructors build their own properties and never see the registry
	private static final ThreadLocal<ResourceKey<?>> PENDING = new ThreadLocal<>();

	private PendingId() {
	}

	static <T, V extends T> V construct(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<V> constructor) {
		ResourceKey<?> outer = PENDING.get();
		PENDING.set(ResourceKey.create(registry, id));
		try {
			return constructor.get();
		} finally {
			PENDING.set(outer);
		}
	}

	// must wrap the properties of every call to a vanilla block constructor
	public static BlockBehaviour.Properties block(BlockBehaviour.Properties properties) {
		return properties.setId(key(Registries.BLOCK));
	}

	// must wrap the properties of every call to a vanilla item constructor
	public static Item.Properties item(Item.Properties properties) {
		return properties.setId(key(Registries.ITEM));
	}

	// names the item like its block, as BlockItem.getDescriptionId did before 1.21.2
	public static Item.Properties blockItem(Block block, Item.Properties properties) {
		return item(properties).overrideDescription(block.getDescriptionId());
	}

	private static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry) {
		ResourceKey<?> pending = PENDING.get();
		if (pending == null) {
			throw new IllegalStateException("Constructed outside of FeatureBlock and FeatureItem, no key for " + registry.identifier());
		}
		return pending.cast(registry).orElseThrow(() -> new IllegalStateException("Constructing " + pending + ", not an entry of " + registry.identifier()));
	}
}
