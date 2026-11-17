package forestry.core.content.machines.recipes;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.ICarpenterRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.Optional;

public class CarpenterRecipe implements ICarpenterRecipe {
	// Inner crafting recipe is either Shaped or Shapeless; dispatch via Either so
	// existing data (e.g. wood pulp, bronze ingot) using shapeless can persist.
	private static final Codec<CraftingRecipe> CRAFTING_RECIPE_CODEC = Codec.either(
		ShapedRecipe.MAP_CODEC.codec(),
		ShapelessRecipe.MAP_CODEC.codec()
	).xmap(
		either -> either.map(sr -> (CraftingRecipe) sr, slr -> (CraftingRecipe) slr),
		cr -> {
			if (cr instanceof ShapedRecipe sr) return Either.left(sr);
			if (cr instanceof ShapelessRecipe slr) return Either.right(slr);
			throw new IllegalStateException("Unsupported CraftingRecipe type for CarpenterRecipe: " + cr.getClass().getName());
		}
	);

	private static final MapCodec<CarpenterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(CarpenterRecipe::getId),
		Codec.INT.fieldOf("time").forGetter(CarpenterRecipe::getPackagingTime),
		ExtraCodecs.optionalEmptyMap(FluidStackTemplate.CODEC).optionalFieldOf("liquid", Optional.empty()).forGetter(recipe -> recipe.liquid),
		Ingredient.CODEC.optionalFieldOf("box").forGetter(CarpenterRecipe::getBox),
		CRAFTING_RECIPE_CODEC.fieldOf("recipe").forGetter(CarpenterRecipe::getCraftingGridRecipe),
		ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(recipe -> recipe.result)
	).apply(instance, CarpenterRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, CarpenterRecipe> STREAM_CODEC = StreamCodec.of(
		CarpenterRecipe::toNetwork,
		CarpenterRecipe::fromNetwork
	);
	public static final RecipeSerializer<CarpenterRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private final Identifier id;
	private final int packagingTime;
	private final Optional<FluidStackTemplate> liquid;
	private final Optional<Ingredient> box;
	private final CraftingRecipe recipe;
	private final Optional<ItemStackTemplate> result;

	public CarpenterRecipe(Identifier id, int packagingTime, Optional<FluidStackTemplate> liquid, Optional<Ingredient> box, CraftingRecipe recipe, Optional<ItemStackTemplate> result) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(liquid, "Recipe liquid cannot be null; use an empty optional for no fluid input");
		Preconditions.checkNotNull(box);
		Preconditions.checkNotNull(recipe);
		if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) {
			throw new IllegalArgumentException("CarpenterRecipe inner recipe must be ShapedRecipe or ShapelessRecipe; got " + recipe.getClass().getName());
		}

		this.id = id;
		this.packagingTime = packagingTime;
		this.liquid = liquid;
		this.box = box;
		this.recipe = recipe;
		this.result = result;
	}

	@Override
	public int getPackagingTime() {
		return this.packagingTime;
	}

	@Override
	public Optional<Ingredient> getBox() {
		return this.box;
	}

	@Override
	public FluidStack getInputFluid() {
		return this.liquid.map(FluidStackTemplate::create).orElse(FluidStack.EMPTY);
	}

	@Override
	public CraftingRecipe getCraftingGridRecipe() {
		return this.recipe;
	}

	@Override
	public ItemStack getResultItem() {
		// the inner recipe has no getter for its result, and neither inner recipe class reads the input
		return this.result.map(ItemStackTemplate::create).orElseGet(() -> this.recipe.assemble(CraftingInput.EMPTY));
	}

	@Override
	public boolean matches(FluidStack fluid, ItemStack boxStack, Container craftingInventory, Level level) {
		if (this.liquid.isPresent()) {
			FluidStackTemplate liquid = this.liquid.get();
			if (fluid.isEmpty() || !(FluidStack.isSameFluidSameComponents(fluid, liquid) && fluid.getAmount() >= liquid.amount())) {
				return false;
			}
		}

		if (this.box.isPresent() && !this.box.get().test(boxStack)) {
			return false;
		}

		return this.recipe.matches(FakeCraftingInventory.of(craftingInventory), level);
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<CarpenterRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public RecipeType<ICarpenterRecipe> getType() {
		return FactoryRecipeTypes.CARPENTER.type();
	}

	private static CarpenterRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
		Identifier recipeId = Identifier.STREAM_CODEC.decode(buffer);
		int packagingTime = ByteBufCodecs.VAR_INT.decode(buffer);
		Optional<FluidStackTemplate> liquid = buffer.readBoolean() ? Optional.of(FluidStackTemplate.STREAM_CODEC.decode(buffer)) : Optional.empty();
		Optional<Ingredient> box = Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buffer);
		CraftingRecipe internal = buffer.readBoolean()
			? ShapedRecipe.STREAM_CODEC.decode(buffer)
			: ShapelessRecipe.STREAM_CODEC.decode(buffer);
		Optional<ItemStackTemplate> result = buffer.readBoolean() ? Optional.of(ItemStackTemplate.STREAM_CODEC.decode(buffer)) : Optional.empty();

		return new CarpenterRecipe(recipeId, packagingTime, liquid, box, internal, result);
	}

	private static void toNetwork(RegistryFriendlyByteBuf buffer, CarpenterRecipe recipe) {
		Identifier.STREAM_CODEC.encode(buffer, recipe.id);
		ByteBufCodecs.VAR_INT.encode(buffer, recipe.packagingTime);
		buffer.writeBoolean(recipe.liquid.isPresent());
		if (recipe.liquid.isPresent()) {
			FluidStackTemplate.STREAM_CODEC.encode(buffer, recipe.liquid.get());
		}
		Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buffer, recipe.box);
		if (recipe.recipe instanceof ShapedRecipe shaped) {
			buffer.writeBoolean(true);
			ShapedRecipe.STREAM_CODEC.encode(buffer, shaped);
		} else if (recipe.recipe instanceof ShapelessRecipe shapeless) {
			buffer.writeBoolean(false);
			ShapelessRecipe.STREAM_CODEC.encode(buffer, shapeless);
		} else {
			throw new IllegalStateException("Unsupported CraftingRecipe type for CarpenterRecipe network sync: " + recipe.recipe.getClass().getName());
		}

		buffer.writeBoolean(recipe.result.isPresent());
		if (recipe.result.isPresent()) {
			ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result.get());
		}
	}
}
