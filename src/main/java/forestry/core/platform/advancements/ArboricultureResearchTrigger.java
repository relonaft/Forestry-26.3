package forestry.core.platform.advancements;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

// fires when a player's share of discovered tree species changes
public class ArboricultureResearchTrigger extends SimpleCriterionTrigger<ArboricultureResearchTrigger.TriggerInstance> {
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(Level level, GameProfile profile, double researchCompletion) {
		if (level.getServer() == null) {
			return;
		}
		ServerLevel serverLevel = level.getServer().getLevel(level.dimension());
		if (serverLevel == null) {
			return;
		}

		Player player = serverLevel.getPlayerByUUID(profile.id());
		if (player instanceof ServerPlayer serverPlayer) {
			trigger(serverPlayer, instance -> instance.check(researchCompletion));
		}
	}

	public record TriggerInstance(Optional<Holder<LootItemCondition>> player,
								  double percentage) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Codec.DOUBLE.fieldOf("percent").forGetter(TriggerInstance::percentage)
			)
			.apply(instance, TriggerInstance::new));

		public static Criterion<TriggerInstance> checkIfResearchIsGreaterThan(double percentage) {
			return ForestryAdvancementTriggers.ARBORICULTURE_RESEARCH.createCriterion(new TriggerInstance(Optional.empty(), percentage));
		}

		public boolean check(double amount) {
			return amount >= this.percentage;
		}
	}
}
