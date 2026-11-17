package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import forestry.core.content.escritoire.EscritoireBlockEntity;
import forestry.core.platform.util.RenderUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class EscritoireRenderer implements BlockEntityRenderer<EscritoireBlockEntity, DisplayStackRenderState> {
	private final ItemModelResolver itemModelResolver;

	public EscritoireRenderer(BlockEntityRendererProvider.Context ctx) {
		this.itemModelResolver = ctx.itemModelResolver();
	}

	@Override
	public DisplayStackRenderState createRenderState() {
		return new DisplayStackRenderState();
	}

	@Override
	public void extractRenderState(EscritoireBlockEntity escritoire, DisplayStackRenderState state, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(escritoire, state, partialTick, cameraPos, breakProgress);
		RenderUtil.extractDisplayStack(state, this.itemModelResolver, escritoire.getIndividualOnDisplay(), escritoire.getLevel(), partialTick);
	}

	@Override
	public void submit(DisplayStackRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.item.isEmpty()) {
			stack.pushPose();
			stack.translate(0.5, 0.9, 0.5);
			stack.scale(0.75f, 0.75f, 0.75f);
			RenderUtil.submitDisplayStack(stack, collector, state);
			stack.popPose();
		}
	}
}
