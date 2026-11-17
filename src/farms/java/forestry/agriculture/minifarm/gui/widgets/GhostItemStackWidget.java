package forestry.agriculture.minifarm.gui.widgets;

import forestry.core.platform.gui.GuiUtil;
import forestry.api.core.tooltips.ToolTip;
import forestry.api.agriculture.HorizontalDirection;
import forestry.core.platform.gui.widgets.ItemStackWidget;
import forestry.core.platform.gui.widgets.WidgetManager;
import forestry.core.platform.render.ColourProperties;
import forestry.agriculture.minifarm.inventory.MinifarmInventory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class GhostItemStackWidget extends ItemStackWidget {
	private final Slot slot;

	public GhostItemStackWidget(WidgetManager widgetManager, int xPos, int yPos, ItemStack itemStack, Slot slot) {
		super(widgetManager, xPos, yPos, itemStack);
		this.slot = slot;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		if (!this.slot.hasItem()) {
			super.draw(graphics, startX, startY);
		}

		Component directionString = getDirectionString();
		if (directionString != null) {
			Font font = this.manager.minecraft.font;
			graphics.text(font, directionString, this.xPos + startX + 5, this.yPos + startY + 4, GuiUtil.textColor(ColourProperties.INSTANCE.get("gui.screen")));
		}

		// deviation: the half alpha no longer carries over to the items of the next ghost widgets
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.manager.gui.textureFile, this.xPos + startX, this.yPos + startY, 206, 0, 16, 16, 256, 256, ARGB.white(0.5f));
	}

	@Nullable
	private Component getDirectionString() {
		if (this.slot.getSlotIndex() >= MinifarmInventory.CONFIG.productionStart
			|| this.slot.getSlotIndex() < MinifarmInventory.CONFIG.productionStart + MinifarmInventory.CONFIG.productionCount) {
			return null;
		}
		int index = this.slot.getSlotIndex() % 4;
		Direction direction = HorizontalDirection.VALUES.get(index);
		String directionString = direction.getSerializedName();
		return Component.translatable("for.gui.planter." + directionString);
	}

	@Nullable
	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		return null;
	}
}
