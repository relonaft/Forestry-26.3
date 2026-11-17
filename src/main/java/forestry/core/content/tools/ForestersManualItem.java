package forestry.core.content.tools;

import forestry.core.platform.registration.PendingId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ForestersManualItem extends Item {
	public ForestersManualItem() {
		super(PendingId.item(new Item.Properties().stacksTo(1)));
	}

	// todo: Patchouli has no 26.3 build, the manual opens nothing until the built-in book exists
	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}
}
