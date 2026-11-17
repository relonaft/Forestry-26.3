package forestry.core.platform.advancements;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

// grants advancements built on minecraft:impossible, which have no criterion that can fire
public class AdvancementHelper {
	public static void tryUnlock(Player player, Identifier id) {
		if (player instanceof ServerPlayer serverPlayer) {
			AdvancementHolder advancement = serverPlayer.level().getServer().getAdvancements().get(id);

			if (advancement != null) {
				AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);

				for (String criterion : progress.getRemainingCriteria()) {
					serverPlayer.getAdvancements().award(advancement, criterion);
				}
			}
		}
	}
}
