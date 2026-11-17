package forestry.arboriculture.wood;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public class ForestrySlabBlockItem extends BlockItem {
	public ForestrySlabBlockItem(ForestrySlabBlock block) {
		super(block, block.isFireproof() ? new Item.Properties() : new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_SLABS));
	}
}
