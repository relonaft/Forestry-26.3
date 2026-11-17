package forestry.core.content.machines;

import forestry.api.ForestryDataMaps;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IPacketRegistry;
import forestry.core.content.machines.client.FactoryClientHandler;
import forestry.core.content.machines.features.FactoryBlockEntities;
import forestry.core.content.machines.network.packets.PacketRecipeTransferRequest;
import forestry.core.content.machines.network.packets.PacketRecipeTransferUpdate;
import forestry.core.platform.network.PacketIdClient;
import forestry.core.platform.network.PacketIdServer;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import forestry.core.platform.transfer.Transfers;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import java.util.function.Consumer;

@ForestryModule
public class ModuleFactory extends BlankForestryModule {
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.BOTTLER.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.CARPENTER.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.CENTRIFUGE.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.FABRICATOR.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.FERMENTER.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.RAINMAKER.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.MOISTENER.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.SMELTER.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.SQUEEZER.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryBlockEntities.STILL.tileType(), (tile, side) -> Transfers.exposeItems(tile.getItemHandler(side)));

		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.BOTTLER.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.CARPENTER.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.CENTRIFUGE.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.FABRICATOR.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.FERMENTER.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.SMELTER.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.SQUEEZER.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryBlockEntities.STILL.tileType(), (tile, side) -> Transfers.exposeEnergy(tile.getEnergyHandler(side)));

		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.BOTTLER.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.CARPENTER.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.FABRICATOR.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.FERMENTER.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.MOISTENER.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.SQUEEZER.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryBlockEntities.STILL.tileType(), (tile, side) -> Transfers.exposeFluid(tile.getFluidHandler(side)));
	}

	@Override
	public Identifier getId() {
		return ForestryModuleIds.FACTORY;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleFactory::registerCapabilities);
		modBus.addListener(ModuleFactory::registerDataMaps);
	}

	private static void registerDataMaps(RegisterDataMapTypesEvent event) {
		event.register(ForestryDataMaps.FERMENTER_FUELS);
		event.register(ForestryDataMaps.MOISTENER_FUELS);
		event.register(ForestryDataMaps.RAINMAKER_FUELS);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new FactoryClientHandler());
	}

	@Override
	public void registerPackets(IPacketRegistry registry) {
		registry.serverbound(PacketIdServer.RECIPE_TRANSFER_REQUEST, PacketRecipeTransferRequest::encode, PacketRecipeTransferRequest::decode, PacketRecipeTransferRequest::handle);
		registry.clientbound(PacketIdClient.RECIPE_TRANSFER_UPDATE, PacketRecipeTransferUpdate::encode, PacketRecipeTransferUpdate::decode, PacketRecipeTransferUpdate::handle);
	}
}
