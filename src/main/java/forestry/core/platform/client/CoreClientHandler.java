package forestry.core.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.api.client.IForestryClientApi;
import forestry.api.core.ISpectacleBlock;
import forestry.apiimpl.client.ForestryClientApiImpl;
import forestry.apiimpl.plugin.PluginManager;
import forestry.core.engine.circuits.SolderingIronScreen;
import forestry.core.platform.config.Constants;
import forestry.core.features.*;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.gui.*;
import forestry.core.content.analyzer.*;
import forestry.core.content.burnbarrel.BurnBarrelScreen;
import forestry.core.content.escritoire.*;
import forestry.core.platform.models.ClientManager;
import forestry.core.platform.models.FluidContainerModel;
import forestry.core.platform.models.ForestryItemTint;
import forestry.core.platform.models.ModelBlockCached;
import forestry.core.platform.particles.CoreParticles;
import forestry.core.platform.render.*;
import forestry.core.content.machines.*;
import forestry.core.platform.util.GeneticsUtil;
import forestry.core.platform.util.RenderUtil;
import forestry.core.platform.registration.FeatureFluid;
import forestry.core.content.energy.features.EnergyBlockEntities;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.content.machines.features.FactoryBlockEntities;
import forestry.modules.ModuleUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;

import java.util.ArrayList;
import java.util.List;

public class CoreClientHandler implements IClientModuleHandler {
	// Copied from RenderPipelines.java (just LINES_TRANSLUCENT but with no depth test)
	// deviation: drawn to the main target, a 26.3 render type cannot pick the outline target without its blur
	public static final RenderPipeline PIPELINE_LINES_XRAY = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
		.withLocation(ForestryConstants.forestry("pipeline/lines_xray"))
		.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
		.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
		.build();
	public static final RenderType RENDER_TYPE_LINES_XRAY = RenderType.create("lines_xray", RenderSetup.builder(PIPELINE_LINES_XRAY)
		.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
		.createRenderSetup());

	private static final ContextKey<List<SpectacleHighlight>> SPECTACLE_HIGHLIGHTS = new ContextKey<>(ForestryConstants.forestry("spectacle_highlights"));

	private record SpectacleHighlight(BlockPos pos, int color) {
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(CoreClientHandler::registerMenuScreens);
		modBus.addListener(CoreClientHandler::registerModelLoaders);
		modBus.addListener(CoreClientHandler::registerItemModels);
		modBus.addListener(CoreClientHandler::bakeModels);
		modBus.addListener(CoreClientHandler::setupLayers);
		modBus.addListener(CoreClientHandler::clientSetupRenderers);
		modBus.addListener(CoreClientHandler::registerReloadListeners);
		modBus.addListener(CoreClientHandler::registerAtlases);
		modBus.addListener(CoreClientHandler::registerRenderPipelines);
		modBus.addListener(CoreClientHandler::registerBlockColors);
		modBus.addListener(CoreClientHandler::registerItemTintSources);
		modBus.addListener(CoreClientHandler::registerParticleFactory);
		modBus.addListener(CoreClientHandler::registerSpecialRenderers);
		modBus.addListener(CoreClientHandler::registerFluidModels);
		NeoForge.EVENT_BUS.addListener(CoreClientHandler::extractSpectacleHighlights);
		NeoForge.EVENT_BUS.addListener(CoreClientHandler::submitSpectacleHighlights);

		ModuleUtil.getModBus(ForestryConstants.MOD_ID).addListener(EventPriority.HIGHEST, ((ForestryClientApiImpl) IForestryClientApi.INSTANCE)::initializeTextureManager);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(CoreMenuTypes.PORTABLE_ANALYZER.menuType(), PortableAnalyzerScreen::new);
		event.register(CoreMenuTypes.ANALYZER.menuType(), AnalyzerScreen::new);
		event.register(CoreMenuTypes.NATURALIST_INVENTORY.menuType(), NaturalistInventoryScreen<NaturalistInventoryMenu>::new);
		event.register(CoreMenuTypes.ESCRITOIRE.menuType(), EscritoireScreen::new);
		event.register(CoreMenuTypes.BURN_BARREL.menuType(), BurnBarrelScreen::new);
		event.register(CoreMenuTypes.SOLDERING_IRON.menuType(), SolderingIronScreen::new);
	}

