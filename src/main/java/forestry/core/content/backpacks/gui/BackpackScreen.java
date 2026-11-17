package forestry.core.content.backpacks.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.ForestryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BackpackScreen extends ForestryScreen<BackpackMenu> {
	public BackpackScreen(BackpackMenu container, Inventory inv, Component title) {
		super(getTextureString(container), container, inv, title, DEFAULT_IMAGE_WIDTH, container.getSize() == BackpackMenu.Size.T2 ? 192 : DEFAULT_IMAGE_HEIGHT);
	}

	private static String getTextureString(BackpackMenu container) {
		BackpackMenu.Size size = container.getSize();
		if (size == BackpackMenu.Size.T2) {
			return Constants.TEXTURE_PATH_GUI + "/backpack_t2.png";
		}
		return Constants.TEXTURE_PATH_GUI + "/backpack.png";
	}

	@Override
	protected void addLedgers() {

	}
}
