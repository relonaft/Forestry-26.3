package forestry.arboriculture.wood;

import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.core.platform.registration.PendingId;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;

public class ForestryHangingSignBlockItem extends HangingSignItem {
	public ForestryHangingSignBlockItem(ForestryHangingSignBlock block, Item.Properties properties) {
		super(block, ArboricultureBlocks.WALL_HANGING_SIGN.get(block.getWoodType()).block(), PendingId.blockItem(block, properties.signText()));
	}
}
