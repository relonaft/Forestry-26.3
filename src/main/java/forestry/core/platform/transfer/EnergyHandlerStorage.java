package forestry.core.platform.transfer;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// shows an energy handler of NeoForge or another mod to Forestry code, every transfer is one transaction
final class EnergyHandlerStorage implements IEnergyStorage {
	final EnergyHandler handler;

	EnergyHandlerStorage(EnergyHandler handler) {
		this.handler = handler;
	}

	@Override
	public int receiveEnergy(int toReceive, boolean simulate) {
		if (toReceive <= 0) {
			return 0;
		}
		try (Transaction transaction = Transfers.openTransaction()) {
			int received = this.handler.insert(toReceive, transaction);
			if (!simulate) {
				transaction.commit();
			}
			return received;
		}
	}

	@Override
	public int extractEnergy(int toExtract, boolean simulate) {
		if (toExtract <= 0) {
			return 0;
		}
		try (Transaction transaction = Transfers.openTransaction()) {
			int extracted = this.handler.extract(toExtract, transaction);
			if (!simulate) {
				transaction.commit();
			}
			return extracted;
		}
	}

	@Override
	public int getEnergyStored() {
		return this.handler.getAmountAsInt();
	}

	@Override
	public int getMaxEnergyStored() {
		return this.handler.getCapacityAsInt();
	}

	// deviation: an EnergyHandler does not tell if it can give energy, so this asks it for some
	// an empty handler gives nothing either way and counts as able
	@Override
	public boolean canExtract() {
		return extractEnergy(Integer.MAX_VALUE, true) > 0 || this.handler.getAmountAsLong() <= 0;
	}

	// deviation: an EnergyHandler does not tell if it can take energy, so this offers it some
	// a full handler takes nothing either way and counts as able
	@Override
	public boolean canReceive() {
		return receiveEnergy(Integer.MAX_VALUE, true) > 0 || this.handler.getAmountAsLong() >= this.handler.getCapacityAsLong();
	}
}
