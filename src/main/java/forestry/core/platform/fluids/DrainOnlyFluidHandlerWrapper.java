package forestry.core.platform.fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import forestry.core.platform.transfer.IFluidHandler;
import forestry.core.platform.transfer.ITransactional;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nonnull;

public class DrainOnlyFluidHandlerWrapper implements IFluidHandler, ITransactional {
	private final IFluidHandler internalFluidHandler;

	public DrainOnlyFluidHandlerWrapper(IFluidHandler internalFluidHandler) {
		this.internalFluidHandler = internalFluidHandler;
	}

	@Override
	public int getTanks() {
		return this.internalFluidHandler.getTanks();
	}

	@Nonnull
	@Override
	public FluidStack getFluidInTank(int tank) {
		return this.internalFluidHandler.getFluidInTank(tank);
	}

	@Override
	public int getTankCapacity(int tank) {
		return this.internalFluidHandler.getTankCapacity(tank);
	}

	@Override
	public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
		return this.internalFluidHandler.isFluidValid(tank, stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return 0;
	}

	@Nonnull
	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return this.internalFluidHandler.drain(resource, action);
	}

	@Nonnull
	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return this.internalFluidHandler.drain(maxDrain, action);
	}

	// the wrapped handler holds the fluid, so it holds the snapshot as well
	@Override
	public void updateSnapshots(TransactionContext transaction) {
		if (!(this.internalFluidHandler instanceof ITransactional transactional)) {
			throw new IllegalStateException("Fluid handler cannot roll back a transaction: " + this.internalFluidHandler.getClass().getName());
		}
		transactional.updateSnapshots(transaction);
	}
}
