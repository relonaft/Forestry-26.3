package forestry.core.platform.item;

import forestry.core.platform.block.IBlockTooltip;
import forestry.core.platform.util.ItemTooltipUtil;
import forestry.core.platform.util.TooltipList;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ForestryBlockItem<B extends Block> extends BlockItem {
	public ForestryBlockItem(B block, Item.Properties builder) {
		super(block, builder);
	}

	public ForestryBlockItem(B block) {
		this(block, new Item.Properties());
	}

	@Override
	public B getBlock() {
		//noinspection unchecked
		return (B) super.getBlock();
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag advanced) {
		List<Component> tooltip = new TooltipList(adder);
		super.appendHoverText(stack, context, display, adder, advanced);
		if (getBlock() instanceof IBlockTooltip blockTooltip) {
			blockTooltip.appendHoverText(stack, context, tooltip, advanced);
		}
		ItemTooltipUtil.addInformation(stack, tooltip);
	}
}
