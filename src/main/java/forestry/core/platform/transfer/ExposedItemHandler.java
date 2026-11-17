package forestry.core.platform.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// shows a Forestry item handler that is more than a container to NeoForge and other mods
final class ExposedItemHandler implements ResourceHandler<ItemResource> {
	final IItemHandler handler;
	private final ITransactional transactional;

	ExposedItemHandler(IItemHandler handler) {
		if (!(handler instanceof ITransactional transactional)) {
			throw new IllegalArgumentException("Item handler cannot roll back a transaction: " + handler.getClass().getName());
		}
		this.handler = handler;
		this.transactional = transactional;
	}

	@Override
	public int size() {
		return this.handler.getSlots();
	}

	@Override
	public ItemResource getResource(int index) {
		return ItemResource.of(this.handler.getStackInSlot(index));
	}

	@Override
	public long getAmountAsLong(int index) {
		return this.handler.getStackInSlot(index).getCount();
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		int limit = this.handler.getSlotLimit(index);
		if (resource.isEmpty()) {
			return limit;
		}
		return isValid(index, resource) ? Math.min(limit, resource.getMaxStackSize()) : 0;
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		return this.handler.isItemValid(index, resource.toStack());
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (amount == 0) {
			return 0;
		}
		ItemStack stack = resource.toStack(amount);
		// no snapshot and no change when nothing fits
		if (this.handler.insertItem(index, stack, true).getCount() >= amount) {
			return 0;
		}
		this.transactional.updateSnapshots(transaction);
		return amount - this.handler.insertItem(index, stack, false).getCount();
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (amount == 0 || !resource.matches(this.handler.extractItem(index, amount, true))) {
			return 0;
		}
		this.transactional.updateSnapshots(transaction);
		return this.handler.extractItem(index, amount, false).getCount();
	}
}
