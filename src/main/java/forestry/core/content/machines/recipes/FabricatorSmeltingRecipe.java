package forestry.core.content.machines.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.IFabricatorSmeltingRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

public class FabricatorSmeltingRecipe implements IFabricatorSmeltingRecipe {
	private static final MapCodec<FabricatorSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(FabricatorSmeltingRecipe::getId),
		Ingredient.CODEC.fieldOf("resource").forGetter(FabricatorSmeltingRecipe::getInput),
		FluidStackTemplate.CODEC.fieldOf("product").forGetter(recipe -> recipe.product),
		Codec.INT.fieldOf("melting").forGetter(FabricatorSmeltingRecipe::getMeltingPoint)
	).apply(instance, FabricatorSmeltingRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, FabricatorSmeltingRecipe> STREAM_CODEC = StreamCodec.of(
		FabricatorSmeltingRecipe::toNetwork,
		FabricatorSmeltingRecipe::fromNetwork
	);
	public static final RecipeSerializer<FabricatorSmeltingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private final Identifier id;
	private final Ingredient resource;
	private final FluidStackTemplate product;
	private final int meltingPoint;

	public FabricatorSmeltingRecipe(Identifier id, Ingredient resource, FluidStack molten, int meltingPoint) {
		this(id, resource, FluidStackTemplate.fromNonEmptyStack(molten), meltingPoint);
	}

	// holds templates, a stack cannot be built while the datapack loads
	public FabricatorSmeltingRecipe(Identifier id, Ingredient resource, FluidStackTemplate molten, int meltingPoint) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(resource);
		// no emptiness check: the codec refuses an empty ingredient and a tag has no contents while the datapack loads
		Preconditions.checkNotNull(molten);

		this.id = id;
		this.resource = resource;
		this.product = molten;
		this.meltingPoint = meltingPoint;
	}

	@Override
	public Ingredient getInput() {
		return this.resource;
	}

	@Override
	public FluidStack getResultFluid() {
		return this.product.create();
	}

	@Override
	public int getMeltingPoint() {
		return this.meltingPoint;
	}

	@Override
	public ItemStack getResultItem() {
		return ItemStack.EMPTY;
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<FabricatorSmeltingRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public RecipeType<IFabricatorSmeltingRecipe> getType() {
		return FactoryRecipeTypes.FABRICATOR_SMELTING.type();
	}

	private static FabricatorSmeltingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
		Identifier recipeId = Identifier.STREAM_CODEC.decode(buffer);
		Ingredient resource = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
		FluidStackTemplate product = FluidStackTemplate.STREAM_CODEC.decode(buffer);
		int meltingPoint = ByteBufCodecs.VAR_INT.decode(buffer);

		return new FabricatorSmeltingRecipe(recipeId, resource, product, meltingPoint);
	}

	private static void toNetwork(RegistryFriendlyByteBuf buffer, FabricatorSmeltingRecipe recipe) {
		Identifier.STREAM_CODEC.encode(buffer, recipe.id);
		Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.resource);
		FluidStackTemplate.STREAM_CODEC.encode(buffer, recipe.product);
		ByteBufCodecs.VAR_INT.encode(buffer, recipe.meltingPoint);
	}
}
