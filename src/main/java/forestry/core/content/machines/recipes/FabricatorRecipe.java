package forestry.core.content.machines.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.IFabricatorRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

public class FabricatorRecipe implements IFabricatorRecipe {

	private static final MapCodec<FabricatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(FabricatorRecipe::getId),
		// "plan" is optional: many fabricator recipes (e.g. electron tubes) require no plan.
		Ingredient.CODEC.optionalFieldOf("plan").forGetter(FabricatorRecipe::getPlan),
		FluidStackTemplate.CODEC.fieldOf("molten").forGetter(recipe -> recipe.resultFluid),
		ShapedRecipe.MAP_CODEC.codec().fieldOf("recipe").forGetter(FabricatorRecipe::getCraftingGridRecipe)
	).apply(instance, FabricatorRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, FabricatorRecipe> STREAM_CODEC = StreamCodec.of(
		FabricatorRecipe::toNetwork,
		FabricatorRecipe::fromNetwork
	);
	public static final RecipeSerializer<FabricatorRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private final Identifier id;
	private final Optional<Ingredient> plan;
	private final FluidStackTemplate resultFluid;
	private final ShapedRecipe recipe;

	public FabricatorRecipe(Identifier id, Optional<Ingredient> plan, FluidStack resultFluid, ShapedRecipe recipe) {
		this(id, plan, FluidStackTemplate.fromNonEmptyStack(resultFluid), recipe);
	}

	// holds templates, a stack cannot be built while the datapack loads
	public FabricatorRecipe(Identifier id, Optional<Ingredient> plan, FluidStackTemplate resultFluid, ShapedRecipe recipe) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(plan);
		Preconditions.checkNotNull(resultFluid);

		this.id = id;
		this.plan = plan;
		this.resultFluid = resultFluid;
		this.recipe = recipe;
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	@Override
	public Optional<Ingredient> getPlan() {
		return this.plan;
	}

	@Override
	public FluidStack getResultFluid() {
		return this.resultFluid.create();
	}

	@Override
	public ShapedRecipe getCraftingGridRecipe() {
		return this.recipe;
	}

	@Override
	public boolean matches(Level level, FluidStack liquid, ItemStack stack, Container inventory) {
		return FluidStack.isSameFluidSameComponents(liquid, this.resultFluid.create()) && liquid.getAmount() >= this.resultFluid.amount() && Ingredient.testOptionalIngredient(this.plan, stack) && this.recipe.matches(FakeCraftingInventory.of(inventory), level);
	}

	@Override
	public ItemStack getResultItem() {
		// the shaped recipe has no getter for its result and does not read the input
		return this.recipe.assemble(CraftingInput.EMPTY);
	}

	@Override
	public RecipeSerializer<FabricatorRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public RecipeType<IFabricatorRecipe> getType() {
		return FactoryRecipeTypes.FABRICATOR.type();
	}

	private static FabricatorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
		Identifier id = Identifier.STREAM_CODEC.decode(buffer);
		Optional<Ingredient> plan = Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buffer);
		FluidStackTemplate molten = FluidStackTemplate.STREAM_CODEC.decode(buffer);
		ShapedRecipe shapedRecipe = ShapedRecipe.STREAM_CODEC.decode(buffer);

		return new FabricatorRecipe(id, plan, molten, shapedRecipe);
	}

	private static void toNetwork(RegistryFriendlyByteBuf buffer, FabricatorRecipe recipe) {
		Identifier.STREAM_CODEC.encode(buffer, recipe.id);
		Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buffer, recipe.plan);
		FluidStackTemplate.STREAM_CODEC.encode(buffer, recipe.resultFluid);
		ShapedRecipe.STREAM_CODEC.encode(buffer, recipe.recipe);
	}
}
