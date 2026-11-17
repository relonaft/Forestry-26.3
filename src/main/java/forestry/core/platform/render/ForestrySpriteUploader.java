package forestry.core.platform.render;

import forestry.api.ForestryConstants;
import forestry.api.client.ForestrySprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

// the forestry gui atlas, stitched by the vanilla atlas manager since TextureAtlasHolder is gone
public class ForestrySpriteUploader {
	public static final Identifier ATLAS_PATH = ForestryConstants.forestry("gui");
	public static final AtlasManager.AtlasConfig ATLAS_CONFIG = new AtlasManager.AtlasConfig(ForestrySprites.TEXTURE_ATLAS, ATLAS_PATH, false);

	public TextureAtlasSprite getSprite(Identifier location) {
		return Minecraft.getInstance().getAtlasManager().get(new SpriteId(ForestrySprites.TEXTURE_ATLAS, location));
	}
}
