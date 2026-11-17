package forestry.core.platform.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.render.DisplayStackRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

import javax.annotation.Nullable;
import java.awt.*;

public class RenderUtil {
	// only read while a render state is extracted, which happens on the render thread
	private static final ItemStackRenderState UNTRANSFORMED_ITEM = new ItemStackRenderState();

	// requires external push/pop
	public static void rotateByHorizontalDirection(PoseStack stack, Direction facing) {
		if (facing != Direction.SOUTH) {
			stack.translate(0.5, 0.5, 0.5);
			stack.rotate(Axis.YP.rotationDegrees(-facing.toYRot()));
			stack.translate(-0.5, -0.5, -0.5);
		}
	}

	// the old RenderType.entityCutout culled, which is entityCutoutCull now
	public static void submitPart(SubmitNodeCollector collector, PoseStack stack, ModelPart part, Identifier texture, int light, int overlay, int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		RenderType renderType = RenderTypes.entityCutoutCull(texture);
		collector.submitModelPart(part, stack, renderType, light, overlay, null, -1, outlineColor);
		if (breakProgress != null) {
			collector.order(1).submitCrumblingOverlay(part, stack, renderType, light, overlay, -1, breakProgress);
		}
	}

	public static void extractDisplayStack(DisplayStackRenderState state, ItemModelResolver resolver, ItemStack displayStack, @Nullable Level level, float partialTick) {
		if (displayStack.isEmpty() || level == null) {
			state.item.clear();
			return;
		}
		resolver.updateForTopItem(state.item, displayStack, ItemDisplayContext.GROUND, level, null, 1);
		// deviation: 26.3 hides the ground transform, its scale is measured from the model bounds
		resolver.updateForTopItem(UNTRANSFORMED_ITEM, displayStack, ItemDisplayContext.NONE, level, null, 1);
		double height = UNTRANSFORMED_ITEM.getModelBoundingBox().getYsize();
		// a model without bounds falls back to the ground scale of item/generated
		state.groundScale = height > 0 ? (float) (state.item.getModelBoundingBox().getYsize() / height) : 0.5f;
		state.smoothTick = ((float) (int) level.getGameTime()) + partialTick;
	}

	// requires external push/pop
	public static void submitDisplayStack(PoseStack stack, SubmitNodeCollector collector, DisplayStackRenderState state) {
		float f1 = Mth.sin(state.smoothTick / 10.0f) * 0.1f + 0.1f;
		stack.translate(0, f1 + 0.25f * state.groundScale, 0);
		stack.rotate(Axis.YP.rotation(state.smoothTick / 20f));

		state.item.submit(stack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
	}

	// replaces LevelRenderer.renderLineBox, which 26.3 does not have
	public static void lineBox(PoseStack.Pose pose, VertexConsumer buffer, float min, float max, int color, float width) {
		for (int corner = 0; corner < 4; corner++) {
			float a = (corner & 1) == 0 ? min : max;
			float b = (corner & 2) == 0 ? min : max;
			line(pose, buffer, min, a, b, max, a, b, color, width);
			line(pose, buffer, a, min, b, a, max, b, color, width);
			line(pose, buffer, a, b, min, a, b, max, color, width);
		}
	}

	// axis aligned lines only
	private static void line(PoseStack.Pose pose, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width) {
		float nx = x2 > x1 ? 1 : 0;
		float ny = y2 > y1 ? 1 : 0;
		float nz = z2 > z1 ? 1 : 0;
		buffer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
		buffer.addVertex(pose, x2, y2, z2).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
	}

	public static int getFluidColor(Fluid fluid) {
		// a fluid model without a tint source is white, the old default
		FluidTintSource tint = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).fluidTintSource();
		int color = tint != null ? tint.color(fluid.defaultFluidState()) : 0xFFFFFFFF;
		ForestryFluids definition = ForestryFluids.getFluidDefinition(fluid);
		if (color < 0) {
			color = 0x0000ff;
			if (definition != null) {
				color = definition.getParticleColor();
			}
		}
		return color;
	}

	// this usage of Color is fine since it's client-only
	public static Color getRainbowColor(long time, float partialTicks) {
		return Color.getHSBColor((180 * Mth.sin((time + partialTicks) / 30.0f) - 180) / 360.0f, 0.5f, 0.8f);
	}

	/**
	 * Pulsing alpha in {@code [0.2, 1.0]} for the flashing-white spectacle outline drawn around the parts of an
	 * unformed multiblock (a faster sine than the rainbow hue so the two styles read differently at a glance).
	 */
	public static float getFlashingAlpha(long time, float partialTicks) {
		return 0.6f + 0.4f * Mth.sin((time + partialTicks) / 8.0f);
	}

	// VANILLA COPY
	public static int getYImage(Button button) {
		int i = 1;
		if (!button.active) {
			i = 0;
		} else if (button.isHoveredOrFocused()) {
			i = 2;
		}

		return i;
	}
}
