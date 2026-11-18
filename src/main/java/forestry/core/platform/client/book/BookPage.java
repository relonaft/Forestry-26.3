package forestry.core.platform.client.book;

import forestry.core.platform.book.Book;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

final class BookPage {
	// the page grid Patchouli books are written for, less the rows the paper of this texture does not have
	static final int WIDTH = 116;
	static final int HEIGHT = 150;
	static final int LINE_HEIGHT = 9;

	final String anchor;
	private final List<BookElement> elements = new ArrayList<>();

	BookPage(String anchor) {
		this.anchor = anchor;
	}

	BookPage add(BookElement element) {
		this.elements.add(element);
		return this;
	}

	void extract(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
		for (BookElement element : this.elements) {
			element.extract(graphics, font, left, top, mouseX, mouseY);
		}
	}

	@Nullable
	Book.Link linkAt(int x, int y) {
		for (BookElement element : this.elements) {
			Book.Link link = element.linkAt(x, y);
			if (link != null) {
				return link;
			}
		}
		return null;
	}
}
