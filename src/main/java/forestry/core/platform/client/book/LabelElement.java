package forestry.core.platform.client.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

// one line of text, shrunk when it is wider than its room
final class LabelElement implements BookElement {
	static final int HEADER_COLOR = 0xFF333333;

	private final Component text;
	private final int x;
	private final int y;
	private final int width;
	private final int color;
	private final boolean centered;
	private final boolean shadow;

	private LabelElement(Component text, int x, int y, int width, int color, boolean centered, boolean shadow) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.width = width;
		this.color = color;
		this.centered = centered;
		this.shadow = shadow;
	}

	static LabelElement header(Component text, int y) {
		return new LabelElement(text, 0, y, BookPage.WIDTH, HEADER_COLOR, true, false);
	}

	static LabelElement left(Component text, int x, int y, int width, int color, boolean shadow) {
		return new LabelElement(text, x, y, width, color, false, shadow);
	}

	@Override
	public void extract(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
		int textWidth = font.width(this.text);
		float scale = textWidth > this.width ? (float) this.width / textWidth : 1.0f;
		float drawnWidth = textWidth * scale;

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(left + this.x + (this.centered ? (this.width - drawnWidth) / 2.0f : 0.0f), top + this.y);
		pose.scale(scale, scale);
		graphics.text(font, this.text, 0, 0, this.color, this.shadow);
		pose.popMatrix();
	}
}
