package forestry.core.platform.advancements;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

// fires when a player discovers one species
public class DiscoverSpeciesTrigger extends SimpleCriterionTrigger<DiscoverSpeciesTrigger.TriggerInstance> {
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(Level level, GameProfile profile, Identifier speciesId) {
		if (level.getServer() == null) {
			return;
		}
		ServerLevel serverLevel = level.getServer().getLevel(level.dimension());
		if (serverLevel == null) {
			return;
		}

		Player player = serverLevel.getPlayerByUUID(profile.id());
		if (player instanceof ServerPlayer serverPlayer) {
			trigger(serverPlayer, instance -> instance.check(speciesId));
		}
	}

	public record TriggerInstance(Optional<Holder<LootItemCondition>> player,
								  Identifier species) implements SimpleCriterionTrigger.SimpleInstance {
		// the field keeps the name "tag" that 1.20.1 wrote it under
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Identifier.CODEC.fieldOf("tag").forGetter(TriggerInstance::species)
			)
			.apply(instance, TriggerInstance::new));

		public static Criterion<TriggerInstance> checkDiscovered(Identifier species) {
			return ForestryAdvancementTriggers.DISCOVER_SPECIES.createCriterion(new TriggerInstance(Optional.empty(), species));
		}

		public boolean check(Identifier speciesId) {
			return speciesId.equals(this.species);
		}
	}
}
