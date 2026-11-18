package forestry.core.platform.client.book;

import forestry.api.ForestryConstants;
import forestry.api.core.machines.ICarpenterRecipe;
import forestry.api.core.machines.IFabricatorRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import forestry.core.platform.book.BookRecipes;
import forestry.core.platform.util.ModUtil;
import forestry.core.platform.util.RecipeUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

// the recipe drawings, each a region of crafting.png with the items of one recipe on it
final class RecipePages {
	private static final Identifier TEXTURE = ForestryConstants.forestry("textures/gui/almanac/crafting.png");
	static final int HEIGHT = 60;
	// the grid, the arrow and the result of the carpenter drawing, without its tank
	static final int CRAFTING_WIDTH = 88;
	private static final int MACHINE_WIDTH = 108;
	private static final int SLOT = 19;

	private RecipePages() {
	}

	static ImageElement slot(int x, int y) {
		return new ImageElement(TEXTURE, x, y, 0, 2, 18, 18);
	}

	// returns the result, or nothing when the client has no such crafting recipe
	static ItemStack crafting(Font font, BookPage page, int x, int y, String recipeId) {
		Identifier id = Identifier.tryParse(recipeId);
		RecipeMap recipes = RecipeUtils.getRecipeMap();
		RecipeHolder<?> holder = id == null || recipes == null ? null : recipes.byKey(ResourceKey.create(Registries.RECIPE, id));
		if (holder == null || !(holder.value() instanceof CraftingRecipe recipe)) {
			missing(font, page, x, y, CRAFTING_WIDTH, recipeId);
			return ItemStack.EMPTY;
		}
		ItemStack result = recipe.assemble(CraftingInput.EMPTY);
		page.add(new ImageElement(TEXTURE, x, y, 0, 0, CRAFTING_WIDTH, HEIGHT));
		grid(page, x + 1, y + 3, recipe);
		page.add(new ItemElement(x + 71, y + 41, List.of(result)));
		return result;
	}

	// the positions come from templates/carpenter/base.json
	static void carpenter(Font font, BookPage page, int y, ItemStack output) {
		ICarpenterRecipe recipe = BookRecipes.byOutput(RecipeUtils.getRecipeMap(), FactoryRecipeTypes.CARPENTER, output);
		page.add(LabelElement.header(output.getHoverName(), y - 10));
		if (recipe == null) {
			missing(font, page, 0, y, MACHINE_WIDTH, ModUtil.getRegistryName(output.getItem()).toString());
			return;
		}
		page.add(new ImageElement(TEXTURE, 0, y, 0, 0, MACHINE_WIDTH, HEIGHT));
		grid(page, 1, y + 3, recipe.getCraftingGridRecipe());
		page.add(new ItemElement(71, y + 41, List.of(recipe.getResultItem())));
		page.add(new FluidElement(91, y + 1, 58, recipe.getInputFluid(), 10000));
		page.add(new ImageElement(TEXTURE, 90, y, 108, 0, 18, HEIGHT));
	}

	// the positions come from templates/fabricator/base.json
	static void fabricator(Font font, BookPage page, int y, ItemStack output) {
		IFabricatorRecipe recipe = BookRecipes.byOutput(RecipeUtils.getRecipeMap(), FactoryRecipeTypes.FABRICATOR, output);
		page.add(LabelElement.header(output.getHoverName(), y - 10));
		if (recipe == null) {
			missing(font, page, 0, y, MACHINE_WIDTH, ModUtil.getRegistryName(output.getItem()).toString());
			return;
		}
		FluidStack molten = recipe.getResultFluid();
		page.add(new ImageElement(TEXTURE, 0, y, 0, 60, MACHINE_WIDTH, HEIGHT));
		grid(page, 21, y + 1, recipe.getCraftingGridRecipe());
		// quirk kept: the template puts the plan at 78, its frame in the drawing is at 91
		recipe.getPlan().ifPresent(plan -> page.add(new ItemElement(78, y + 1, BookRecipes.stacks(plan))));
		page.add(new ItemElement(1, y + 6, BookRecipes.meltable(RecipeUtils.getRecipeMap(), molten)));
		page.add(new ItemElement(91, y + 39, List.of(recipe.getResultItem())));
		page.add(new FluidElement(1, y + 33, 16, molten, 2000));
		page.add(new ImageElement(TEXTURE, 0, y + 32, 108, 60, 18, 18));
	}

	// deviation: CE filled the slots in list order, so a recipe narrower than three lost its shape
	private static void grid(BookPage page, int x, int y, CraftingRecipe recipe) {
		List<Optional<Ingredient>> ingredients;
		int width = 3;
		if (recipe instanceof ShapedRecipe shaped) {
			ingredients = shaped.getIngredients();
			width = shaped.getWidth();
		} else {
			ingredients = recipe.placementInfo().ingredients().stream().map(Optional::of).toList();
		}
		for (int i = 0; i < ingredients.size() && i / width < 3; i++) {
			int slotX = x + i % width * SLOT;
			int slotY = y + i / width * SLOT;
			ingredients.get(i).ifPresent(ingredient -> page.add(new ItemElement(slotX, slotY, BookRecipes.stacks(ingredient))));
		}
	}

	// deviation: a missing recipe is red text on the page, in Patchouli its place stays empty
	private static void missing(Font font, BookPage page, int x, int y, int width, String name) {
		BookClient.warnOnce("Book recipe not found: " + name);
		page.add(TextElement.error(font, Component.translatable("for.gui.book.error.recipe", name), x, y, width));
	}
}
