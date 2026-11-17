package forestry.mail.gui;

import forestry.core.platform.gui.GuiUtil;
import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.ForestryScreen;
import forestry.core.platform.render.ColourProperties;
import forestry.mail.tradestation.TradeStationBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TradeStationScreen extends ForestryScreen<TradeStationMenu> {
	private final TradeStationBlockEntity tile;

	public TradeStationScreen(TradeStationMenu container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/trade_station.png", container, inv, title, 226, 220);
		this.tile = container.getTile();
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Component name = this.tile.getTitle();
		graphics.text(this.font, name, this.textLayout.getCenteredOffset(name), 6, GuiUtil.textColor(ColourProperties.INSTANCE.get("gui.mail.text")));

		Component receive = Component.translatable("for.gui.mail.receive");
		graphics.text(this.font, receive, this.textLayout.getCenteredOffset(receive, 70) + 51, 45, GuiUtil.textColor(ColourProperties.INSTANCE.get("gui.mail.text")));

		Component send = Component.translatable("for.gui.mail.send");
		graphics.text(this.font, send, this.textLayout.getCenteredOffset(send, 70) + 51, 99, GuiUtil.textColor(ColourProperties.INSTANCE.get("gui.mail.text")));

		super.extractLabels(graphics, mouseX, mouseY);
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		super.extractBg(graphics, partialTicks, mouseX, mouseY);

		graphics.text(this.font, this.menu.getAddress().getName(), this.leftPos + 19, this.topPos + 22, GuiUtil.textColor(ColourProperties.INSTANCE.get("gui.mail.text")));
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("trade.station");
		addOwnerLedger(this.tile);
	}
}
