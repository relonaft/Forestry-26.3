package forestry.core.platform.transfer;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// item access over one stack that reproduces what the 1.21.1 item handlers did to their container
// not ItemAccess.forStack, that one cannot swap the item and a bucket could never be filled or emptied
final class ItemStackAccess extends SnapshotJournal<ItemStack> implements ItemAccess {
	private final ItemStack original;
	// an empty stack reports air
	private final Item originalItem;
	// replaced on every change and never changed in place, so the old stack is the snapshot
	private ItemStack stack;

	ItemStackAccess(ItemStack stack) {
		this.original = stack;
		this.originalItem = stack.getItem();
		this.stack = stack;
	}

	ItemStack getStack() {
		return this.stack;
	}

	@Override
	public ItemResource getResource() {
		return ItemResource.of(this.stack);
	}

	@Override
	public int getAmount() {
		return this.stack.getCount();
	}

	@Override
	public int insert(ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (amount == 0 || !this.stack.isEmpty() && !resource.matches(this.stack)) {
			return 0;
		}
		updateSnapshots(transaction);
		this.stack = resource.toStack(this.stack.getCount() + amount);
		return amount;
	}

	@Override
	public int extract(ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (!resource.matches(this.stack)) {
			return 0;
		}
		int extracted = Math.min(amount, this.stack.getCount());
		if (extracted > 0) {
			updateSnapshots(transaction);
			int left = this.stack.getCount() - extracted;
			this.stack = left == 0 ? ItemStack.EMPTY : resource.toStack(left);
		}
		return extracted;
	}

	@Override
	protected ItemStack createSnapshot() {
		return this.stack;
	}

	@Override
	protected void revertToSnapshot(ItemStack snapshot) {
		this.stack = snapshot;
	}

	// callers on 1.21.1 kept using the stack they made the handler from, a component handler changed that stack
	// and a bucket handler left it alone and held a new stack
	@Override
	protected void onRootCommit(ItemStack originalState) {
		if (this.stack == this.original) {
			return;
		}
		if (this.stack.isEmpty()) {
			this.original.setCount(0);
			this.stack = this.original;
		} else if (this.stack.getItem() == this.originalItem) {
			// the count first, an empty stack has no component map to patch
			this.original.setCount(this.stack.getCount());
			((PatchedDataComponentMap) this.original.getComponents()).restorePatch(this.stack.getComponentsPatch());
			this.stack = this.original;
		}
	}
}
