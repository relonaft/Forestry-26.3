package forestry.core.platform.book;

import forestry.api.core.machines.IForestryRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import forestry.core.platform.registration.FeatureRecipeType;
import forestry.core.platform.util.ModUtil;
import forestry.core.platform.util.RecipeUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;

// what the carpenter and fabricator pages look up, as the Patchouli processors of Community Edition did
public final class BookRecipes {
	private BookRecipes() {
	}

	// a datapack can remove the recipe a page was written for
	@Nullable
	public static <R extends IForestryRecipe> R byOutput(@Nullable RecipeMap recipes, FeatureRecipeType<R> type, ItemStack output) {
		if (recipes == null || output.isEmpty()) {
			return null;
		}
		return RecipeUtils.getRecipes(recipes, type)
			.filter(recipe -> ItemStack.isSameItem(recipe.getResultItem(), output))
			.findFirst()
			.orElse(null);
	}

	// sand for glass, else the input of the first smelting recipe that gives the fluid
	public static List<ItemStack> meltable(@Nullable RecipeMap recipes, FluidStack molten) {
		if (ModUtil.getRegistryName(molten.getFluid()).getPath().contains("glass")) {
			return List.of(new ItemStack(Items.SAND));
		}
		if (recipes == null) {
			return List.of();
		}
		return RecipeUtils.getRecipes(recipes, FactoryRecipeTypes.FABRICATOR_SMELTING)
			.filter(smelting -> FluidStack.isSameFluidSameComponents(smelting.getResultFluid(), molten))
			.findFirst()
			.map(smelting -> stacks(smelting.getInput()))
			.orElse(List.of());
	}

	public static List<ItemStack> stacks(Ingredient ingredient) {
		return ingredient.items().map(ItemStack::new).toList();
	}
}
