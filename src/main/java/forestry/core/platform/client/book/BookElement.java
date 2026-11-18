package forestry.core.platform.client.book;

import forestry.core.platform.book.Book;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.Nullable;

// left and top place the page on the screen, the mouse is in screen pixels
interface BookElement {
	void extract(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY);

	// x and y are relative to the page
	@Nullable
	default Book.Link linkAt(int x, int y) {
		return null;
	}
}
