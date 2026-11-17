package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.TitledForestryScreen;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.machines.tiles.StillBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StillScreen extends TitledForestryScreen<StillMenu> {
	private final StillBlockEntity tile;

	public StillScreen(StillMenu container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/still.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new TankWidget(this.widgetManager, 35, 15, 0));
        this.widgetManager.add(new TankWidget(this.widgetManager, 125, 15, 1));
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseY, int mouseX) {
		super.extractBg(graphics, partialTicks, mouseY, mouseX);

		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 81, this.topPos + 57, 176, 60, 14, 14, 256, 256);

		if (this.tile.getWorkCounter() > 0) {
			int massRemaining = this.tile.getProgressScaled(16);
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 84, this.topPos + 17 + massRemaining, 176, 74 + massRemaining, 4, 17 - massRemaining, 256, 256);
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("still");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
