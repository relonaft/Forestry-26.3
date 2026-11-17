package forestry.core.content.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import forestry.api.ForestryConstants;
import forestry.core.platform.util.RenderUtil;
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
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import forestry.core.platform.render.ForestryModelLayers;
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import java.util.function.Consumer;

// This is called Mill because it used to be for the Forester and Treetap blocks in older versions of Forestry
public class RainmakerRenderer implements BlockEntityRenderer<MillBlockEntity, RainmakerRenderer.State> {
	private enum Textures {PEDESTAL, EXTENSION, BLADE, CHARGE}

	private final Identifier[] textures;
	private final ModelPart root;
	private final ModelPart pedestal;
	private final ModelPart column;
	private final ModelPart extension;
	private final ModelPart blade;

	public RainmakerRenderer(BlockEntityRendererProvider.Context ctx, String baseTexture) {
		this(ctx.bakeLayer(ForestryModelLayers.MILL_LAYER), baseTexture);
	}

	private RainmakerRenderer(ModelPart root, String baseTexture) {
		this.root = root;
		this.pedestal = root.getChild(Textures.PEDESTAL.name());
		this.column = root.getChild(Textures.CHARGE.name());
		this.extension = root.getChild(Textures.EXTENSION.name());
		this.blade = root.getChild(Textures.BLADE.name());

		this.textures = new Identifier[11];

		this.textures[Textures.PEDESTAL.ordinal()] = ForestryConstants.forestry(baseTexture + "pedestal.png");
		this.textures[Textures.EXTENSION.ordinal()] = ForestryConstants.forestry(baseTexture + "extension.png");
		this.textures[Textures.BLADE.ordinal()] = ForestryConstants.forestry(baseTexture + "blade.png");

		for (int i = 0; i < 8; i++) {
			this.textures[Textures.CHARGE.ordinal() + i] = ForestryConstants.forestry(baseTexture + "column_" + i + ".png");
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild(Textures.PEDESTAL.name(), CubeListBuilder.create().texOffs(0, 0)
			.addBox(0f, 0f, 0f, 16, 1, 16), PartPose.offset(0, 0, 0));
		partdefinition.addOrReplaceChild(Textures.CHARGE.name(), CubeListBuilder.create().texOffs(0, 0)
			.addBox(0f, 0f, 0f, 4, 15, 4), PartPose.offset(6, 1, 6));
		partdefinition.addOrReplaceChild(Textures.EXTENSION.name(), CubeListBuilder.create().texOffs(0, 0)
			.addBox(0f, 0f, 0f, 14, 2, 2), PartPose.offset(1, 8, 7));
		partdefinition.addOrReplaceChild(Textures.BLADE.name(), CubeListBuilder.create().texOffs(0, 0)
			.addBox(0f, 0f, 0f, 1, 12, 8), PartPose.offset(10, 3, 4));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(MillBlockEntity mill, State state, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(mill, state, partialTick, cameraPos, breakProgress);
		state.charge = mill.charge;
		state.step = getBladeStep(mill, partialTick) / 16f;
	}

	@Override
	public void submit(State state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		submit(stack, collector, state.charge, state.step, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
	}

	private void submit(PoseStack stack, SubmitNodeCollector collector, int charge, float step, int light, int overlay, int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		stack.pushPose();

		// todo implement and add rotated voxel shape
		Direction orientation = Direction.SOUTH;//mill.getBlockState().getValue(BlockBase.FACING);
		RenderUtil.rotateByHorizontalDirection(stack, orientation);

		RenderUtil.submitPart(collector, stack, this.pedestal, this.textures[Textures.PEDESTAL.ordinal()], light, overlay, outlineColor, breakProgress);
		RenderUtil.submitPart(collector, stack, this.column, this.textures[Textures.CHARGE.ordinal() + charge], light, overlay, outlineColor, breakProgress);
		RenderUtil.submitPart(collector, stack, this.extension, this.textures[Textures.EXTENSION.ordinal()], light, overlay, outlineColor, breakProgress);

		Identifier bladeTexture = this.textures[Textures.BLADE.ordinal()];
		stack.pushPose();
		stack.translate(step, 0, 0);
		RenderUtil.submitPart(collector, stack, this.blade, bladeTexture, light, overlay, outlineColor, breakProgress);
		stack.popPose();
		stack.translate(1, 0, 1);
		stack.rotate(Axis.YP.rotation(Mth.PI));
		stack.translate(step, 0, 0);
		RenderUtil.submitPart(collector, stack, this.blade, bladeTexture, light, overlay, outlineColor, breakProgress);

		stack.popPose();
	}

	private static float getBladeStep(MillBlockEntity mill, float partialTick) {
		float progress;

		if (mill.hasLevel()) {
			progress = mill.progress;
			if (mill.stage != 0) {
				float smoothing = mill.speed * partialTick;
				progress = (progress + smoothing);
			}
		} else {
			progress = 0.0f;
		}

		if (progress > 0.5f) {
			return 3.99f - (progress - 0.5f) * 2f * 3.99f;
		} else {
			return progress * 2f * 3.99f;
		}
	}

	public static class State extends BlockEntityRenderState {
		public int charge;
		public float step;
	}

	// the item form, uncharged with the blades at rest
	public record Special(RainmakerRenderer renderer) implements NoDataSpecialModelRenderer {
		@Override
		public void submit(PoseStack stack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
			this.renderer.submit(stack, collector, 0, 0f, light, overlay, outlineColor, null);
		}

		@Override
		public void getExtents(Consumer<Vector3fc> output) {
			this.renderer.root.getExtentsForGui(new PoseStack(), output);
		}

		public record Unbaked(String texture) implements NoDataSpecialModelRenderer.Unbaked {
			public static final MapCodec<Unbaked> MAP_CODEC = Codec.STRING.fieldOf("texture").xmap(Unbaked::new, Unbaked::texture);

			@Override
			public MapCodec<Unbaked> type() {
				return MAP_CODEC;
			}

			@Override
			public Special bake(SpecialModelRenderer.BakingContext ctx) {
				return new Special(new RainmakerRenderer(ctx.entityModelSet().bakeLayer(ForestryModelLayers.MILL_LAYER), this.texture));
			}
		}
	}
}
