package forestry.core.platform.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Set;

// passes for exactly one choice per entity, so a villager offers one trade out of a group of variants
public record TraderChoiceCondition(int choice, int choices) implements LootItemCondition {
	public static final MapCodec<TraderChoiceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ExtraCodecs.NON_NEGATIVE_INT.fieldOf("choice").forGetter(TraderChoiceCondition::choice),
		ExtraCodecs.POSITIVE_INT.fieldOf("choices").forGetter(TraderChoiceCondition::choices)
	).apply(instance, TraderChoiceCondition::new));

	@Override
	public boolean test(LootContext context) {
		Entity trader = context.getOptional(LootContextParams.THIS_ENTITY);
		return trader != null && Math.floorMod(trader.getUUID().hashCode(), this.choices) == this.choice;
	}

	@Override
	public Set<ContextKey<?>> getReferencedContextParams() {
		return Set.of(LootContextParams.THIS_ENTITY);
	}

	@Override
	public MapCodec<TraderChoiceCondition> codec() {
		return CODEC;
	}
}
