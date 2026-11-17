package forestry.lepidopterology.proxy;

import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.render.ForestryModelLayers;
import forestry.lepidopterology.features.LepidopterologyEntities;
import forestry.lepidopterology.butterflies.ItemButterflyGE;
import forestry.lepidopterology.render.ButterflyEntityRenderer;
import forestry.lepidopterology.render.ButterflyItemModel;
import forestry.lepidopterology.render.ButterflyModel;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.bus.api.IEventBus;

import javax.annotation.Nullable;

public class LepidopterologyClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(LepidopterologyClientHandler::setupRenderers);
		modBus.addListener(LepidopterologyClientHandler::setupLayers);
		modBus.addListener(LepidopterologyClientHandler::registerItemModels);
		modBus.addListener(LepidopterologyClientHandler::registerItemProperties);
	}

	private static void registerItemProperties(RegisterRangeSelectItemModelPropertyEvent event) {
		event.register(ForestryConstants.forestry("cocoon_age"), CocoonAgeProperty.MAP_CODEC);
	}

	public static void setupRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(LepidopterologyEntities.BUTTERFLY.entityType(), ButterflyEntityRenderer::new);
	}

	public static void setupLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ForestryModelLayers.BUTTERFLY_LAYER, ButterflyModel::createLayer);
	}

	public static void registerItemModels(RegisterItemModelsEvent event) {
		event.register(ForestryConstants.forestry("butterfly_ge"), ButterflyItemModel.Unbaked.MAP_CODEC);
	}

	// replaces the "age" item property that the cocoon model selected its overrides with
	public record CocoonAgeProperty() implements RangeSelectItemModelProperty {
		public static final MapCodec<CocoonAgeProperty> MAP_CODEC = MapCodec.unit(new CocoonAgeProperty());

		@Override
		public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
			return ItemButterflyGE.getAge(stack);
		}

		@Override
		public MapCodec<CocoonAgeProperty> type() {
			return MAP_CODEC;
		}
	}
}
