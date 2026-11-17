package forestry.core.platform.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

// shows the fluid handler of an item stack the way a 1.21.1 item handler looked to Forestry code
final class ItemStackFluidHandler extends ResourceFluidHandler implements IFluidHandlerItem {
	private final ItemStackAccess access;

	ItemStackFluidHandler(ItemStackAccess access, ResourceHandler<FluidResource> handler) {
		super(handler);
		this.access = access;
	}

	@Override
	public ItemStack getContainer() {
		return this.access.getStack();
	}

	// a 1.21.1 item handler did nothing unless the stack held exactly one item
	private boolean isSingleItem() {
		return this.access.getAmount() == 1;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return isSingleItem() ? super.fill(resource, action) : 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return isSingleItem() ? super.drain(resource, action) : FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return isSingleItem() ? super.drain(maxDrain, action) : FluidStack.EMPTY;
	}
}
