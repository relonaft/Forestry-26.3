package forestry.core.content.energy;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.energy.client.EnergyClientHandler;
import forestry.core.content.energy.features.EnergyBlockEntities;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import forestry.core.platform.transfer.Transfers;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Consumer;
import forestry.api.ForestryDataMaps;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@ForestryModule
public class ModuleEnergy extends BlankForestryModule {
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyBlockEntities.CLOCKWORK_ENGINE.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyBlockEntities.BIOGAS_ENGINE.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyBlockEntities.PEAT_ENGINE.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		// Deviation from 1.20.1: capabilities are registered here instead of overriding getCapability on the tile.
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyBlockEntities.SOLAR_ENGINE.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyBlockEntities.COMBUSTION_ENGINE.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, EnergyBlockEntities.BIOGAS_ENGINE.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, EnergyBlockEntities.COMBUSTION_ENGINE.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
	}

	@Override
	public Identifier getId() {
		return ForestryModuleIds.ENERGY;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleEnergy::registerCapabilities);
		modBus.addListener(ModuleEnergy::registerDataMaps);
	}

	private static void registerDataMaps(RegisterDataMapTypesEvent event) {
		event.register(ForestryDataMaps.BIOGAS_FUELS);
		event.register(ForestryDataMaps.COMBUSTION_FUELS);
		event.register(ForestryDataMaps.COMBUSTION_COOLANTS);
		event.register(ForestryDataMaps.PEAT_FUELS);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new EnergyClientHandler());
	}
}
