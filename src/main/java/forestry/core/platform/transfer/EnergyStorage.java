package forestry.core.platform.transfer;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// energy buffer with the behavior of the NeoForge 1.21.1 class that ForestryEnergyStorage was built on
public class EnergyStorage implements IEnergyStorage, ITransactional {
	protected int energy;
	protected int capacity;
	protected int maxReceive;
	protected int maxExtract;

	private final SnapshotJournal<Integer> journal = new SnapshotJournal<>() {
		@Override
		protected Integer createSnapshot() {
			return EnergyStorage.this.energy;
		}

		@Override
		protected void revertToSnapshot(Integer snapshot) {
			EnergyStorage.this.energy = snapshot;
		}
	};

	public EnergyStorage(int capacity, int maxReceive, int maxExtract) {
		this.capacity = capacity;
		this.maxReceive = maxReceive;
		this.maxExtract = maxExtract;
	}

	@Override
	public int receiveEnergy(int toReceive, boolean simulate) {
		if (!canReceive() || toReceive <= 0) {
			return 0;
		}

		int received = Mth.clamp(this.capacity - this.energy, 0, Math.min(this.maxReceive, toReceive));
		if (!simulate) {
			this.energy += received;
		}
		return received;
	}

	@Override
	public int extractEnergy(int toExtract, boolean simulate) {
		if (!canExtract() || toExtract <= 0) {
			return 0;
		}

		int extracted = Math.min(this.energy, Math.min(this.maxExtract, toExtract));
		if (!simulate) {
			this.energy -= extracted;
		}
		return extracted;
	}

	@Override
	public int getEnergyStored() {
		return this.energy;
	}

	@Override
	public int getMaxEnergyStored() {
		return this.capacity;
	}

	@Override
	public boolean canExtract() {
		return this.maxExtract > 0;
	}

	@Override
	public boolean canReceive() {
		return this.maxReceive > 0;
	}

	@Override
	public void updateSnapshots(TransactionContext transaction) {
		this.journal.updateSnapshots(transaction);
	}
}
