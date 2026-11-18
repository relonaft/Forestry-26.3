package forestry.core.content.tools;

import forestry.core.platform.client.ClientsideCode;
import forestry.core.platform.registration.PendingId;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ForestersManualItem extends Item {
	public ForestersManualItem() {
		super(PendingId.item(new Item.Properties().stacksTo(1)));
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		// deviation: Patchouli has no 26.3 build, the book is a screen of this mod that the client opens itself
		if (world.isClientSide()) {
			ClientsideCode.openForestersManual();
		}
		// the server plays it to the players nearby, the client to the reader
		player.playSound(SoundEvents.BOOK_PAGE_TURN, 1F, (float) (0.7 + Math.random() * 0.4));

		return InteractionResult.SUCCESS;
	}
}
