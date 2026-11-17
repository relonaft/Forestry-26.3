package forestry.core.platform.crafting;

import forestry.api.ForestryConstants;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ForestryIngredientTypes {
	public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, ForestryConstants.MOD_ID);

	public static final DeferredHolder<IngredientType<?>, IngredientType<FluidContainerIngredient>> FLUID_CONTAINER =
		INGREDIENT_TYPES.register("fluid_container", () -> new IngredientType<>(FluidContainerIngredient.CODEC));

	private ForestryIngredientTypes() {
	}
}
