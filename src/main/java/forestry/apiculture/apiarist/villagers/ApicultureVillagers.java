package forestry.apiculture.apiarist.villagers;

import com.google.common.collect.ImmutableSet;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.block.CoreMachineBlockType;
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
public class ApicultureVillagers {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	private static final DeferredRegister<PoiType> POINTS_OF_INTEREST = REGISTRY.getRegistry(Registries.POINT_OF_INTEREST_TYPE);
	private static final DeferredRegister<VillagerProfession> PROFESSIONS = REGISTRY.getRegistry(Registries.VILLAGER_PROFESSION);

	public static final DeferredHolder<PoiType, PoiType> POI_ESCRITOIRE = POINTS_OF_INTEREST.register("escritoire", () -> new PoiType(Set.copyOf(CoreBlocks.BASE.get(CoreMachineBlockType.ESCRITOIRE).block().getStateDefinition().getPossibleStates()), 1, 1));
	public static final DeferredHolder<VillagerProfession, VillagerProfession> PROF_BEEKEEPER = PROFESSIONS.register("beekeeper", () -> {
		ResourceKey<PoiType> key = Objects.requireNonNull(POI_ESCRITOIRE.getKey());
		Predicate<Holder<PoiType>> jobSitePredicate = e -> e.is(key);
		// the key that Villager.getTypeName built from the profession id until 1.21.5
		return new VillagerProfession(Component.translatable("entity.minecraft.villager.forestry.beekeeper"), jobSitePredicate, jobSitePredicate, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LIBRARIAN, VillagerTradeSets.levels("beekeeper", 4));
	});
}
