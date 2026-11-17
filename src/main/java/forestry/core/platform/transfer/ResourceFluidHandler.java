package forestry.core.platform.transfer;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// shows a fluid handler of NeoForge or another mod to Forestry code, every fill or drain is one transaction
class ResourceFluidHandler implements IFluidHandler {
	final ResourceHandler<FluidResource> handler;

	ResourceFluidHandler(ResourceHandler<FluidResource> handler) {
		this.handler = handler;
	}

	@Override
	public int getTanks() {
		return this.handler.size();
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		FluidResource resource = this.handler.getResource(tank);
		return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack(this.handler.getAmountAsInt(tank));
	}

	@Override
	public int getTankCapacity(int tank) {
		return this.handler.getCapacityAsInt(tank, FluidResource.EMPTY);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return !stack.isEmpty() && this.handler.isValid(tank, FluidResource.of(stack));
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) {
			return 0;
		}
		try (Transaction transaction = Transfers.openTransaction()) {
			int filled = this.handler.insert(FluidResource.of(resource), resource.getAmount(), transaction);
			if (action.execute()) {
				transaction.commit();
			}
			return filled;
		}
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty()) {
			return FluidStack.EMPTY;
		}
		return drain(FluidResource.of(resource), resource.getAmount(), action);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain <= 0) {
			return FluidStack.EMPTY;
		}
		// the first fluid that gives something, as a 1.21.1 handler picked the fluid itself
		for (int tank = 0; tank < this.handler.size(); tank++) {
			FluidResource resource = this.handler.getResource(tank);
			if (!resource.isEmpty()) {
				FluidStack drained = drain(resource, maxDrain, action);
				if (!drained.isEmpty()) {
					return drained;
				}
			}
		}
		return FluidStack.EMPTY;
	}

	private FluidStack drain(FluidResource resource, int amount, FluidAction action) {
		try (Transaction transaction = Transfers.openTransaction()) {
			int drained = this.handler.extract(resource, amount, transaction);
			if (action.execute()) {
				transaction.commit();
			}
			return drained > 0 ? resource.toStack(drained) : FluidStack.EMPTY;
		}
	}
}
