package forestry.core.content.tools;

import forestry.api.ForestryTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

public final class ToolTier {
	// bronze sits between iron and stone, so it uses the iron tag for blocks it cannot harvest
	public static final ToolMaterial SURVIVALIST = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 200, 7.0F, 2.5F, 10, ForestryTags.Items.INGOTS_BRONZE);

	private ToolTier() {
	}
}
