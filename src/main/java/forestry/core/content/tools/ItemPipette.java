package forestry.core.content.tools;

import forestry.api.core.IToolPipette;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.fluids.PipetteContents;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.transfer.Transfers;
import forestry.core.platform.util.RenderUtil;
import forestry.core.platform.util.TooltipList;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.fluids.FluidType;
import forestry.core.platform.transfer.IFluidHandlerItem;

import java.util.List;
import forestry.core.platform.item.ItemForestry;

public class ItemPipette extends ItemForestry implements IToolPipette, ITintedItem {
	public ItemPipette() {
		super(new Properties().stacksTo(1));
	}

	@Override
	public boolean canPipette(ItemStack itemstack) {
		PipetteContents contained = PipetteContents.create(itemstack);
		return contained == null || !contained.isFull();
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
		List<Component> list = new TooltipList(adder);
		super.appendHoverText(itemstack, context, display, adder, flag);

		PipetteContents contained = PipetteContents.create(itemstack);
		if (contained != null) {
			contained.addTooltip(list);
		}
	}

	public IFluidHandlerItem createFluidHandler(ItemStack stack) {
		return Transfers.viewFluid(stack, PipetteFluidHandler::new);
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		if (tintIndex == 1) {
			PipetteContents contents = PipetteContents.create(stack);

			if (contents != null) {
				return RenderUtil.getFluidColor(contents.getContents().getFluid());
			}
		}
		return 0xffffff;
	}
}
