package forestry.core.platform.transfer;

// energy storage with the shape NeoForge had on 1.21.1, the energy logic of Forestry is written against it
public interface IEnergyStorage {
	int receiveEnergy(int toReceive, boolean simulate);

	int extractEnergy(int toExtract, boolean simulate);

	int getEnergyStored();

	int getMaxEnergyStored();

	boolean canExtract();

	boolean canReceive();
}
