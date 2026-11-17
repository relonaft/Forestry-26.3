package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import forestry.api.ForestryConstants;
import forestry.core.platform.block.BaseBlock;
import forestry.core.platform.config.Constants;
import forestry.core.platform.tile.NaturalistChestBlockEntity;
import forestry.core.platform.util.RenderUtil;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class NaturalistChestRenderer implements BlockEntityRenderer<NaturalistChestBlockEntity, NaturalistChestRenderer.State> {
	private static final String LID = "lid";
	private static final String BASE = "base";
	private static final String LOCK = "lock";

	private final ChestModel model;
	private final Identifier texture;

	public NaturalistChestRenderer(BlockEntityRendererProvider.Context ctx, String textureName) {
		this(ctx.bakeLayer(ForestryModelLayers.NATURALIST_CHEST_LAYER), textureName);
	}

	private NaturalistChestRenderer(ModelPart root, String textureName) {
		this.model = new ChestModel(root);
		this.texture = ForestryConstants.forestry(Constants.TEXTURE_PATH_BLOCK + "/" + textureName + ".png");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild(BASE, CubeListBuilder.create().texOffs(0, 19)
			.addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.offset(0, 0, 0));
		partdefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0)
			.addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0, 9.0F, 1.0F));
		partdefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0)
			.addBox(7.0F, -1.0F, 15.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0, 8.0F, 0));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(NaturalistChestBlockEntity chest, State state, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(chest, state, partialTick, cameraPos, breakProgress);
		state.facing = chest.getBlockState().getValue(BaseBlock.FACING);

		// calculate lid angle
		float prevLidAngle = chest.prevLidAngle;
		float lidAngle = chest.lidAngle;
		float angle = prevLidAngle + (lidAngle - prevLidAngle) * partialTick;
		angle = 1.0F - angle;
		angle = 1.0F - angle * angle * angle;
		state.rotation = -(angle * Mth.HALF_PI);
	}

	@Override
	public void submit(State state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		stack.pushPose();

		RenderUtil.rotateByHorizontalDirection(stack, state.facing);

		RenderType renderType = this.model.renderType(this.texture);
		collector.submitModel(this.model, state.rotation, stack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		if (state.breakProgress != null) {
			collector.order(1).submitCrumblingOverlay(this.model, state.rotation, stack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
		}

		stack.popPose();
	}

	public static class State extends BlockEntityRenderState {
		public Direction facing = Direction.SOUTH;
		public float rotation;
	}

	// the lid angle is a model state because parts are drawn after every chest of the frame was submitted
	private static class ChestModel extends Model<Float> {
		private final ModelPart lid;
		private final ModelPart lock;

		private ChestModel(ModelPart root) {
			super(root, RenderTypes::entityCutoutCull);
			this.lid = root.getChild(LID);
			this.lock = root.getChild(LOCK);
		}

		@Override
		public void setupAnim(Float rotation) {
			super.setupAnim(rotation);
			this.lid.xRot = rotation;
			this.lock.xRot = rotation;
		}
	}

	// the item form, a closed chest that faces south
	public record Special(NaturalistChestRenderer renderer) implements NoDataSpecialModelRenderer {
		@Override
		public void submit(PoseStack stack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
			collector.submitModel(this.renderer.model, 0f, stack, this.renderer.model.renderType(this.renderer.texture), light, overlay, outlineColor);
		}

		@Override
		public void getExtents(Consumer<Vector3fc> output) {
			this.renderer.model.setupAnim(0f);
			this.renderer.model.root().getExtentsForGui(new PoseStack(), output);
		}

		public record Unbaked(String texture) implements NoDataSpecialModelRenderer.Unbaked {
			public static final MapCodec<Unbaked> MAP_CODEC = Codec.STRING.fieldOf("texture").xmap(Unbaked::new, Unbaked::texture);

			@Override
			public MapCodec<Unbaked> type() {
				return MAP_CODEC;
			}

			@Override
			public Special bake(SpecialModelRenderer.BakingContext ctx) {
				return new Special(new NaturalistChestRenderer(ctx.entityModelSet().bakeLayer(ForestryModelLayers.NATURALIST_CHEST_LAYER), this.texture));
			}
		}
	}
}
