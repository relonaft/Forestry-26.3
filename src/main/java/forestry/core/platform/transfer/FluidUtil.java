package forestry.core.platform.transfer;

import forestry.core.platform.transfer.IFluidHandler.FluidAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

// the fluid helpers of NeoForge 1.21.1 that Forestry calls, with the behavior they had there
// not the FluidUtil of the NeoForge transfer package, which has other methods
public final class FluidUtil {
	private FluidUtil() {
	}

	public static Optional<IFluidHandler> getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
		return Optional.ofNullable(Transfers.viewFluid(level.getCapability(Capabilities.Fluid.BLOCK, pos, side)));
	}

	public static Optional<IFluidHandlerItem> getFluidHandler(ItemStack stack) {
		if (stack.isEmpty()) {
			return Optional.empty();
		}
		ItemStackAccess access = new ItemStackAccess(stack);
		ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
		return handler == null ? Optional.empty() : Optional.of(new ItemStackFluidHandler(access, handler));
	}

	public static Optional<FluidStack> getFluidContained(ItemStack container) {
		if (container.isEmpty()) {
			return Optional.empty();
		}
		return getFluidHandler(container.copyWithCount(1))
			.map(handler -> handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE))
			.filter(fluid -> !fluid.isEmpty());
	}

	// a held container is filled from the handler, or emptied into it when it cannot be filled
	public static boolean interactWithFluidHandler(Player player, InteractionHand hand, Level level, BlockPos pos, @Nullable Direction side) {
		Objects.requireNonNull(level);
		Objects.requireNonNull(pos);
		return getFluidHandler(level, pos, side).map(handler -> interactWithFluidHandler(player, hand, handler)).orElse(false);
	}

	private static boolean interactWithFluidHandler(Player player, InteractionHand hand, IFluidHandler handler) {
		ItemStack held = player.getItemInHand(hand);
		if (held.isEmpty()) {
			return false;
		}

		IItemHandler inventory = Transfers.viewItems(player.getCapability(Capabilities.Item.ENTITY));
		Objects.requireNonNull(inventory, "Player item handler is null");

		FluidActionResult result = tryStow(held, handler, inventory, player, true);
		if (!result.isSuccess()) {
			result = tryStow(held, handler, inventory, player, false);
		}
		if (result.isSuccess()) {
			player.setItemInHand(hand, result.getResult());
			return true;
		}
		return false;
	}

	// fills or empties one container of the held stack and puts the result into the inventory of the player
	private static FluidActionResult tryStow(ItemStack container, IFluidHandler handler, IItemHandler inventory, Player player, boolean fill) {
		if (container.isEmpty()) {
			return FluidActionResult.FAILURE;
		}

		if (player.getAbilities().instabuild) {
			// creative mode: the held stack does not change
			return tryMove(container, handler, player, true, fill).isSuccess() ? new FluidActionResult(container) : FluidActionResult.FAILURE;
		}
		if (container.getCount() == 1) {
			FluidActionResult moved = tryMove(container, handler, player, true, fill);
			return moved.isSuccess() ? moved : FluidActionResult.FAILURE;
		}

		if (!tryMove(container, handler, player, false, fill).isSuccess()) {
			return FluidActionResult.FAILURE;
		}
		FluidActionResult moved = tryMove(container, handler, player, true, fill);
		ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, moved.getResult(), false);
		if (!remainder.isEmpty()) {
			ItemHandlerHelper.giveItemToPlayer(player, remainder);
		}

		ItemStack rest = container.copy();
		rest.shrink(1);
		return new FluidActionResult(rest);
	}

	private static FluidActionResult tryMove(ItemStack container, IFluidHandler handler, @Nullable Player player, boolean execute, boolean fill) {
		return fill ? tryFillContainer(container, handler, Integer.MAX_VALUE, player, execute) : tryEmptyContainer(container, handler, Integer.MAX_VALUE, player, execute);
	}

	private static FluidActionResult tryFillContainer(ItemStack container, IFluidHandler source, int maxAmount, @Nullable Player player, boolean doFill) {
		// the stack of the caller stays untouched
		IFluidHandlerItem containerHandler = getFluidHandler(container.copyWithCount(1)).orElse(null);
		if (containerHandler == null) {
			return FluidActionResult.FAILURE;
		}

		FluidStack simulated = tryFluidTransfer(containerHandler, source, maxAmount, false);
		if (simulated.isEmpty()) {
			return FluidActionResult.FAILURE;
		}

		if (doFill) {
			tryFluidTransfer(containerHandler, source, maxAmount, true);
			playSound(player, simulated, SoundActions.BUCKET_FILL);
		} else {
			// the handler works on a copy, and getContainer is only right after a real fill
			containerHandler.fill(simulated, FluidAction.EXECUTE);
		}
		return new FluidActionResult(containerHandler.getContainer());
	}

	public static FluidActionResult tryEmptyContainer(ItemStack container, IFluidHandler destination, int maxAmount, @Nullable Player player, boolean doDrain) {
		// the stack of the caller stays untouched
		IFluidHandlerItem containerHandler = getFluidHandler(container.copyWithCount(1)).orElse(null);
		if (containerHandler == null) {
			return FluidActionResult.FAILURE;
		}

		FluidStack transfer = tryFluidTransfer(destination, containerHandler, maxAmount, doDrain);
		if (transfer.isEmpty()) {
			return FluidActionResult.FAILURE;
		}

		if (doDrain) {
			playSound(player, transfer, SoundActions.BUCKET_EMPTY);
		} else {
			// the handler works on a copy, and getContainer is only right after a real drain
			containerHandler.drain(transfer, FluidAction.EXECUTE);
		}
		return new FluidActionResult(containerHandler.getContainer());
	}

	private static void playSound(@Nullable Player player, FluidStack fluid, SoundAction action) {
		if (player == null) {
			return;
		}
		SoundEvent sound = fluid.getFluidType().getSound(fluid, action);
		if (sound != null) {
			player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	public static FluidActionResult tryPickUpFluid(ItemStack emptyContainer, @Nullable Player player, Level level, BlockPos pos, Direction side) {
		if (emptyContainer.isEmpty() || level == null || pos == null) {
			return FluidActionResult.FAILURE;
		}

		IFluidHandler source;
		if (level.getBlockState(pos).getBlock() instanceof BucketPickup pickup) {
			source = new BucketPickupFluidHandler(player, pickup, level, pos);
		} else {
			source = getFluidHandler(level, pos, side).orElse(null);
			if (source == null) {
				return FluidActionResult.FAILURE;
			}
		}
		return tryFillContainer(emptyContainer, source, Integer.MAX_VALUE, player, true);
	}

	// moves up to maxAmount of the first fluid the source gives, returns what was moved
	public static FluidStack tryFluidTransfer(IFluidHandler destination, IFluidHandler source, int maxAmount, boolean doTransfer) {
		FluidStack drainable = source.drain(maxAmount, FluidAction.SIMULATE);
		if (drainable.isEmpty()) {
			return FluidStack.EMPTY;
		}

		int fillable = destination.fill(drainable, FluidAction.SIMULATE);
		if (fillable <= 0) {
			return FluidStack.EMPTY;
		}
		drainable.setAmount(fillable);
		if (!doTransfer) {
			return drainable;
		}

		FluidStack drained = source.drain(drainable, FluidAction.EXECUTE);
		if (drained.isEmpty()) {
			return FluidStack.EMPTY;
		}
		drained.setAmount(destination.fill(drained, FluidAction.EXECUTE));
		return drained;
	}
}
