package forestry.core.platform.loot;

import com.mojang.serialization.MapCodec;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@FeatureProvider
public class CoreLootFunctions {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
	private static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS = REGISTRY.getRegistry(Registries.LOOT_FUNCTION_TYPE);

	public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<OrganismFunction>> ORGANISM = LOOT_FUNCTIONS.register("set_species_nbt", () -> OrganismFunction.CODEC);
	public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<RandomOrganismFunction>> RANDOM_ORGANISM = LOOT_FUNCTIONS.register("random_organism", () -> RandomOrganismFunction.CODEC);
	public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<RandomItemFunction>> RANDOM_ITEM = LOOT_FUNCTIONS.register("random_item", () -> RandomItemFunction.CODEC);

	private static final DeferredRegister<MapCodec<? extends LootItemCondition>> LOOT_CONDITIONS = REGISTRY.getRegistry(Registries.LOOT_CONDITION_TYPE);
	public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<TraderChoiceCondition>> TRADER_CHOICE = LOOT_CONDITIONS.register("trader_choice", () -> TraderChoiceCondition.CODEC);
}
