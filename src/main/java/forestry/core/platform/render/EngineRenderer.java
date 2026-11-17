package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import forestry.api.ForestryConstants;
import forestry.core.platform.config.Constants;
import forestry.core.platform.tile.TemperatureState;
import forestry.core.platform.util.RenderUtil;
import forestry.core.content.energy.blocks.EngineBlock;
import forestry.core.content.energy.tiles.EngineBlockEntity;
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
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class EngineRenderer implements BlockEntityRenderer<EngineBlockEntity, EngineRenderer.State> {
	private static final float[] ANGLE_MAP = new float[6];

	private enum Textures {
		BASE, PISTON, EXTENSION, TRUNK_HIGHEST, TRUNK_HIGHER, TRUNK_HIGH, TRUNK_MEDIUM, TRUNK_LOW
	}

	private final Identifier[] textures;
	private final ModelPart root;
	private final ModelPart boiler;
	private final ModelPart trunk;
	private final ModelPart piston;
	private final ModelPart extension;

	static {
		ANGLE_MAP[Direction.EAST.ordinal()] = -Mth.HALF_PI;
		ANGLE_MAP[Direction.NORTH.ordinal()] = -Mth.HALF_PI;
		ANGLE_MAP[Direction.WEST.ordinal()] = Mth.HALF_PI;
		ANGLE_MAP[Direction.SOUTH.ordinal()] = Mth.HALF_PI;
		ANGLE_MAP[Direction.UP.ordinal()] = 0;
		ANGLE_MAP[Direction.DOWN.ordinal()] = Mth.PI;
	}

	public EngineRenderer(BlockEntityRendererProvider.Context ctx, String baseTexture) {
		this(ctx.bakeLayer(ForestryModelLayers.ENGINE_LAYER), baseTexture);
	}

	private EngineRenderer(ModelPart root, String baseTexture) {
		this.root = root;
		this.boiler = root.getChild("boiler");
		this.trunk = root.getChild("trunk");
		this.piston = root.getChild("piston");
		this.extension = root.getChild("extension");

		this.textures = new Identifier[]{
			ForestryConstants.forestry(baseTexture + "base.png"),
			ForestryConstants.forestry(baseTexture + "piston.png"),
			ForestryConstants.forestry(baseTexture + "extension.png"),
			ForestryConstants.forestry(Constants.TEXTURE_PATH_BLOCK + "/engine_trunk_highest.png"),
			ForestryConstants.forestry(Constants.TEXTURE_PATH_BLOCK + "/engine_trunk_higher.png"),
			ForestryConstants.forestry(Constants.TEXTURE_PATH_BLOCK + "/engine_trunk_high.png"),
			ForestryConstants.forestry(Constants.TEXTURE_PATH_BLOCK + "/engine_trunk_medium.png"),
			ForestryConstants.forestry(Constants.TEXTURE_PATH_BLOCK + "/engine_trunk_low.png"),
		};
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("boiler", CubeListBuilder.create().texOffs(0, 0)
			.addBox(0, 0, 0, 16, 6, 16), PartPose.offset(0, 0, 0));
		root.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0, 0)
			.addBox(0, 0, 0, 8, 12, 8), PartPose.offset(4, 4, 4));
		root.addOrReplaceChild("piston", CubeListBuilder.create().texOffs(0, 0)
			.addBox(0, 0, 0, 12, 4, 12), PartPose.offset(2, 6, 2));
		root.addOrReplaceChild("extension", CubeListBuilder.create().texOffs(0, 0)
			.addBox(0, 0, 0, 10, 2, 10), PartPose.offset(3, 5, 3));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EngineBlockEntity engine, State state, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(engine, state, partialTick, cameraPos, breakProgress);
		state.orientation = engine.getBlockState().getValue(EngineBlock.VERTICAL_FACING);
		state.step = getPistonStep(engine, partialTick);
		state.temperature = engine.hasLevel() ? engine.getTemperatureState() : TemperatureState.COOL;
	}

	@Override
	public void submit(State state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		submit(stack, collector, state.orientation, state.step, state.temperature, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
	}

	private void submit(PoseStack stack, SubmitNodeCollector collector, Direction orientation, float step, TemperatureState temperature, int light, int overlay, int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		stack.pushPose();

		// apply direction rotation
		stack.translate(0.5, 0.5, 0.5);
		switch (orientation) {
			case EAST, WEST, DOWN -> stack.rotate(Axis.ZP.rotation(ANGLE_MAP[orientation.ordinal()]));
			default -> stack.rotate(Axis.XP.rotation(ANGLE_MAP[orientation.ordinal()]));
		}
		stack.translate(-0.5, -0.5, -0.5);

		// render base
		RenderUtil.submitPart(collector, stack, this.boiler, this.textures[Textures.BASE.ordinal()], light, overlay, outlineColor, breakProgress);

		// render piston with smooth lerp
		float tfactor = step / 16;
		stack.translate(0, tfactor, 0);
		RenderUtil.submitPart(collector, stack, this.piston, this.textures[Textures.PISTON.ordinal()], light, overlay, outlineColor, breakProgress);
		stack.translate(0, -tfactor, 0);

		// render trunk with color based on heat
		Identifier texture = switch (temperature) {
			case OVERHEATING -> this.textures[Textures.TRUNK_HIGHEST.ordinal()];
			case RUNNING_HOT -> this.textures[Textures.TRUNK_HIGHER.ordinal()];
			case OPERATING_TEMPERATURE -> this.textures[Textures.TRUNK_HIGH.ordinal()];
			case WARMED_UP -> this.textures[Textures.TRUNK_MEDIUM.ordinal()];
			default -> this.textures[Textures.TRUNK_LOW.ordinal()];
		};
		RenderUtil.submitPart(collector, stack, this.trunk, texture, light, overlay, outlineColor, breakProgress);

		// render piston sleeve
		float chamberf = 2F / 16F;
		if (step > 0) {
			for (int i = 0; i <= step + 2; i += 2) {
				RenderUtil.submitPart(collector, stack, this.extension, this.textures[Textures.EXTENSION.ordinal()], light, overlay, outlineColor, breakProgress);
				stack.translate(0, chamberf, 0);
			}
		}

		stack.popPose();
	}

	private static float getPistonStep(EngineBlockEntity engine, float partialTick) {
		float progress;

		if (engine.hasLevel()) {
			progress = engine.progress;
			if (engine.stagePiston != 0) {
				float smoothing = engine.pistonSpeedServer * partialTick;
				progress = (progress + smoothing);
			}
		} else {
			progress = Special.PROGRESS;
		}

		return getPistonStep(progress);
	}

	private static float getPistonStep(float progress) {
		if (progress > 0.5f) {
			return 6f - (progress - 0.5f) * 2f * 6F;
		} else {
			return progress * 2f * 6f;
		}
	}

	public static class State extends BlockEntityRenderState {
		public Direction orientation = Direction.UP;
		public float step;
		public TemperatureState temperature = TemperatureState.COOL;
	}

	// the item form, drawn as the old BEWLR drew an engine without a level
	public record Special(EngineRenderer renderer) implements NoDataSpecialModelRenderer {
		private static final float PROGRESS = 0.25f;

		@Override
		public void submit(PoseStack stack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
			this.renderer.submit(stack, collector, Direction.UP, getPistonStep(PROGRESS), TemperatureState.COOL, light, overlay, outlineColor, null);
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
				return new Special(new EngineRenderer(ctx.entityModelSet().bakeLayer(ForestryModelLayers.ENGINE_LAYER), this.texture));
			}
		}
	}
}
