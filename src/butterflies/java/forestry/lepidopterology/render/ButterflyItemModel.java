package forestry.lepidopterology.render;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import forestry.api.ForestryConstants;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.lepidopterology.IButterflyClientManager;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ButterflyItemModel implements ItemModel {
	private static final Identifier BASE_MODEL = ForestryConstants.forestry("item/butterfly_base");

	// keyed by item texture location, not species, so species added by datapacks resolve through the client manager
	private final Map<Identifier, ItemModel> subModels;

	public ButterflyItemModel(Map<Identifier, ItemModel> subModels) {
		this.subModels = subModels;
	}

	private ItemModel modelFor(Identifier itemTexture) {
		ItemModel model = this.subModels.get(itemTexture);
		if (model != null) {
			return model;
		}
		// a texture that was not known at model load, ex. a datapack species added after the last resource reload
		Identifier defaultTexture = IForestryClientApi.INSTANCE.getButterflyManager().getDefaultTextures().getFirst();
		return this.subModels.get(defaultTexture);
	}

	@Override
	public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
		IIndividual individual = Objects.requireNonNull(IIndividualHandlerItem.getIndividual(stack));
		IGenome genome = individual.getGenome();
		IButterflySpecies species = genome.resolveActive(ButterflyChromosomes.SPECIES);
		Identifier itemTexture = IForestryClientApi.INSTANCE.getButterflyManager().getTextures(species).getFirst();

		// todo include scale, the size chromosome never reached the item model
		modelFor(itemTexture).update(output, stack, resolver, displayContext, level, owner, seed);
	}

	public static final class Unbaked implements ItemModel.Unbaked {
		public static final Unbaked INSTANCE = new Unbaked();
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

		private Unbaked() {
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			resolver.markDependency(BASE_MODEL);
		}

		@Override
		public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
			// built from the client-registered texture map, never the live species list, which may be empty here
			IButterflyClientManager manager = IForestryClientApi.INSTANCE.getButterflyManager();
			ModelBaker baker = context.blockModelBaker();
			ResolvedModel base = baker.getModel(BASE_MODEL);
			Map<Identifier, ItemModel> subModels = new HashMap<>();

			for (Pair<Identifier, Identifier> pair : manager.getAllTextures()) {
				bakeInto(subModels, base, baker, transformation, pair.getFirst());
			}
			// ensure the default texture is baked even if no species registered an explicit texture pair
			bakeInto(subModels, base, baker, transformation, manager.getDefaultTextures().getFirst());

			return new ButterflyItemModel(subModels);
		}

		private static void bakeInto(Map<Identifier, ItemModel> baked, ResolvedModel base, ModelBaker baker, Matrix4fc transformation, Identifier texture) {
			if (baked.containsKey(texture)) {
				return;
			}
			TextureSlots.Data override = new TextureSlots.Data.Builder().addTexture("butterfly", new Material(texture)).build();
			TextureSlots.Resolver resolver = new TextureSlots.Resolver().addLast(override);
			for (ResolvedModel current = base; current != null; current = current.parent()) {
				resolver.addLast(current.wrapped().textureSlots());
			}
			TextureSlots slots = resolver.resolve(base);
			// not bakeTopGeometry, it caches the first identity bake and ignores the texture override after it
			QuadCollection quads = base.getTopGeometry().bake(slots, baker, BlockModelRotation.IDENTITY, base, base.getTopAdditionalProperties());
			ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, base, slots);

			baked.put(texture, new CuboidItemModelWrapper(List.of(), quads, properties, transformation));
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}
}
