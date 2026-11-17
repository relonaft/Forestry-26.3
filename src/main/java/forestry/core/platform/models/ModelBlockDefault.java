package forestry.core.platform.models;

import com.google.common.base.Preconditions;
import forestry.core.platform.models.baker.ModelBaker;
import forestry.core.platform.models.baker.ModelBakerModel;
import forestry.core.platform.util.ResourceUtil;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.List;

// one instance is both the block model and the item model of the blocks it was registered for
public abstract class ModelBlockDefault<B extends Block, K> implements DynamicBlockStateModel, ItemModel {
	private static final Matrix4fc NO_TRANSFORM = new Matrix4f();

	protected final Class<B> blockClass;

	@Nullable
	protected ModelBakerModel blockModel;
	@Nullable
	protected ModelBakerModel itemModel;

	protected ModelBlockDefault(Class<B> blockClass) {
		this.blockClass = blockClass;
	}

	protected ModelBakerModel bakeModel(BlockState state, K key, B block, ModelData extraData) {
		ModelBaker baker = new ModelBaker();

		bakeBlock(block, extraData, key, baker, false);

		// return the local, not the field: another worker thread may overwrite blockModel mid-bake
		ModelBakerModel model = baker.bake(false);
		onCreateModel(model);
		this.blockModel = model;
		return model;
	}

	protected ModelBakerModel getModel(BlockState state, ModelData extraData) {
		Preconditions.checkArgument(this.blockClass.isInstance(state.getBlock()));

		K worldKey = getWorldKey(state, extraData);
		B block = this.blockClass.cast(state.getBlock());
		return bakeModel(state, worldKey, block, extraData);
	}

	protected ModelBakerModel bakeModel(ItemStack stack, @Nullable Level world, K key) {
		ModelBaker baker = new ModelBaker();
		Block block = Block.byItem(stack.getItem());
		Preconditions.checkArgument(this.blockClass.isInstance(block));
		B bBlock = this.blockClass.cast(block);
		bakeBlock(bBlock, ModelData.EMPTY, key, baker, true);

		return this.itemModel = baker.bake(true);
	}

	protected ModelBakerModel getModel(ItemStack stack, @Nullable Level world) {
		return bakeModel(stack, world, getInventoryKey(stack));
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
		// the context-free overload of the vanilla interface passes air
		if (this.blockClass.isInstance(state.getBlock())) {
			parts.add(getModel(state, level.getModelData(pos)));
		}
	}

	protected void onCreateModel(ModelBakerModel model) {
		model.setAmbientOcclusion(true);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (!this.blockClass.isInstance(state.getBlock())) {
			return particleMaterial();
		}
		return getModel(state, level.getModelData(pos)).particleMaterial();
	}

	// last resort for callers that have no block state: the model baked last, or the missing texture before the
	// first bake
	@Override
	public Material.Baked particleMaterial() {
		ModelBakerModel model = this.blockModel;
		if (model != null) {
			return model.particleMaterial();
		}
		return new Material.Baked(ResourceUtil.getMissingTexture(), false);
	}

	@Override
	public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (!this.blockClass.isInstance(state.getBlock())) {
			return materialFlags();
		}
		return getModel(state, level.getModelData(pos)).materialFlags();
	}

	@Override
	public int materialFlags() {
		ModelBakerModel model = this.blockModel;
		return model != null ? model.materialFlags() : 0;
	}

	@Override
	public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
		ModelBakerModel model = getModel(stack, level);

		output.appendModelIdentityElement(model);
		ItemStackRenderState.LayerRenderState layer = output.newLayer();
		if (stack.hasFoil()) {
			layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
			output.setAnimated();
			output.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
		}

		// every tint index of the model asks the item, as the shared item colour did
		IntList tintLayers = layer.tintLayers();
		for (int tintIndex = 0; tintIndex < model.tintLayers(); tintIndex++) {
			int tint = ForestryItemTint.getColor(stack, tintIndex);
			tintLayers.add(tint);
			output.appendModelIdentityElement(tint);
		}

		layer.setExtents(model.extents());
		layer.setLocalTransform(NO_TRANSFORM);
		layer.setUsesBlockLight(true);
		layer.setParticleMaterial(model.particleMaterial());
		layer.setItemTransform(ModelTransforms.BLOCK.getTransform(displayContext));
		layer.setQuads(model.itemQuads());
	}

	protected abstract K getInventoryKey(ItemStack stack);

	protected abstract K getWorldKey(BlockState state, ModelData extraData);

	protected abstract void bakeBlock(B block, ModelData extraData, K key, ModelBaker baker, boolean inventory);
}
