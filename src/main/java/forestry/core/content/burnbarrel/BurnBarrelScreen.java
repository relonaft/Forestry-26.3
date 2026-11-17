package forestry.core.content.burnbarrel;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.TitledForestryScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BurnBarrelScreen extends TitledForestryScreen<BurnBarrelMenu> {
	private final BurnBarrelBlockEntity tile;

	public BurnBarrelScreen(BurnBarrelMenu menu, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/burn_barrel.png", menu, inventory, title, 176, 202);
		this.tile = menu.getTile();
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		super.extractBg(graphics, partialTicks, mouseX, mouseY);

		// Display the progress arrows
		if (this.tile.getAshProductionTimer() > 0) {
			int ashProgress = this.tile.getValueScaled(this.tile.getAshProductionTimer(), BurnBarrelBlockEntity.ASH_PRODUCTION_TIME, 18);
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile,
				this.leftPos + 68, this.topPos + 65, // where to draw thing
				176, 14, // coordinates of thing to be drawn
				38, ashProgress, 256, 256); // width and height of thing to be drawn
		}

		// Display the burning flame icon
		if (this.tile.getBurnTime() > 0) {
			int burnTimeRemaining = this.tile.getValueScaled(this.tile.getBurnTime(), this.tile.getCurrentMaxBurnTime(), 14);
			burnTimeRemaining = Math.min(burnTimeRemaining + 1, 14);
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile,
				this.leftPos + 80, this.topPos + 67 + (14 - burnTimeRemaining), // where to draw thing
				176, 14 - burnTimeRemaining, // coordinates of thing to be drawn
				14, burnTimeRemaining, 256, 256); // width and height of thing to be drawn
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("burn_barrel");
	}
}
