package forestry.arboriculture.client;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.leaves.DecorativeLeavesBlock;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.features.ArboricultureEntities;
import forestry.arboriculture.features.ArboricultureTiles;
import forestry.arboriculture.models.*;
import forestry.core.platform.models.ClientManager;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.bus.api.IEventBus;

public class ArboricultureClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ArboricultureClientHandler::registerBlockColors);
		modBus.addListener(ArboricultureClientHandler::registerBlockStateModels);
		modBus.addListener(ArboricultureClientHandler::registerItemModels);
		modBus.addListener(ArboricultureClientHandler::registerEntityRenderers);
		modBus.addListener(ArboricultureClientHandler::registerModelLayers);
	}

	private static void registerItemModels(RegisterItemModelsEvent event) {
		event.register(ForestryConstants.forestry("sapling_ge"), SaplingModel.ItemUnbaked.MAP_CODEC);
	}

	private static void registerBlockStateModels(RegisterBlockStateModels event) {
		event.registerModel(ForestryConstants.forestry("sapling_ge"), SaplingModel.Unbaked.MAP_CODEC);

		// registered here and not in FMLClientSetupEvent, which races the ModifyBakingResult that reads these models back
		ClientManager clientManager = ClientManager.INSTANCE;
		clientManager.registerModel(new LeavesModel(), ArboricultureBlocks.LEAVES);
		clientManager.registerModel(new DecorativeLeavesModel<>(DecorativeLeavesBlock.class), ArboricultureBlocks.LEAVES_DECORATIVE);
		clientManager.registerModel(new DefaultLeavesModel(), ArboricultureBlocks.LEAVES_DEFAULT);
		clientManager.registerModel(new DefaultFruitLeavesModel(), ArboricultureBlocks.LEAVES_DEFAULT_FRUIT);
	}

	private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ArboricultureEntities.BOAT.entityType(), ctx -> new ForestryBoatRenderer(ctx, false));
		event.registerEntityRenderer(ArboricultureEntities.CHEST_BOAT.entityType(), ctx -> new ForestryBoatRenderer(ctx, true));
		// draws the text only, the sign itself is a block model since Sheets.addWoodType is gone
		event.registerBlockEntityRenderer(ArboricultureTiles.SIGN.tileType(), StandingSignRenderer::new);
		// HANGING_SIGN BE renderer is provided by vanilla; Forestry's hanging-sign blocks reuse
		// vanilla's BlockEntityType.HANGING_SIGN via BlockEntityTypeAddBlocksEvent.
	}

	private static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		for (ForestryWoodType type : ForestryWoodType.VALUES) {
			event.registerLayerDefinition(ForestryBoatRenderer.createBoatModelLocation(type, false), BoatModel::createBoatModel);
			event.registerLayerDefinition(ForestryBoatRenderer.createBoatModelLocation(type, true), BoatModel::createChestBoatModel);
		}
	}

	private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
		event.register(ClientManager.FORESTRY_BLOCK_TINTS, ArboricultureBlocks.LEAVES.block());
		event.register(ClientManager.FORESTRY_BLOCK_TINTS, ArboricultureBlocks.LEAVES_DEFAULT.blockArray());
		event.register(ClientManager.FORESTRY_BLOCK_TINTS, ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.blockArray());
		event.register(ClientManager.FORESTRY_BLOCK_TINTS, ArboricultureBlocks.LEAVES_DECORATIVE.blockArray());
	}

}
