package forestry.core.content.analyzer;

import forestry.core.features.CoreDataComponents;
import forestry.core.content.analyzer.PortableAnalyzerMenu;
import forestry.core.platform.inventory.PortableAnalyzerInventory;
import forestry.core.platform.util.TooltipList;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import forestry.core.platform.item.WithScreenItem;
import net.minecraft.world.item.component.TooltipDisplay;

public class PortableAnalyzerItem extends WithScreenItem {
	public PortableAnalyzerItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag advanced) {
		List<Component> tooltip = new TooltipList(adder);
		super.appendHoverText(stack, context, display, adder, advanced);
		int charges = stack.getOrDefault(CoreDataComponents.ALYZER_CHARGES, 0);
		tooltip.add(Component.translatable(stack.getItem().getDescriptionId() + ".charges", charges).withStyle(ChatFormatting.GOLD));
	}

	@Override
	public AbstractContainerMenu getContainer(int containerId, Player player, ItemStack heldItem) {
		return new PortableAnalyzerMenu(containerId, new PortableAnalyzerInventory(player, heldItem), player);
	}
}
