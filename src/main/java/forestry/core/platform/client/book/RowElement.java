package forestry.core.platform.client.book;

import forestry.core.platform.book.Book;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

// one line of a category or entry list. The whole row is the link
final class RowElement implements BookElement {
	static final int HEIGHT = 11;
	private static final int HOVER_COLOR = 0xFF8800CC;
	private static final int TEXT_X = 11;

	private final int y;
	private final ItemElement icon;
	private final LabelElement label;
	private final LabelElement hoverLabel;
	private final Book.Link link;

	RowElement(int y, ItemStack icon, Component name, Book.Link link) {
		this.y = y;
		this.icon = new ItemElement(0, y + 1, 0.5f, icon.isEmpty() ? List.of() : List.of(icon), null);
		this.label = LabelElement.left(name, TEXT_X, y + 1, BookPage.WIDTH - TEXT_X, TextElement.TEXT_COLOR, false);
		this.hoverLabel = LabelElement.left(name, TEXT_X, y + 1, BookPage.WIDTH - TEXT_X, HOVER_COLOR, false);
		this.link = link;
	}

	@Override
	public void extract(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
		// the icon of a row shows no tooltip
		this.icon.extract(graphics, font, left, top, left - 1, top - 1);
		boolean hovered = linkAt(mouseX - left, mouseY - top) != null;
		(hovered ? this.hoverLabel : this.label).extract(graphics, font, left, top, mouseX, mouseY);
	}

	@Nullable
	@Override
	public Book.Link linkAt(int x, int y) {
		return x >= 0 && x < BookPage.WIDTH && y >= this.y && y < this.y + HEIGHT ? this.link : null;
	}
}
