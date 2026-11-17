package forestry.arboriculture.models;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.arboriculture.ITreeClientManager;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.arboriculture.sapling.SaplingBlockEntity;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// the sapling block and the sapling item, which both take their model from the tree species
public final class SaplingModel {
	private SaplingModel() {
	}

	// the block state model of the sapling block
	public static final class Unbaked implements CustomUnbakedBlockStateModel {
		public static final Unbaked INSTANCE = new Unbaked();
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

		private Unbaked() {
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			ITreeClientManager treeManager = IForestryClientApi.INSTANCE.getTreeManager();

			for (Pair<Identifier, Identifier> pair : treeManager.getAllSaplingModels()) {
				resolver.markDependency(pair.getFirst());
			}
			// ensure the default pair is baked even if no species registered a model
			resolver.markDependency(treeManager.getDefaultSaplingModels().getFirst());
		}

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			ITreeClientManager treeManager = IForestryClientApi.INSTANCE.getTreeManager();
			Map<Identifier, BlockStateModelPart> baked = new HashMap<>();

			for (Pair<Identifier, Identifier> pair : treeManager.getAllSaplingModels()) {
				bakeInto(baked, baker, pair.getFirst());
			}
			bakeInto(baked, baker, treeManager.getDefaultSaplingModels().getFirst());

			return new Baked(treeManager, baked);
		}

		private static void bakeInto(Map<Identifier, BlockStateModelPart> baked, ModelBaker baker, Identifier location) {
			if (!baked.containsKey(location)) {
				baked.put(location, SimpleModelWrapper.bake(baker, location, BlockModelRotation.IDENTITY));
			}
		}

		@Override
		public MapCodec<Unbaked> codec() {
			return MAP_CODEC;
		}
	}

	public static class Baked implements DynamicBlockStateModel {
		private final ITreeClientManager treeManager;
		private final Map<Identifier, BlockStateModelPart> baked;

		public Baked(ITreeClientManager treeManager, Map<Identifier, BlockStateModelPart> baked) {
			this.treeManager = treeManager;
			this.baked = baked;
		}

		private BlockStateModelPart blockModelFor(@Nullable ITreeSpecies species) {
			Pair<Identifier, Identifier> pair = species != null ? this.treeManager.getSaplingModels(species) : this.treeManager.getDefaultSaplingModels();
			BlockStateModelPart model = this.baked.get(pair.getFirst());
			return model != null ? model : this.baked.get(this.treeManager.getDefaultSaplingModels().getFirst());
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
			parts.add(blockModelFor(level.getModelData(pos).get(SaplingBlockEntity.TREE_SPECIES)));
		}

		@Override
		public Material.Baked particleMaterial() {
			return blockModelFor(null).particleMaterial();
		}

		@Override
		public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			return blockModelFor(level.getModelData(pos).get(SaplingBlockEntity.TREE_SPECIES)).particleMaterial();
		}

		@Override
		public int materialFlags() {
			return blockModelFor(null).materialFlags();
		}

		@Override
		public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			return blockModelFor(level.getModelData(pos).get(SaplingBlockEntity.TREE_SPECIES)).materialFlags();
		}
	}

	// the item model of the sapling item
	public record ItemUnbaked(List<ItemTintSource> tints) implements ItemModel.Unbaked {
		public static final MapCodec<ItemUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(ItemUnbaked::tints)
		).apply(instance, ItemUnbaked::new));

		@Override
		public void resolveDependencies(Resolver resolver) {
			ITreeClientManager treeManager = IForestryClientApi.INSTANCE.getTreeManager();

			for (Pair<Identifier, Identifier> pair : treeManager.getAllSaplingModels()) {
				resolver.markDependency(pair.getSecond());
			}
			resolver.markDependency(treeManager.getDefaultSaplingModels().getSecond());
		}

		@Override
		public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
			ITreeClientManager treeManager = IForestryClientApi.INSTANCE.getTreeManager();
			Map<Identifier, ItemModel> baked = new HashMap<>();

			for (Pair<Identifier, Identifier> pair : treeManager.getAllSaplingModels()) {
				bakeInto(baked, context, transformation, pair.getSecond());
			}
			bakeInto(baked, context, transformation, treeManager.getDefaultSaplingModels().getSecond());

			return new ItemBaked(treeManager, baked);
		}

		private void bakeInto(Map<Identifier, ItemModel> baked, ItemModel.BakingContext context, Matrix4fc transformation, Identifier location) {
			if (!baked.containsKey(location)) {
				baked.put(location, new CuboidItemModelWrapper.Unbaked(location, Optional.empty(), this.tints).bake(context, transformation));
			}
		}

		@Override
		public MapCodec<ItemUnbaked> type() {
			return MAP_CODEC;
		}
	}

	public static class ItemBaked implements ItemModel {
		private final ITreeClientManager treeManager;
		private final Map<Identifier, ItemModel> baked;

		public ItemBaked(ITreeClientManager treeManager, Map<Identifier, ItemModel> baked) {
			this.treeManager = treeManager;
			this.baked = baked;
		}

		private ItemModel itemModelFor(@Nullable ITreeSpecies species) {
			Pair<Identifier, Identifier> pair = species != null ? this.treeManager.getSaplingModels(species) : this.treeManager.getDefaultSaplingModels();
			ItemModel model = this.baked.get(pair.getSecond());
			return model != null ? model : this.baked.get(this.treeManager.getDefaultSaplingModels().getSecond());
		}

		@Override
		public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
			IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
			// deviation: a stack without an individual showed the default block model, it shows the default item model now
			ITreeSpecies species = individual != null ? (ITreeSpecies) individual.getSpecies() : null;

			itemModelFor(species).update(output, stack, resolver, displayContext, level, owner, seed);
		}
	}
}
