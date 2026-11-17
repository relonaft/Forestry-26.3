package forestry.arboriculture.wood;

import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.core.platform.registration.PendingId;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;

public class SignBlockItem extends StandingAndWallBlockItem {
	public SignBlockItem(ForestryStandingSignBlock block, Item.Properties properties) {
		super(block, ArboricultureBlocks.WALL_SIGN.get(block.getWoodType()).block(), Direction.DOWN, PendingId.blockItem(block, properties.signText()));
	}
}
