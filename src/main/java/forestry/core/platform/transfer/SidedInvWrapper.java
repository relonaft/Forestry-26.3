package forestry.core.platform.transfer;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import javax.annotation.Nullable;

// item handler over one side of a vanilla sided container, behaves like the NeoForge 1.21.1 wrapper of the same name
public class SidedInvWrapper implements IItemHandler {
	protected final WorldlyContainer inv;
	@Nullable
	protected final Direction side;
	// canPlaceItem of these two refuses a second item, which cannot hold back an insert of several at once
	private final boolean brewingStand;
	private final boolean furnace;

	public SidedInvWrapper(WorldlyContainer inv, @Nullable Direction side) {
		this.inv = inv;
		this.side = side;
		this.brewingStand = inv instanceof BrewingStandBlockEntity;
		this.furnace = inv instanceof AbstractFurnaceBlockEntity;
	}

	public static int getSlot(WorldlyContainer inv, int slot, @Nullable Direction side) {
		if (side == null) {
			return slot;
		}
		int[] slots = inv.getSlotsForFace(side);
		return slot < slots.length ? slots[slot] : -1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SidedInvWrapper that = (SidedInvWrapper) o;
		return this.inv.equals(that.inv) && this.side == that.side;
	}

	@Override
	public int hashCode() {
		return 31 * this.inv.hashCode() + (this.side == null ? 0 : this.side.hashCode());
	}

	@Override
	public int getSlots() {
		return this.side == null ? this.inv.getContainerSize() : this.inv.getSlotsForFace(this.side).length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		int invSlot = getSlot(this.inv, slot, this.side);
		return invSlot == -1 ? ItemStack.EMPTY : this.inv.getItem(invSlot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int invSlot = getSlot(this.inv, slot, this.side);
		if (invSlot == -1) {
			return stack;
		}

		ItemStack present = this.inv.getItem(invSlot);
		boolean occupied = !present.isEmpty();
		if (occupied) {
			if (present.getCount() >= Math.min(present.getMaxStackSize(), getSlotLimit(slot))) {
				return stack;
			}
			if (!ItemStack.isSameItemSameComponents(stack, present)) {
				return stack;
			}
		}
		if (!this.inv.canPlaceItemThroughFace(invSlot, stack, this.side) || !this.inv.canPlaceItem(invSlot, stack)) {
			return stack;
		}

		int room;
		if (occupied) {
			room = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - present.getCount();
		} else if (this.furnace && invSlot == 1 && stack.is(Items.BUCKET)) {
			// a furnace takes one empty bucket as fuel
			room = 1;
		} else {
			room = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
		}

		if (stack.getCount() <= room) {
			if (!simulate) {
				ItemStack placed = stack;
				if (occupied) {
					placed = stack.copy();
					placed.grow(present.getCount());
				}
				setInventorySlotContents(invSlot, placed);
			}
			return ItemStack.EMPTY;
		}

		// the stack of the caller stays untouched
		ItemStack rest = stack.copy();
		if (simulate) {
			rest.shrink(room);
			return rest;
		}
		ItemStack placed = rest.split(room);
		if (occupied) {
			placed.grow(present.getCount());
		}
		setInventorySlotContents(invSlot, placed);
		return rest;
	}

	private void setInventorySlotContents(int invSlot, ItemStack stack) {
		// setChanged comes first, as it did in the NeoForge wrapper
		this.inv.setChanged();
		this.inv.setItem(invSlot, stack);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount == 0) {
			return ItemStack.EMPTY;
		}

		int invSlot = getSlot(this.inv, slot, this.side);
		if (invSlot == -1) {
			return ItemStack.EMPTY;
		}

		ItemStack present = this.inv.getItem(invSlot);
		if (present.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (this.side != null && !this.inv.canTakeItemThroughFace(invSlot, present, this.side)) {
			return ItemStack.EMPTY;
		}

		if (simulate) {
			ItemStack copy = present.copy();
			if (present.getCount() >= amount) {
				copy.setCount(amount);
			}
			return copy;
		}

		ItemStack removed = this.inv.removeItem(invSlot, Math.min(present.getCount(), amount));
		this.inv.setChanged();
		return removed;
	}

	@Override
	public int getSlotLimit(int slot) {
		// a brewing stand holds one bottle per bottle slot
		if (this.brewingStand && getSlot(this.inv, slot, this.side) < 3) {
			return 1;
		}
		return this.inv.getMaxStackSize();
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		int invSlot = getSlot(this.inv, slot, this.side);
		return invSlot != -1 && this.inv.canPlaceItem(invSlot, stack);
	}
}
