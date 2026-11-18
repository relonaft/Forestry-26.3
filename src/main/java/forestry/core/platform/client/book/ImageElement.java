package forestry.core.platform.client.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

// a texture region, the size and the texture size are in texture pixels before the scale
record ImageElement(Identifier texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight, float scale) implements BookElement {
	ImageElement(Identifier texture, int x, int y, int u, int v, int width, int height) {
		this(texture, x, y, u, v, width, height, 256, 256, 1.0f);
	}

	@Override
	public void extract(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(left + this.x, top + this.y);
		pose.scale(this.scale, this.scale);
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, 0, 0, this.u, this.v, this.width, this.height, this.textureWidth, this.textureHeight);
		pose.popMatrix();
	}
}
