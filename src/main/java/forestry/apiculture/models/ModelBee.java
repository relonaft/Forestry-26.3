package forestry.apiculture.models;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.apiculture.IBeeClientManager;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// The item model for a bee, which supports custom bee textures
// Only supports the four life stages used in base Forestry (DRONE, LARVAE, PRINCESS, QUEEN)
// If you have a custom life stage, you'll need to implement your own model code
public record ModelBee(BeeLifeStage stage, List<ItemTintSource> tints) implements ItemModel.Unbaked {
	private static final Codec<BeeLifeStage> STAGE_CODEC = Codec.STRING.xmap(
		name -> BeeLifeStage.valueOf(name.toUpperCase(Locale.ENGLISH)),
		stage -> stage.name().toLowerCase(Locale.ENGLISH)
	);
	public static final MapCodec<ModelBee> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		STAGE_CODEC.fieldOf("stage").forGetter(ModelBee::stage),
		ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(ModelBee::tints)
	).apply(instance, ModelBee::new));

	@Override
	public void resolveDependencies(Resolver resolver) {
		for (Identifier location : IForestryClientApi.INSTANCE.getBeeManager().getAllModelLocations(this.stage)) {
			resolver.markDependency(location);
		}
	}

	@Override
	public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
		IBeeClientManager manager = IForestryClientApi.INSTANCE.getBeeManager();
		Map<Identifier, ItemModel> bakedModels = new HashMap<>();

		for (Identifier location : manager.getAllModelLocations(this.stage)) {
			bakedModels.put(location, new CuboidItemModelWrapper.Unbaked(location, Optional.empty(), this.tints).bake(context, transformation));
		}

		return new ModelBee.Baked(this.stage, manager, bakedModels);
	}

	@Override
	public MapCodec<ModelBee> type() {
		return MAP_CODEC;
	}

	private static class Baked implements ItemModel {
		private final ILifeStage stage;
		private final IBeeClientManager manager;
		private final Map<Identifier, ItemModel> bakedModels;

		public Baked(ILifeStage stage, IBeeClientManager manager, Map<Identifier, ItemModel> bakedModels) {
			this.stage = stage;
			this.manager = manager;
			this.bakedModels = bakedModels;
		}

		@Override
		public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
			IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
			if (individual == null) {
				// no layers, as before: a bee item without an individual is not drawn
				return;
			}

			Identifier speciesId = individual.getSpecies().id();
			Identifier location = this.manager.getModelLocation(this.stage, speciesId);
			ItemModel resolved = this.bakedModels.get(location);

			if (resolved == null) {
				// Fall back to the default model for this life stage
				resolved = this.bakedModels.get(this.manager.getDefaultModelLocation(this.stage));
			}
			if (resolved != null) {
				resolved.update(output, stack, resolver, displayContext, level, owner, seed);
			}
		}
	}
}
