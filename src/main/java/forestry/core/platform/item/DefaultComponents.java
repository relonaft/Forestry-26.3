package forestry.core.platform.item;

import forestry.api.ForestryConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

// burn time and compost chance were data maps until 26.1, they are item components now
public final class DefaultComponents {
	private DefaultComponents() {
	}

	public static void cookingFuel(ModifyDefaultComponentsEvent event, ItemLike item, ResourceKey<ContextIntProvider> burnTime) {
		event.modify(item, (components, context, key) -> components.set(DataComponents.COOKING_FUEL, new CookingFuel(burnTime, ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)));
	}

	public static void compostable(ModifyDefaultComponentsEvent event, ItemLike item, ResourceKey<ContextIntProvider> layers) {
		event.modify(item, (components, context, key) -> components.set(DataComponents.COMPOSTABLE, new Compostable(ResolvableInt.fromKey(layers))));
	}

	// the providers Forestry ships in data/forestry/context_int_provider
	public static ResourceKey<ContextIntProvider> provider(String path) {
		return ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, ForestryConstants.forestry(path));
	}
}
