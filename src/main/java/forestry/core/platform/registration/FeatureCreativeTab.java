package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class FeatureCreativeTab extends ModFeature {
	private final DeferredHolder<CreativeModeTab, CreativeModeTab> creativeTabObject;

	public FeatureCreativeTab(IFeatureRegistry registry, Identifier moduleId, String name, Consumer<CreativeModeTab.Builder> builder) {
		super(moduleId, name);

		this.creativeTabObject = registry.getRegistry(Registries.CREATIVE_MODE_TAB).register(name, () -> {
			CreativeModeTab.Builder tab = new DistinctBuilder().title(Component.translatable("itemGroup." + name));
			builder.accept(tab);
			return tab.build();
		});
	}

	// NeoForge 1.21.1 kept the first of two equal stacks in a tab, 26.3 throws on the second
	private static final class DistinctBuilder extends CreativeModeTab.Builder {
		private DistinctBuilder() {
			super(CreativeModeTab.Row.TOP, 0);
		}

		@Override
		public CreativeModeTab.Builder displayItems(CreativeModeTab.DisplayItemsGenerator generator) {
			return super.displayItems((parameters, output) -> {
				Set<ItemStack> shown = ItemStackLinkedSet.createTypeAndComponentsSet();
				generator.accept(parameters, (stack, visibility) -> {
					if (visibility == CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY || shown.add(stack)) {
						output.accept(stack, visibility);
					} else if (visibility == CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS) {
						output.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
					}
				});
			});
		}
	}

	public CreativeModeTab creativeTab() {
		return this.creativeTabObject.get();
	}

	@Override
	public ResourceKey<? extends Registry<?>> getRegistry() {
		return Registries.CREATIVE_MODE_TAB;
	}

	public ResourceKey<CreativeModeTab> getKey() {
		return Objects.requireNonNull(this.creativeTabObject.getKey());
	}
}
