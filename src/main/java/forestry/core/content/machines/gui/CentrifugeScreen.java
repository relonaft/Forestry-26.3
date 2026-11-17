package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.TitledForestryScreen;
import forestry.core.platform.gui.widgets.SocketWidget;
import forestry.core.content.machines.tiles.CentrifugeBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CentrifugeScreen extends TitledForestryScreen<CentrifugeMenu> {
	private final CentrifugeBlockEntity tile;

	public CentrifugeScreen(CentrifugeMenu container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/centrifugesocket2.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new SocketWidget(this.widgetManager, 79, 37, this.tile, 0));
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseY, int mouseX) {
		super.extractBg(graphics, partialTicks, mouseY, mouseX);

		int progress = this.tile.getProgressScaled(16);
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 43, this.topPos + 36 + 17 - progress, 176, 17 - progress, 4, progress, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 67, this.topPos + 36 + 17 - progress, 176, 17 - progress, 4, progress, 256, 256);
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("centrifuge");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
