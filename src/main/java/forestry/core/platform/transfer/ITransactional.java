package forestry.core.platform.transfer;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// a Forestry handler that other mods may change inside a NeoForge transaction
public interface ITransactional {
	// must run before every change made inside the transaction, an abort then puts the saved contents back
	void updateSnapshots(TransactionContext transaction);
}
