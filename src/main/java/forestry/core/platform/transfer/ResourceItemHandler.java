package forestry.core.platform.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// shows an item handler of NeoForge or another mod to Forestry code, every insert or extract is one transaction
final class ResourceItemHandler implements IItemHandler {
	final ResourceHandler<ItemResource> handler;

	ResourceItemHandler(ResourceHandler<ItemResource> handler) {
		this.handler = handler;
	}

	@Override
	public int getSlots() {
		return this.handler.size();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return ItemUtil.getStack(this.handler, slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int inserted;
		try (Transaction transaction = Transfers.openTransaction()) {
			inserted = this.handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
			if (!simulate) {
				transaction.commit();
			}
		}

		if (inserted <= 0) {
			return stack;
		}
		return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemResource resource = this.handler.getResource(slot);
		if (resource.isEmpty()) {
			return ItemStack.EMPTY;
		}

		try (Transaction transaction = Transfers.openTransaction()) {
			int extracted = this.handler.extract(slot, resource, amount, transaction);
			if (!simulate) {
				transaction.commit();
			}
			return extracted > 0 ? resource.toStack(extracted) : ItemStack.EMPTY;
		}
	}

	@Override
	public int getSlotLimit(int slot) {
		return this.handler.getCapacityAsInt(slot, ItemResource.EMPTY);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return !stack.isEmpty() && this.handler.isValid(slot, ItemResource.of(stack));
	}

	// two views of one handler are the same inventory, as two 1.21.1 wrappers of one container were
	@Override
	public boolean equals(Object o) {
		return o instanceof ResourceItemHandler other && this.handler.equals(other.handler);
	}

	@Override
	public int hashCode() {
		return this.handler.hashCode();
	}
}
