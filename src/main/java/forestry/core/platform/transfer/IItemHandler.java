package forestry.core.platform.transfer;

import net.minecraft.world.item.ItemStack;

// item handler with the shape NeoForge had on 1.21.1, the inventory logic of Forestry is written against it
public interface IItemHandler {
	int getSlots();

	// callers must not change the returned stack
	ItemStack getStackInSlot(int slot);

	// returns what did not fit, the given stack itself when nothing was inserted
	ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

	ItemStack extractItem(int slot, int amount, boolean simulate);

	int getSlotLimit(int slot);

	boolean isItemValid(int slot, ItemStack stack);
}
