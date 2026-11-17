package forestry.core.platform.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.IForestryApi;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// OrganismFunction with a random species, from a list or from all species up to a complexity
public class RandomOrganismFunction extends LootItemConditionalFunction {
	public static final MapCodec<RandomOrganismFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.and(instance.group(
			Identifier.CODEC.fieldOf("type_id").forGetter(function -> function.typeId),
			Identifier.CODEC.listOf().optionalFieldOf("species").forGetter(function -> function.species),
			Codec.INT.optionalFieldOf("max_complexity").forGetter(function -> function.maxComplexity)
		))
		.apply(instance, RandomOrganismFunction::new));

	private final Identifier typeId;
	private final Optional<List<Identifier>> species;
	private final Optional<Integer> maxComplexity;

	private RandomOrganismFunction(Optional<Holder<LootItemCondition>> condition, Identifier typeId, Optional<List<Identifier>> species, Optional<Integer> maxComplexity) {
		super(condition);
		this.typeId = typeId;
		this.species = species;
		this.maxComplexity = maxComplexity;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		ISpeciesType<?, ?> speciesType = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(this.typeId);
		ILifeStage stage = speciesType.getLifeStage(stack);

		if (stage == null) {
			return stack;
		}

		ArrayList<ISpecies<?>> candidates = new ArrayList<>();
		if (this.species.isPresent()) {
			for (Identifier id : this.species.get()) {
				candidates.add(speciesType.getSpecies(id));
			}
		} else {
			candidates.addAll(speciesType.getAllSpecies());
		}
		if (this.maxComplexity.isPresent()) {
			int max = this.maxComplexity.get();
			candidates.removeIf(candidate -> candidate.getComplexity() > max);
		}

		// an empty stack makes the villager trade unavailable
		if (candidates.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return candidates.get(context.getRandom().nextInt(candidates.size())).createStack(stage);
	}

	@Override
	public MapCodec<RandomOrganismFunction> codec() {
		return CODEC;
	}
}
