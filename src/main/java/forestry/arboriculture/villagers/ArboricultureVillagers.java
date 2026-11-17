package forestry.arboriculture.villagers;

import com.google.common.collect.ImmutableSet;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.block.NaturalistChestBlockType;
import forestry.core.features.CoreBlocks;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.platform.villager.VillagerTradeSets;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

@FeatureProvider
public class ArboricultureVillagers {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	private static final DeferredRegister<PoiType> POINTS_OF_INTEREST = REGISTRY.getRegistry(Registries.POINT_OF_INTEREST_TYPE);
	private static final DeferredRegister<VillagerProfession> PROFESSIONS = REGISTRY.getRegistry(Registries.VILLAGER_PROFESSION);

	public static final DeferredHolder<PoiType, PoiType> POI_TREE_CHEST = POINTS_OF_INTEREST.register("tree_chest", () -> new PoiType(Set.copyOf(CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.ARBORIST_CHEST).block().getStateDefinition().getPossibleStates()), 1, 1));
	public static final DeferredHolder<VillagerProfession, VillagerProfession> ARBORIST = PROFESSIONS.register("arborist", () -> {
		ResourceKey<PoiType> key = Objects.requireNonNull(POI_TREE_CHEST.getKey());
		Predicate<Holder<PoiType>> jobSitePredicate = poi -> poi.is(key);
		// the key that Villager.getTypeName built from the profession id until 1.21.5
		return new VillagerProfession(Component.translatable("entity.minecraft.villager.forestry.arborist"), jobSitePredicate, jobSitePredicate, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_FISHERMAN, VillagerTradeSets.levels("arborist", 4));
	});
}
