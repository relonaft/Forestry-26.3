package forestry.core.content.backpacks.client;

import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.gui.NaturalistInventoryScreen;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.backpacks.features.BackpackMenuTypes;
import forestry.core.content.backpacks.gui.NaturalistBackpackMenu;
import forestry.core.content.backpacks.gui.BackpackScreen;
import forestry.core.content.backpacks.items.BackpackItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import javax.annotation.Nullable;

public class StorageClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(StorageClientHandler::onClientSetup);
		modBus.addListener(StorageClientHandler::registerBackpackItemProperties);
	}

	private static void registerBackpackItemProperties(RegisterRangeSelectItemModelPropertyEvent event) {
		// addon backpacks from IBackpackInterface name this property in their item model definition
		event.register(ForestryConstants.forestry("backpack_mode"), BackpackModeProperty.MAP_CODEC);
	}

	private static void onClientSetup(RegisterMenuScreensEvent event) {
		event.register(BackpackMenuTypes.BACKPACK.menuType(), BackpackScreen::new);
		event.register(BackpackMenuTypes.NATURALIST_BACKPACK.menuType(), NaturalistInventoryScreen<NaturalistBackpackMenu>::new);
	}

	// replaces the "mode" item property of the backpack overrides
	public record BackpackModeProperty() implements RangeSelectItemModelProperty {
		public static final MapCodec<BackpackModeProperty> MAP_CODEC = MapCodec.unit(new BackpackModeProperty());

		@Override
		public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
			return BackpackItem.getMode(stack).ordinal();
		}

		@Override
		public MapCodec<BackpackModeProperty> type() {
			return MAP_CODEC;
		}
	}
}
