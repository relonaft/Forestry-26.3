package forestry.core.platform.models;

import forestry.core.platform.block.IColoredBlock;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.platform.tile.IRenderableTile;
import forestry.core.platform.util.ModUtil;
import forestry.core.platform.util.RenderUtil;
import forestry.core.platform.registration.FeatureBlock;
import forestry.core.platform.registration.FeatureGroup;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureTable;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum ClientManager {
	INSTANCE;

	// a block lists one tint source per tint index: leaves use 0 and 2, tank slices 0 and 1
	public static final List<BlockTintSource> FORESTRY_BLOCK_TINTS = List.of(new BlockTint(0), new BlockTint(1), new BlockTint(2));

	/* CUSTOM MODELS*/
	// keyed, not appended: registering the same block again must replace its entry
	private final Map<Block, BlockModelEntry> customBlockModels = new LinkedHashMap<>();
	private final Map<Identifier, ItemModel> customModels = new LinkedHashMap<>();

	public void registerModel(ModelBlockDefault<?, ?> model, Object feature) {
		if (feature instanceof FeatureGroup<?, ?, ?> group) {
			group.getFeatures().forEach(f -> registerModel(model, f));
		} else if (feature instanceof FeatureTable<?, ?, ?, ?> group) {
			group.getFeatures().forEach(f -> registerModel(model, f));
		} else if (feature instanceof FeatureBlock<?, ?> block) {
			registerModel(model, block.block(), block.item());
		} else if (feature instanceof FeatureItem<?> item) {
			registerModel((ItemModel) model, item.item());
		}
	}

	public void registerModel(ModelBlockDefault<?, ?> model, Block block, @Nullable BlockItem item) {
		registerModel(model, block, item, block.getStateDefinition().getPossibleStates());
	}

	public void registerModel(ModelBlockDefault<?, ?> model, Block block, @Nullable BlockItem item, Collection<BlockState> states) {
        this.customBlockModels.put(block, new BlockModelEntry(model, item, states));
	}

	public void registerModel(ItemModel model, Item item) {
        this.customModels.put(ModUtil.getRegistryName(item), model);
	}

	public void onBakeModels(ModelEvent.ModifyBakingResult event) {
		//register custom models
		Map<BlockState, BlockStateModel> blockModels = event.getBakingResult().blockStateModels();
		Map<Identifier, ItemModel> itemModels = event.getBakingResult().itemStackModels();
		for (final BlockModelEntry entry : this.customBlockModels.values()) {
			for (BlockState state : entry.states) {
				blockModels.put(state, entry.model);
			}
			if (entry.item != null) {
				Identifier registryName = ModUtil.getRegistryName(entry.item);
				if (registryName == null) {
					continue;
				}
				itemModels.put(registryName, entry.model);
			}
		}

		itemModels.putAll(this.customModels);
	}

	private record BlockModelEntry(ModelBlockDefault<?, ?> model, @Nullable BlockItem item, Collection<BlockState> states) {
	}

	private record BlockTint(int tintIndex) implements BlockTintSource {
		@Override
		public int color(BlockState state) {
			return 0xffffffff;
		}

		@Override
		public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
			// the block renderer multiplies by ARGB, so the alpha that the old block colours left at 0 must be set
			if (state.getBlock() instanceof IColoredBlock coloredBlock) {
				return ARGB.opaque(coloredBlock.colorMultiplier(state, level, pos, this.tintIndex));
			}

			// Packaging machine tank slices. tintIndex 0 is the resource tank, tintIndex 1 the product tank
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof IRenderableTile tile) {
				TankRenderInfo tankInfo = this.tintIndex == 0 ? tile.getResourceTankInfo() : tile.getProductTankInfo();
				FluidStack fluidStack = tankInfo.getFluidStack();
				if (!fluidStack.isEmpty()) {
					return ARGB.opaque(RenderUtil.getFluidColor(fluidStack.getFluid()));
				}
				return 0xff000000;
			}

			return 0xffffffff;
		}
	}
}
