package forestry.core.platform.models;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.core.platform.transfer.FluidUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// fixes issue in the DynamicFluidContainerModel where fluids have edges
public class FluidContainerModel implements ItemModel {
	private static final ModelDebugName DEBUG_NAME = () -> "FluidContainerModel";
	// the old model files took their display from neoforge:item/default, which copies this model
	private static final Identifier TRANSFORMS_MODEL = Identifier.withDefaultNamespace("item/generated");

	private final Unbaked unbaked;
	private final BakingContext context;
	private final Matrix4fc transformation;
	private final ItemTransforms transforms;
	private final Map<Fluid, ItemModel> cache = new IdentityHashMap<>();

	private FluidContainerModel(Unbaked unbaked, BakingContext context, Matrix4fc transformation) {
		this.unbaked = unbaked;
		this.context = context;
		this.transformation = transformation;
		this.transforms = context.blockModelBaker().getModel(TRANSFORMS_MODEL).getTopTransforms();
	}

	// Note: The fluid mask is ignored, the fluid element is always from (4, 2) to (12, 14).
	private ItemModel bake(Fluid fluid) {
		ModelBaker baker = this.context.blockModelBaker();
		MaterialBaker materials = baker.materials();
		DynamicFluidContainerModel.Textures textures = this.unbaked.textures;
		boolean coverIsMask = this.unbaked.coverIsMask;

		Material.Baked baseSprite = textures.base().map(material -> materials.get(material, DEBUG_NAME)).orElse(null);
		Material.Baked fluidSprite = fluid != Fluids.EMPTY ? Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).stillMaterial() : null;
		Material.Baked coverSprite = textures.cover().isPresent() && (!coverIsMask || baseSprite != null) ? materials.get(textures.cover().get(), DEBUG_NAME) : null;

		Material.Baked particleSprite = fluidSprite;
		if (particleSprite == null && baseSprite != null) {
			particleSprite = baseSprite;
		} else if (!coverIsMask) {
			particleSprite = coverSprite;
		}

		// one sub model per layer, a layer cannot mix the block and the item atlas
		List<ItemModel> layers = new ArrayList<>();
		ModelRenderProperties properties = new ModelRenderProperties(false, particleSprite, this.transforms);

		if (baseSprite != null) {
			QuadCollection quads = baker.compute(new ItemModelGenerator.ItemLayerKey(baseSprite, BlockModelRotation.IDENTITY, 0));
			layers.add(new CuboidItemModelWrapper(List.of(), quads, properties, this.transformation));
		}

		// Fluid layer
		if (textures.fluid().isPresent() && fluidSprite != null) {
			boolean emissive = this.unbaked.applyFluidLuminosity && fluid.getFluidType().getLightLevel() > 0;
			// no edges
			QuadCollection quads = make2dQuads(baker, fluidMaterial(fluidSprite, emissive), 4, 2, 12, 14, -0.002f);
			// tint index 1 of the container item is the colour of the contained fluid
			layers.add(new CuboidItemModelWrapper(List.of(new ForestryItemTint(1)), quads, properties, this.transformation));
		}

		if (coverSprite != null) {
			Material.Baked sprite = coverIsMask ? baseSprite : coverSprite;
			if (sprite != null) {
				// no edges
				BakedQuad.MaterialInfo material = BakedQuad.MaterialInfo.of(sprite, transparency(sprite), -1, null, 0, true);
				QuadCollection quads = make2dQuads(baker, material, 0, 0, 16, 16, 0.002f);
				layers.add(new CuboidItemModelWrapper(List.of(), quads, properties, this.transformation));
			}
		}

		return new CompositeModel(layers);
	}

	private static Transparency transparency(Material.Baked material) {
		return material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency();
	}

	private static BakedQuad.MaterialInfo fluidMaterial(Material.Baked material, boolean emissive) {
		Transparency transparency = transparency(material);
		if (!emissive) {
			return BakedQuad.MaterialInfo.of(material, transparency, 0, null, 0, true);
		}
		// glowing fluids are drawn at full brightness, with the unlit render types
		TextureAtlasSprite sprite = material.sprite();
		Identifier atlas = sprite.atlasLocation();
		if (transparency.hasTranslucent()) {
			return new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.TRANSLUCENT, NeoForgeRenderTypes.getItemTranslucentUnlit(atlas), NeoForgeRenderTypes.getItemGlintTranslucentUnlit(atlas), NeoForgeRenderTypes.getItemGlintSpecialTranslucentUnlit(atlas), 0, Direction.UP, Level.MAX_BRIGHTNESS, false);
		}
		return new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.CUTOUT, NeoForgeRenderTypes.getItemCutoutUnlit(atlas), NeoForgeRenderTypes.getItemGlintCutoutUnlit(atlas), NeoForgeRenderTypes.getItemGlintSpecialCutoutUnlit(atlas), 0, Direction.UP, Level.MAX_BRIGHTNESS, false);
	}

	// front and back 2d faces
	public static QuadCollection make2dQuads(ModelBaker baker, BakedQuad.MaterialInfo material, float startX, float startY, float endX, float endY, float zOffset) {
		ModelBaker.Interner interner = baker.interner();
		BakedQuad.MaterialInfo interned = interner.materialInfo(material);
		Vector3f from = new Vector3f(startX, startY, 7.5f - zOffset);
		Vector3f to = new Vector3f(endX, endY, 8.5f + zOffset);

		QuadCollection.Builder quads = new QuadCollection.Builder();
		quads.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0, interned, Direction.SOUTH, BlockModelRotation.IDENTITY, null));
		quads.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, new CuboidFace.UVs(16.0F, 0.0F, 0.0F, 16.0F), Quadrant.R0, interned, Direction.NORTH, BlockModelRotation.IDENTITY, null));
		return quads.build();
	}

	@Override
	public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
		// not a fluid item apparently, or an empty container
		Fluid fluid = FluidUtil.getFluidContained(stack).map(FluidStack::getFluid).orElse(Fluids.EMPTY);

		this.cache.computeIfAbsent(fluid, this::bake).update(output, stack, resolver, displayContext, level, owner, seed);
	}

	public record Unbaked(DynamicFluidContainerModel.Textures textures, boolean coverIsMask, boolean applyFluidLuminosity) implements ItemModel.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			DynamicFluidContainerModel.Textures.CODEC.fieldOf("textures").forGetter(Unbaked::textures),
			Codec.BOOL.optionalFieldOf("cover_is_mask", true).forGetter(Unbaked::coverIsMask),
			Codec.BOOL.optionalFieldOf("apply_fluid_luminosity", true).forGetter(Unbaked::applyFluidLuminosity)
		).apply(instance, Unbaked::new));

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public ItemModel bake(BakingContext context, Matrix4fc transformation) {
			return new FluidContainerModel(this, context, transformation);
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			resolver.markDependency(TRANSFORMS_MODEL);
		}
	}
}
