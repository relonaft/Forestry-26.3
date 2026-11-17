package forestry.arboriculture.wood;

import forestry.core.platform.item.ForestryBlockItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public class ForestryWoodBlockItem<B extends Block & IWoodTyped> extends ForestryBlockItem<B> {
	private final IWoodTyped wood;

	public ForestryWoodBlockItem(B block) {
		this(block, ContextIntProviders.COOKING_TIME_WOOD_BLOCKS);
	}

	protected ForestryWoodBlockItem(B block, ResourceKey<ContextIntProvider> burnTime) {
		super(block, block.isFireproof() ? new Item.Properties() : new Item.Properties().cookingFuel(burnTime));

		// Safeguard against Diagonal Fence's registry replacements causing crashes
		this.wood = block;
	}
}
