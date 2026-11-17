package forestry.core.platform.transfer;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

import javax.annotation.Nullable;

// journal of one Forestry handler, also tells when the last transaction that touched the handler is over
public abstract class HandlerJournal<S> extends SnapshotJournal<S> {
	private int openSnapshots;
	@Nullable
	private S start;

	protected abstract S save();

	protected abstract void restore(S snapshot);

	// start holds the contents from before the transaction, they are still the contents if everything was aborted
	protected void onEnd(S start) {
	}

	public final boolean isActive() {
		return this.openSnapshots > 0;
	}

	@Override
	protected final S createSnapshot() {
		S snapshot = save();
		if (this.openSnapshots++ == 0) {
			this.start = snapshot;
		}
		return snapshot;
	}

	@Override
	protected final void revertToSnapshot(S snapshot) {
		restore(snapshot);
	}

	// every snapshot ends here, after an abort, after the root commit or when an outer snapshot replaces it
	@Override
	protected final void releaseSnapshot(S snapshot) {
		if (--this.openSnapshots == 0) {
			S first = this.start;
			this.start = null;
			onEnd(first);
		}
	}
}
