package forestry.core.platform.transfer;

import forestry.Forestry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nullable;

// a block that a bucket can pick up, as a fluid handler that gives one bucket of its fluid
final class BucketPickupFluidHandler implements IFluidHandler {
	@Nullable
	private final Player player;
	private final BucketPickup pickup;
	private final Level level;
	private final BlockPos pos;

	BucketPickupFluidHandler(@Nullable Player player, BucketPickup pickup, Level level, BlockPos pos) {
		this.player = player;
		this.pickup = pickup;
		this.level = level;
		this.pos = pos;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		if (tank == 0) {
			FluidState fluidState = this.level.getFluidState(this.pos);
			if (!fluidState.isEmpty()) {
				return new FluidStack(fluidState.getType(), FluidType.BUCKET_VOLUME);
			}
		}
		return FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return FluidType.BUCKET_VOLUME;
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return true;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || resource.getAmount() < FluidType.BUCKET_VOLUME) {
			return FluidStack.EMPTY;
		}
		FluidState fluidState = this.level.getFluidState(this.pos);
		if (fluidState.isEmpty() || resource.getFluid() != fluidState.getType()) {
			return FluidStack.EMPTY;
		}

		if (action.simulate()) {
			return new FluidStack(fluidState.getType(), FluidType.BUCKET_VOLUME);
		}

		FluidStack picked = pickUp();
		if (!picked.isEmpty() && !FluidStack.isSameFluidSameComponents(resource, picked)) {
			Forestry.LOGGER.error("Fluid removed without successfully being picked up. Fluid {} at {} in {} matched requested type, but after performing pickup was {}.",
				BuiltInRegistries.FLUID.getKey(fluidState.getType()), this.pos, this.level.dimension().identifier(), BuiltInRegistries.FLUID.getKey(picked.getFluid()));
			return FluidStack.EMPTY;
		}
		return picked;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain < FluidType.BUCKET_VOLUME) {
			return FluidStack.EMPTY;
		}
		FluidState fluidState = this.level.getFluidState(this.pos);
		if (fluidState.isEmpty()) {
			return FluidStack.EMPTY;
		}
		if (action.simulate()) {
			return new FluidStack(fluidState.getType(), FluidType.BUCKET_VOLUME);
		}
		return pickUp();
	}

	private FluidStack pickUp() {
		ItemStack bucket = this.pickup.pickupBlock(this.player, this.level, this.pos, this.level.getBlockState(this.pos));
		if (bucket != ItemStack.EMPTY && bucket.getItem() instanceof BucketItem bucketItem) {
			return new FluidStack(bucketItem.content, FluidType.BUCKET_VOLUME);
		}
		return FluidStack.EMPTY;
	}
}
