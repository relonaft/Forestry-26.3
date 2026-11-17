package forestry.core.platform.transfer;

import forestry.Forestry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Predicate;

// single tank with the behavior of the NeoForge 1.21.1 tank that StandardTank was built on
public class FluidTank implements IFluidHandler, IFluidTank {
	protected Predicate<FluidStack> validator;
	protected FluidStack fluid = FluidStack.EMPTY;
	protected int capacity;

	public FluidTank(int capacity) {
		this(capacity, stack -> true);
	}

	public FluidTank(int capacity, Predicate<FluidStack> validator) {
		this.capacity = capacity;
		this.validator = validator;
	}

	public FluidTank setCapacity(int capacity) {
		this.capacity = capacity;
		return this;
	}

	public FluidTank setValidator(Predicate<FluidStack> validator) {
		if (validator != null) {
			this.validator = validator;
		}
		return this;
	}

	@Override
	public boolean isFluidValid(FluidStack stack) {
		return this.validator.test(stack);
	}

	@Override
	public int getCapacity() {
		return this.capacity;
	}

	@Override
	public FluidStack getFluid() {
		return this.fluid;
	}

	@Override
	public int getFluidAmount() {
		return this.fluid.getAmount();
	}

	public FluidTank readFromNBT(HolderLookup.Provider registries, CompoundTag nbt) {
		CompoundTag tag = nbt.getCompoundOrEmpty("Fluid");
		if (tag.isEmpty()) {
			this.fluid = FluidStack.EMPTY;
		} else {
			this.fluid = FluidStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
				.resultOrPartial(error -> Forestry.LOGGER.error("Tried to load invalid fluid: '{}'", error))
				.orElse(FluidStack.EMPTY);
		}
		return this;
	}

	public CompoundTag writeToNBT(HolderLookup.Provider registries, CompoundTag nbt) {
		if (!this.fluid.isEmpty()) {
			nbt.put("Fluid", FluidStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this.fluid).getOrThrow());
		}
		return nbt;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return getFluid();
	}

	@Override
	public int getTankCapacity(int tank) {
		return getCapacity();
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return isFluidValid(stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !isFluidValid(resource)) {
			return 0;
		}
		boolean wasEmpty = this.fluid.isEmpty();
		if (!wasEmpty && !FluidStack.isSameFluidSameComponents(this.fluid, resource)) {
			return 0;
		}

		int space = wasEmpty ? this.capacity : this.capacity - this.fluid.getAmount();
		int filled = Math.min(space, resource.getAmount());
		if (action.simulate()) {
			return filled;
		}

		if (wasEmpty) {
			this.fluid = resource.copyWithAmount(filled);
			onContentsChanged();
			return this.fluid.getAmount();
		}

		if (resource.getAmount() < space) {
			this.fluid.grow(filled);
		} else {
			this.fluid.setAmount(this.capacity);
		}
		if (filled > 0) {
			onContentsChanged();
		}
		return filled;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, this.fluid)) {
			return FluidStack.EMPTY;
		}
		return drain(resource.getAmount(), action);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		int drained = Math.min(maxDrain, this.fluid.getAmount());
		FluidStack stack = this.fluid.copyWithAmount(drained);
		if (action.execute() && drained > 0) {
			this.fluid.shrink(drained);
			onContentsChanged();
		}
		return stack;
	}

	protected void onContentsChanged() {
	}

	public void setFluid(FluidStack stack) {
		this.fluid = stack;
	}

	public boolean isEmpty() {
		return this.fluid.isEmpty();
	}

	public int getSpace() {
		return Math.max(0, this.capacity - this.fluid.getAmount());
	}
}
