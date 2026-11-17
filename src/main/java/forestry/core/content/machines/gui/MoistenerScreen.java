package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.TitledForestryScreen;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.machines.tiles.MoistenerBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MoistenerScreen extends TitledForestryScreen<MoistenerMenu> {
	private final MoistenerBlockEntity tile;

	public MoistenerScreen(MoistenerMenu container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/moistener.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new TankWidget(this.widgetManager, 16, 16, 0));
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseY, int mouseX) {
		super.extractBg(graphics, partialTicks, mouseY, mouseX);

		// Mycelium production progress
		if (this.tile.isProducing()) {
			int i1 = this.tile.getProductionProgressScaled(16);
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 124, this.topPos + 36, 176, 74, 16 - i1, 16, 256, 256);
		}

		// Resource consumption progress
		if (this.tile.isWorking()) {
			int i1 = this.tile.getConsumptionProgressScaled(54);
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 93, this.topPos + 18 + i1, 176, 92 + i1, 29, 54 - i1, 256, 256);
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("moistener");
	}
}
