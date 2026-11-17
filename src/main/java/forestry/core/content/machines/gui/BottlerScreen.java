package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.TitledForestryScreen;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.platform.render.ColourProperties;
import forestry.core.content.machines.tiles.BottlerBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Matrix3x2fStack;

public class BottlerScreen extends TitledForestryScreen<BottlerMenu> {
	private final BottlerBlockEntity tile;

	public BottlerScreen(BottlerMenu container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/bottler.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new TankWidget(this.widgetManager, 80, 14, 0));
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseY, int mouseX) {
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

		//RenderHelper.enableGUIStandardItemLighting(); TODO Gui Light
		// RenderSystem.disableLighting();
		// RenderSystem.enableRescaleNormal();

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		{
			pose.translate(this.leftPos, this.topPos);
			drawWidgets(graphics);
		}
		pose.popMatrix();

		Component name = this.tile.getTitle();
        this.textLayout.line = 5;
        this.textLayout.drawCenteredLine(graphics, name, 0, ColourProperties.INSTANCE.get("gui.title"));

		BottlerBlockEntity bottler = this.tile;
		int progressArrow = bottler.getProgressScaled(22);
		if (progressArrow > 0) {
			if (bottler.isFillRecipe) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 108, this.topPos + 35, 177, 74, progressArrow, 16, 256, 256);
			} else {
				graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 46, this.topPos + 35, 177, 74, progressArrow, 16, 256, 256);
			}
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("bottler");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
