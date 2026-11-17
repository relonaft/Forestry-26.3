package forestry.core.platform.models.baker;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.Quadrant;
import forestry.core.platform.util.ResourceUtil;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;


/**
 * A model baker to make custom block models with more than one texture layer.
 */
//Todo: Test if baker can be replaced with model loaders
public final class ModelBaker {
	// baked at render time, after the vanilla baker and its interner are gone
	private static final net.minecraft.client.resources.model.ModelBaker.Interner INTERNER = new net.minecraft.client.resources.model.ModelBaker.Interner() {
		@Override
		public Vector3fc vector(Vector3fc vector) {
			return vector;
		}

		@Override
		public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
			return material;
		}
	};
	private static final Vector3f POS_FROM = new Vector3f(0.0F, 0.0F, 0.0F);
	private static final Vector3f POS_TO = new Vector3f(16.0F, 16.0F, 16.0F);
	private static final CuboidFace.UVs FULL_FACE = new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F);

	private final List<ModelBakerFace> faces = new ArrayList<>();

	private TextureAtlasSprite particleSprite = ResourceUtil.getMissingTexture();

	private int colorIndex = -1;

	public ModelBaker addBlockModel(TextureAtlasSprite[] textures, int colorIndex) {
		this.colorIndex = colorIndex;

		for (Direction facing : Direction.VALUES) {
			addFace(facing, textures[facing.ordinal()]);
		}
		return this;
	}

	public ModelBaker addBlockModel(TextureAtlasSprite texture, int colorIndex) {
		return addBlockModel(new TextureAtlasSprite[]{texture, texture, texture, texture, texture, texture}, colorIndex);
	}

	public ModelBaker addFace(Direction facing, TextureAtlasSprite sprite) {
		if (sprite != ResourceUtil.getMissingTexture()) {
            this.faces.add(new ModelBakerFace(facing, this.colorIndex, sprite));
		}
		return this;
	}

	public ModelBakerModel bake(boolean flip) {
		ModelState modelRotation = BlockModelRotation.IDENTITY;

		if (flip) {
			modelRotation = BlockModelRotation.get(Quadrant.R180.rotationY);
		}

		QuadCollection.Builder quads = new QuadCollection.Builder();
		int tintLayers = 0;

		for (ModelBakerFace face : this.faces) {
			Direction facing = face.face;
			// the sprite picks the render layer: cutout for sprites with holes
			Transparency transparency = face.spite.transparency();
			BakedQuad.MaterialInfo material = BakedQuad.MaterialInfo.of(new Material.Baked(face.spite, false), transparency, face.colorIndex, null, 0, true);
			BakedQuad quad = FaceBakery.bakeQuad(INTERNER, POS_FROM, POS_TO, FULL_FACE, Quadrant.R0, material, facing, modelRotation, null);

			quads.addCulledFace(facing, quad);
			tintLayers = Math.max(tintLayers, face.colorIndex + 1);
		}

		return new ModelBakerModel(quads.build(), new Material.Baked(this.particleSprite, false), tintLayers);
	}

	public void setParticleSprite(TextureAtlasSprite particleSprite) {
        this.particleSprite = particleSprite;
	}
}
