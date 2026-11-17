package forestry.agriculture.client;

import forestry.agriculture.multifarm.gui.MultifarmScreen;
import forestry.api.client.IClientModuleHandler;
import forestry.agriculture.features.FarmingMenuTypes;
import forestry.agriculture.minifarm.gui.MinifarmScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class FarmingClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(FarmingClientHandler::registerMenuScreens);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(FarmingMenuTypes.MULTIFARM.menuType(), MultifarmScreen::new);
		event.register(FarmingMenuTypes.MINIFARM.menuType(), MinifarmScreen::new);
	}
}
