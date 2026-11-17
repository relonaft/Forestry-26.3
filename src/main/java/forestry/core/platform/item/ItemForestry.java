package forestry.core.platform.item;

import forestry.core.platform.util.ItemTooltipUtil;
import forestry.core.platform.util.TooltipList;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemForestry extends Item {
	public ItemForestry() {
		this(new Properties());
	}

	public ItemForestry(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag advanced) {
		List<Component> tooltip = new TooltipList(adder);
		ItemTooltipUtil.addInformation(stack, tooltip);
	}
}
