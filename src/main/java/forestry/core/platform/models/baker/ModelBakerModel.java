package forestry.core.platform.models.baker;

import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ModelBakerModel implements BlockStateModelPart {
	private boolean isAmbientOcclusion;
	private final Material.Baked particleMaterial;
	private final QuadCollection quads;
	private final int tintLayers;
	// the item renderer needs the quads split by blending
	private final Supplier<ItemQuads> itemQuads;
	private final Supplier<Vector3fc[]> extents;

	ModelBakerModel(QuadCollection quads, Material.Baked particleMaterial, int tintLayers) {
		this.quads = quads;
		this.particleMaterial = particleMaterial;
		this.tintLayers = tintLayers;
		this.isAmbientOcclusion = false;
		this.itemQuads = Suppliers.memoize(() -> ItemQuads.split(quads.getAll()));
		this.extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads.getAll()));
	}

	public void setAmbientOcclusion(boolean ambientOcclusion) {
		this.isAmbientOcclusion = ambientOcclusion;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return this.isAmbientOcclusion;
	}

	@Override
	public Material.Baked particleMaterial() {
		return this.particleMaterial;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable Direction side) {
		return this.quads.getQuads(side);
	}

	@Override
	public int materialFlags() {
		return this.quads.materialFlags();
	}

	public ItemQuads itemQuads() {
		return this.itemQuads.get();
	}

	public Supplier<Vector3fc[]> extents() {
		return this.extents;
	}

	// highest tint index plus one
	public int tintLayers() {
		return this.tintLayers;
	}
}
