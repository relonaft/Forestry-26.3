package forestry.core.platform.transfer;

import forestry.core.platform.transfer.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.FluidStack;

public interface IFluidTank {
	FluidStack getFluid();

	int getFluidAmount();

	int getCapacity();

	boolean isFluidValid(FluidStack stack);

	int fill(FluidStack resource, FluidAction action);

	FluidStack drain(int maxDrain, FluidAction action);

	FluidStack drain(FluidStack resource, FluidAction action);
}
