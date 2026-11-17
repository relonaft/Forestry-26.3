package forestry.core.platform.transfer;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// shows a Forestry energy storage to NeoForge and other mods
final class ExposedEnergyStorage implements EnergyHandler {
	final IEnergyStorage storage;
	private final ITransactional transactional;

	ExposedEnergyStorage(IEnergyStorage storage) {
		if (!(storage instanceof ITransactional transactional)) {
			throw new IllegalArgumentException("Energy storage cannot roll back a transaction: " + storage.getClass().getName());
		}
		this.storage = storage;
		this.transactional = transactional;
	}

	@Override
	public long getAmountAsLong() {
		return this.storage.getEnergyStored();
	}

	@Override
	public long getCapacityAsLong() {
		return this.storage.getMaxEnergyStored();
	}

	@Override
	public int insert(int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonNegative(amount);
		// no snapshot when nothing is received
		if (amount == 0 || this.storage.receiveEnergy(amount, true) <= 0) {
			return 0;
		}
		this.transactional.updateSnapshots(transaction);
		return this.storage.receiveEnergy(amount, false);
	}

	@Override
	public int extract(int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonNegative(amount);
		if (amount == 0 || this.storage.extractEnergy(amount, true) <= 0) {
			return 0;
		}
		this.transactional.updateSnapshots(transaction);
		return this.storage.extractEnergy(amount, false);
	}
}
