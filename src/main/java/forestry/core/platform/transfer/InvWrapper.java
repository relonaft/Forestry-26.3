package forestry.core.platform.transfer;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

// item handler over a vanilla container, behaves like the NeoForge 1.21.1 wrapper of the same name
public class InvWrapper implements IItemHandler {
	private final Container inv;

	public InvWrapper(Container inv) {
		this.inv = inv;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return getInv().equals(((InvWrapper) o).getInv());
	}

	@Override
	public int hashCode() {
		return getInv().hashCode();
	}

	@Override
	public int getSlots() {
		return getInv().getContainerSize();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return getInv().getItem(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		Container inv = getInv();
		ItemStack present = inv.getItem(slot);
		boolean occupied = !present.isEmpty();
		if (occupied) {
			if (present.getCount() >= Math.min(present.getMaxStackSize(), getSlotLimit(slot))) {
				return stack;
			}
			if (!ItemStack.isSameItemSameComponents(stack, present)) {
				return stack;
			}
		}
		if (!inv.canPlaceItem(slot, stack)) {
			return stack;
		}

		int room = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - (occupied ? present.getCount() : 0);
		if (stack.getCount() <= room) {
			if (!simulate) {
				ItemStack placed = stack;
				if (occupied) {
					placed = stack.copy();
					placed.grow(present.getCount());
				}
				inv.setItem(slot, placed);
				inv.setChanged();
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
		inv.setItem(slot, placed);
		inv.setChanged();
		return rest;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount == 0) {
			return ItemStack.EMPTY;
		}

		Container inv = getInv();
		ItemStack present = inv.getItem(slot);
		if (present.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (simulate) {
			ItemStack copy = present.copy();
			if (present.getCount() >= amount) {
				copy.setCount(amount);
			}
			return copy;
		}

		ItemStack removed = inv.removeItem(slot, Math.min(present.getCount(), amount));
		inv.setChanged();
		return removed;
	}

	@Override
	public int getSlotLimit(int slot) {
		return getInv().getMaxStackSize();
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return getInv().canPlaceItem(slot, stack);
	}

	public Container getInv() {
		return this.inv;
	}
}
