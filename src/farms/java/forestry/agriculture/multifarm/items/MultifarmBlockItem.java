package forestry.agriculture.multifarm.items;

import forestry.core.platform.client.ClientsideCode;
import forestry.core.platform.util.TooltipList;
import forestry.core.platform.util.TranslationKeys;
import forestry.core.platform.item.ForestryBlockItem;
import forestry.agriculture.multifarm.blocks.MultifarmBlock;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class MultifarmBlockItem extends ForestryBlockItem<MultifarmBlock> {
	public MultifarmBlockItem(MultifarmBlock block) {
		super(block, new Item.Properties());
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
		List<Component> tooltip = new TooltipList(adder);
		if (FMLEnvironment.getDist() == Dist.CLIENT && ClientsideCode.hasShiftDown()) {
			tooltip.add(Component.translatable("block.forestry.farm.tooltip").withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(Component.translatable(TranslationKeys.HOLD_SHIFT_FOR_DETAILS).withStyle(ChatFormatting.GRAY));
		}
	}
}
