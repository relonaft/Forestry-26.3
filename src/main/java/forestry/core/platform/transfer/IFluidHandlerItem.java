package forestry.core.platform.transfer;

import net.minecraft.world.item.ItemStack;

public interface IFluidHandlerItem extends IFluidHandler {
	// the stack that is left after the last fill or drain, it can be another stack than the one the handler started with
	ItemStack getContainer();
}
