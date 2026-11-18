package forestry.core.platform.client.book;

import forestry.core.platform.book.Book;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import javax.annotation.Nullable;
import java.util.List;

// several stacks take turns, one each second
final class ItemElement implements BookElement {
	private static final int SIZE = 16;

	private final int x;
	private final int y;
	private final float scale;
	private final List<ItemStack> stacks;
	@Nullable
	private final Book.Link link;

	ItemElement(int x, int y, float scale, List<ItemStack> stacks, @Nullable Book.Link link) {
		this.x = x;
		this.y = y;
		this.scale = scale;
		this.stacks = stacks;
		this.link = link;
	}

	ItemElement(int x, int y, List<ItemStack> stacks) {
		this(x, y, 1.0f, stacks, null);
	}

	private boolean contains(int pageX, int pageY) {
		return pageX >= this.x && pageY >= this.y && pageX < this.x + SIZE * this.scale && pageY < this.y + SIZE * this.scale;
	}

	@Override
	public void extract(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
		if (this.stacks.isEmpty()) {
			return;
		}
		ItemStack stack = this.stacks.get((int) (Util.getMillis() / 1000 % this.stacks.size()));

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(left + this.x, top + this.y);
		pose.scale(this.scale, this.scale);
		graphics.item(stack, 0, 0);
		graphics.itemDecorations(font, stack, 0, 0);
		pose.popMatrix();

		if (contains(mouseX - left, mouseY - top)) {
			graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
		}
	}

	@Nullable
	@Override
	public Book.Link linkAt(int x, int y) {
		return contains(x, y) ? this.link : null;
	}
}
