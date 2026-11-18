package forestry.core.platform.client.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix3x2f;

import java.util.List;
import java.util.Optional;

// a tank 16 wide, filled from the bottom up to amount / capacity
record FluidElement(int x, int y, int height, FluidStack fluid, int capacity) implements BookElement {
	private static final int WIDTH = 16;

	@Override
	public void extract(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
		if (this.fluid.isEmpty()) {
			return;
		}
		FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(this.fluid.getFluid().defaultFluidState());
		TextureAtlasSprite sprite = model.stillMaterial().sprite();
		FluidTintSource tintSource = model.fluidTintSource();
		int color = ARGB.opaque(tintSource == null ? 0xffffff : tintSource.colorAsStack(this.fluid));
		AbstractTexture atlas = Minecraft.getInstance().getTextureManager().getTexture(sprite.atlasLocation());

		int filled = Math.clamp((long) this.height * this.fluid.getAmount() / this.capacity, 1, this.height);
		int bottom = top + this.y + this.height;
		for (int done = 0; done < filled; done += 16) {
			// the top tile is cut
			int tile = Math.min(16, filled - done);
			float v0 = sprite.getV1() - tile / 16.0f * (sprite.getV1() - sprite.getV0());
			// a plain blit takes either custom uv or a tint, not both
			graphics.submitGuiElementRenderState(new BlitRenderState(
				RenderPipelines.GUI_TEXTURED,
				TextureSetup.singleTexture(atlas.getTextureView(), atlas.getSampler()),
				new Matrix3x2f(graphics.pose()),
				left + this.x, bottom - done - tile, left + this.x + WIDTH, bottom - done,
				sprite.getU0(), sprite.getU1(), v0, sprite.getV1(),
				color,
				graphics.peekScissorStack()
			));
		}

		int pageX = mouseX - left;
		int pageY = mouseY - top;
		if (pageX >= this.x && pageX < this.x + WIDTH && pageY >= this.y && pageY < this.y + this.height) {
			List<Component> lines = List.of(this.fluid.getHoverName(), Component.translatable("for.gui.tooltip.liquid.amount", this.fluid.getAmount(), this.capacity));
			graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
		}
	}
}