	private static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
		// Client model registration depends on TreeManager / BeeManager / etc. being
		// initialized. RegisterLoaders fires before FMLCommonSetupEvent, so make
		// sure the API is bootstrapped before plugin client registration runs.
		forestry.core.ModuleCore.ensureApiInitialized();
		PluginManager.registerClient();
	}

	private static void registerItemModels(RegisterItemModelsEvent event) {
		event.register(Identifier.fromNamespaceAndPath(ForestryConstants.MOD_ID, "fluid_container"), FluidContainerModel.Unbaked.MAP_CODEC);
	}

	private static void bakeModels(ModelEvent.ModifyBakingResult event) {
		ClientManager.INSTANCE.onBakeModels(event);
	}

	private static void setupLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ForestryModelLayers.NATURALIST_CHEST_LAYER, NaturalistChestRenderer::createBodyLayer);
		event.registerLayerDefinition(ForestryModelLayers.MILL_LAYER, RainmakerRenderer::createBodyLayer);
		event.registerLayerDefinition(ForestryModelLayers.ENGINE_LAYER, EngineRenderer::createBodyLayer);
	}

	private static void clientSetupRenderers(EntityRenderersEvent.RegisterRenderers event) {
		// Core
		event.registerBlockEntityRenderer(CoreTiles.ANALYZER.tileType(), AnalyzerRenderer::new);
		event.registerBlockEntityRenderer(CoreTiles.ESCRITOIRE.tileType(), EscritoireRenderer::new);
		// Apiculture/Arboriculture/Lepidopterology
		event.registerBlockEntityRenderer(CoreTiles.APIARIST_CHEST.tileType(), ctx -> new NaturalistChestRenderer(ctx, "apiaristchest"));
		event.registerBlockEntityRenderer(CoreTiles.ARBORIST_CHEST.tileType(), ctx -> new NaturalistChestRenderer(ctx, "arbchest"));
		event.registerBlockEntityRenderer(CoreTiles.LEPIDOPTERIST_CHEST.tileType(), ctx -> new NaturalistChestRenderer(ctx, "lepichest"));
		// Engine
		event.registerBlockEntityRenderer(EnergyBlockEntities.CLOCKWORK_ENGINE.tileType(), ctx -> new EngineRenderer(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_clock_"));
		event.registerBlockEntityRenderer(EnergyBlockEntities.BIOGAS_ENGINE.tileType(), ctx -> new EngineRenderer(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_bronze_"));
		event.registerBlockEntityRenderer(EnergyBlockEntities.PEAT_ENGINE.tileType(), ctx -> new EngineRenderer(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_copper_"));
		event.registerBlockEntityRenderer(EnergyBlockEntities.COMBUSTION_ENGINE.tileType(), ctx -> new EngineRenderer(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_iron_"));
		event.registerBlockEntityRenderer(EnergyBlockEntities.SOLAR_ENGINE.tileType(), ctx -> new EngineRenderer(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_tin_"));
		// Factory
		event.registerBlockEntityRenderer(FactoryBlockEntities.RAINMAKER.tileType(), ctx -> new RainmakerRenderer(ctx, Constants.TEXTURE_PATH_BLOCK + "/rainmaker_"));
	}

	private static void registerReloadListeners(AddClientReloadListenersEvent event) {
		event.addListener(ForestryConstants.forestry("colour_properties"), ColourProperties.INSTANCE);

		event.addListener(ForestryConstants.forestry("model_cache"), (currentReload, taskExecutor, prepBarrier, reloadExecutor) -> {
			return prepBarrier.wait(Unit.INSTANCE).thenRunAsync(ModelBlockCached::clear, reloadExecutor);
		});
	}

	// replaces the sprite uploader reload listener, the vanilla atlas manager stitches the atlas now
	private static void registerAtlases(RegisterTextureAtlasesEvent event) {
		event.register(ForestrySpriteUploader.ATLAS_CONFIG);
	}

	private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
		event.registerPipeline(PIPELINE_LINES_XRAY);
	}

	private static void registerParticleFactory(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(CoreParticles.REFRACTORY_WAX.get(), RefractoryWaxParticle::new);
	}

	// which items use them is listed in their item model definitions under assets/forestry/items
	private static void registerSpecialRenderers(RegisterSpecialModelRendererEvent event) {
		event.register(ForestryConstants.forestry("naturalist_chest"), NaturalistChestRenderer.Special.Unbaked.MAP_CODEC);
		event.register(ForestryConstants.forestry("engine"), EngineRenderer.Special.Unbaked.MAP_CODEC);
		event.register(ForestryConstants.forestry("rainmaker"), RainmakerRenderer.Special.Unbaked.MAP_CODEC);
	}

	private static void registerFluidModels(RegisterFluidModelsEvent event) {
		for (ForestryFluids fluid : ForestryFluids.values()) {
			FluidType type = fluid.getFluid().getFluidType();
			if (!(type instanceof FeatureFluid.ForestryFluidType forestryType)) {
				continue;
			}
			// translucent like the old render layer, no tint source since the textures are tinted already
			event.register(new FluidModel.Unbaked(
				new Material(forestryType.getStillTexture(), true),
				new Material(forestryType.getFlowingTexture(), true),
				null,
				(FluidTintSource) null
			), fluid.getFluid(), fluid.getFlowing());
		}
	}

	private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
		// Turf wears the grass texture and tints it by biome, the same way vanilla tints its grass
		event.register(List.of(BlockTintSources.grass()), CoreBlocks.TURF_BLOCK.block(), CoreBlocks.TURF.block());

		// Packaging machine tank slices, tinted with the fluid colour of whatever they hold
		event.register(ClientManager.FORESTRY_BLOCK_TINTS, FactoryBlocks.PLAIN.blockArray());
	}

	private static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
		// which items use it is listed in their item model definitions under assets/forestry/items
		event.register(Identifier.fromNamespaceAndPath(ForestryConstants.MOD_ID, "item_tint"), ForestryItemTint.MAP_CODEC);
	}

	private static void extractSpectacleHighlights(ExtractLevelRenderStateEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;

		if (player != null) {
			if (GeneticsUtil.hasNaturalistEye(player)) {
				// Draw lines around pollinated leaves and wild hives
				List<SpectacleHighlight> highlights = new ArrayList<>();

				int renderDistance = minecraft.options.renderDistance().get();
				BlockPos playerPos = player.blockPosition();
				ChunkPos playerChunkPos = ChunkPos.containing(playerPos);

				long gameTime = event.getLevel().getGameTime();
				float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);

				// Steady rainbow outline (pollinated leaves, assembled multiblock anchors)...
				int rainbow = ARGB.opaque(RenderUtil.getRainbowColor(gameTime, partialTick).getRGB());
				// ...and a pulsing white outline for work-in-progress markers (unformed multiblock parts).
				int flashing = ARGB.colorFromFloat(RenderUtil.getFlashingAlpha(gameTime, partialTick), 1.0F, 1.0F, 1.0F);

				// Iterate through all chunks in render distance
				for (int chunkX = playerChunkPos.x() - renderDistance; chunkX <= playerChunkPos.x() + renderDistance; chunkX++) {
					for (int chunkZ = playerChunkPos.z() - renderDistance; chunkZ <= playerChunkPos.z() + renderDistance; chunkZ++) {
						LevelChunk chunk = event.getLevel().getChunk(chunkX, chunkZ);

						// Get all block entities in the chunk
						for (BlockEntity be : chunk.getBlockEntities().values()) {
							if (be instanceof ISpectacleBlock naturalist && naturalist.isHighlighted(player)) {
								highlights.add(new SpectacleHighlight(be.getBlockPos(), naturalist.usesFlashingHighlight(player) ? flashing : rainbow));
							}
						}
					}
				}

				event.getRenderState().setRenderData(SPECTACLE_HIGHLIGHTS, highlights);
			}
		}
	}

	private static void submitSpectacleHighlights(SubmitCustomGeometryEvent event) {
		LevelRenderState levelState = event.getLevelRenderState();
		List<SpectacleHighlight> highlights = levelState.getRenderData(SPECTACLE_HIGHLIGHTS);

		if (highlights != null) {
			PoseStack stack = event.getPoseStack();
			SubmitNodeCollector collector = event.getSubmitNodeCollector();
			Vec3 cameraPos = levelState.cameraRenderState.pos;
			float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;

			for (SpectacleHighlight highlight : highlights) {
				BlockPos pos = highlight.pos();
				int color = highlight.color();

				stack.pushPose();
				// Translate the matrix stack to avoid floating point precision errors
				stack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

				// render at origin (inflate slightly to avoid weirdness with selection box)
				// not submitCustomGeometry: a blended type goes to the translucent phase, which has no OIT pipeline for it and
				// crashes with improved transparency on
				collector.submitSpecial(RenderPhaseKeys.ALWAYS_ON_TOP, new CustomFeatureRenderer.Submit(stack.last().copy(), RENDER_TYPE_LINES_XRAY,
					(pose, buffer) -> RenderUtil.lineBox(pose, buffer, -0.001F, 1.001F, color, width)));

				stack.popPose();
			}
		}
	}
}
