package forestry.arboriculture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.entities.ForestryBoat;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import java.util.EnumMap;

import static forestry.api.ForestryConstants.forestry;

// one entity type covers every wood type, so the wood type picks a vanilla renderer per boat
public class ForestryBoatRenderer extends AbstractBoatRenderer {
	private static final ForestryWoodType DEFAULT_TYPE = ForestryWoodType.CAMELTHORN;

	private final EnumMap<ForestryWoodType, Renderer> boatRenderers;

	public ForestryBoatRenderer(EntityRendererProvider.Context ctx, boolean hasChest) {
		super(ctx, forestry(getTextureLocation(DEFAULT_TYPE, hasChest)));

		this.boatRenderers = new EnumMap<>(ForestryWoodType.class);
		for (ForestryWoodType type : ForestryWoodType.VALUES) {
			this.boatRenderers.put(type, new Renderer(ctx, createBoatModelLocation(type, hasChest)));
		}
	}

	private static String getTextureLocation(ForestryWoodType type, boolean hasChest) {
		return (hasChest ? "textures/entity/chest_boat/" : "textures/entity/boat/") + type.getSerializedName() + ".png";
	}

	// BoatRenderer derives the texture from this location, which gives the path of getTextureLocation
	public static ModelLayerLocation createBoatModelLocation(ForestryWoodType type, boolean hasChest) {
		return new ModelLayerLocation(forestry((hasChest ? "chest_boat/" : "boat/") + type.getSerializedName()), "main");
	}

	@Override
	public BoatRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(AbstractBoat boat, BoatRenderState state, float partialTick) {
		super.extractRenderState(boat, state, partialTick);
		((State) state).woodType = boat instanceof ForestryBoat forestryBoat ? forestryBoat.getWoodType() : DEFAULT_TYPE;
	}

	@Override
	public void submit(BoatRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		this.boatRenderers.get(((State) state).woodType).submit(state, stack, collector, camera);
	}

	@Override
	protected EntityModel<BoatRenderState> model() {
		return this.boatRenderers.get(DEFAULT_TYPE).model();
	}

	private static class State extends BoatRenderState {
		private ForestryWoodType woodType = DEFAULT_TYPE;
	}

	// widens model() so the outer class can reach it
	private static class Renderer extends BoatRenderer {
		private Renderer(EntityRendererProvider.Context ctx, ModelLayerLocation modelId) {
			super(ctx, modelId);
		}

		@Override
		protected EntityModel<BoatRenderState> model() {
			return super.model();
		}
	}
}
