package forestry.core.platform.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

// shows an insert-only Forestry item handler whose insert cannot be undone, ex. the mailbox that sends the letter
// inside a transaction the insert is only simulated, the real insert runs when the transaction is committed
final class DeferredInsertHandler extends SnapshotJournal<Integer> implements ResourceHandler<ItemResource> {
	private final IItemHandler handler;
	private final Consumer<ItemStack> rejected;
	private final Deque<Insert> pending = new ArrayDeque<>();

	DeferredInsertHandler(IItemHandler handler, Consumer<ItemStack> rejected) {
		this.handler = handler;
		this.rejected = rejected;
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
		int accepted = amount - this.handler.insertItem(index, resource.toStack(amount), true).getCount();
		if (accepted <= 0) {
			return 0;
		}
		updateSnapshots(transaction);
		this.pending.add(new Insert(index, resource, accepted));
		return accepted;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return 0;
	}

	@Override
	protected Integer createSnapshot() {
		return this.pending.size();
	}

	@Override
	protected void revertToSnapshot(Integer snapshot) {
		while (this.pending.size() > snapshot) {
			this.pending.removeLast();
		}
	}

	@Override
	protected void onRootCommit(Integer originalState) {
		// one at a time, an insert can start a transaction that adds to the queue
		while (!this.pending.isEmpty()) {
			Insert insert = this.pending.removeFirst();
			ItemStack rest = this.handler.insertItem(insert.slot, insert.resource.toStack(insert.amount), false);
			if (!rest.isEmpty()) {
				this.rejected.accept(rest);
			}
		}
	}

	private record Insert(int slot, ItemResource resource, int amount) {
	}
}
