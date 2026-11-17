package forestry.core.platform.transfer;

import forestry.core.platform.transfer.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

// shows a Forestry fluid handler to NeoForge and other mods
final class ExposedFluidHandler implements ResourceHandler<FluidResource> {
	final IFluidHandler handler;
	private final ITransactional transactional;

	ExposedFluidHandler(IFluidHandler handler) {
		if (!(handler instanceof ITransactional transactional)) {
			throw new IllegalArgumentException("Fluid handler cannot roll back a transaction: " + handler.getClass().getName());
		}
		this.handler = handler;
		this.transactional = transactional;
	}

	@Override
	public int size() {
		return this.handler.getTanks();
	}

	@Override
	public FluidResource getResource(int index) {
		return FluidResource.of(this.handler.getFluidInTank(index));
	}

	@Override
	public long getAmountAsLong(int index) {
		return this.handler.getFluidInTank(index).getAmount();
	}

	@Override
	public long getCapacityAsLong(int index, FluidResource resource) {
		return resource.isEmpty() || isValid(index, resource) ? this.handler.getTankCapacity(index) : 0;
	}

	@Override
	public boolean isValid(int index, FluidResource resource) {
		return this.handler.isFluidValid(index, resource.toStack(1));
	}

	// the Forestry handler picks the tank itself, the index only decides if the call is allowed
	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, size());
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (!isValid(index, resource)) {
			return 0;
		}
		return insert(resource, amount, transaction);
	}

	@Override
	public int insert(FluidResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (amount == 0) {
			return 0;
		}
		FluidStack stack = resource.toStack(amount);
		// no snapshot and no change when nothing fits
		if (this.handler.fill(stack, FluidAction.SIMULATE) <= 0) {
			return 0;
		}
		this.transactional.updateSnapshots(transaction);
		return Math.max(0, this.handler.fill(stack, FluidAction.EXECUTE));
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, size());
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (!resource.matches(this.handler.getFluidInTank(index))) {
			return 0;
		}
		return extract(resource, amount, transaction);
	}

	@Override
	public int extract(FluidResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (amount == 0) {
			return 0;
		}
		FluidStack stack = resource.toStack(amount);
		// a tank manager matches the fluid only, the caller must get exactly the resource it asked for
		if (!resource.matches(this.handler.drain(stack, FluidAction.SIMULATE))) {
			return 0;
		}
		this.transactional.updateSnapshots(transaction);
		return this.handler.drain(stack, FluidAction.EXECUTE).getAmount();
	}
}
