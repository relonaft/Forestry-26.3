package forestry.core.platform.client.book;

import forestry.core.platform.book.Book;
import forestry.core.platform.book.BookText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

final class TextElement implements BookElement {
	static final int TEXT_COLOR = 0xFF000000;
	static final int LINK_COLOR = 0x0000CC;
	private static final int LINK_HOVER_COLOR = 0xFF8800CC;
	private static final int ERROR_COLOR = 0xFFAA0000;

	record LinkArea(int x0, int x1, Book.Link link) {
	}

	record Line(FormattedCharSequence text, List<LinkArea> links) {
	}

	private final int x;
	private final int y;
	private final int color;
	private final List<Line> lines;

	TextElement(int x, int y, int color, List<Line> lines) {
		this.x = x;
		this.y = y;
		this.color = color;
		this.lines = lines;
	}

	// what a page shows in place of content it cannot show
	static TextElement error(Font font, Component message, int x, int y, int width) {
		List<Line> lines = new ArrayList<>();
		for (FormattedCharSequence line : font.split(message, width)) {
			lines.add(new Line(line, List.of()));
		}
		return new TextElement(x, y, ERROR_COLOR, lines);
	}

	// raw is translated text with book markup
	// deviation: set in the font of the game, Patchouli sets its books in the thin unicode font
	static List<Line> layout(Font font, Book book, String raw, int width) {
		MutableComponent text = Component.empty();
		for (BookText.Span span : BookText.parse(raw, command -> BookClient.warnOnce("Unknown book markup $(" + command + ")"))) {
			Style style = Style.EMPTY;
			for (BookText.Format format : span.formats()) {
				style = switch (format) {
					case OBFUSCATED -> style.withObfuscated(true);
					case BOLD -> style.withBold(true);
					case STRIKETHROUGH -> style.withStrikethrough(true);
					case UNDERLINE -> style.withUnderlined(true);
					case ITALIC -> style.withItalic(true);
				};
			}
			if (span.color() != BookText.NO_COLOR) {
				style = style.withColor(span.color());
			}
			if (span.link() != null) {
				if (book.resolve(span.link()) != null) {
					// the insertion carries the link through the line splitter
					style = style.withColor(LINK_COLOR).withInsertion(span.link());
				} else {
					BookClient.warnOnce("Book link to nothing: " + span.link());
				}
			}
			text.append(Component.literal(span.text()).withStyle(style));
		}

		List<Line> lines = new ArrayList<>();
		for (FormattedCharSequence line : font.split(text, width)) {
			lines.add(new Line(line, linkAreas(font, book, line)));
		}
		return lines;
	}

	private static List<LinkArea> linkAreas(Font font, Book book, FormattedCharSequence line) {
		List<LinkArea> areas = new ArrayList<>();
		// width so far, start of the open link, the open link
		float[] width = new float[2];
		String[] open = new String[1];
		line.accept((position, style, codepoint) -> {
			String insertion = style.getInsertion();
			if (open[0] != null && !open[0].equals(insertion)) {
				areas.add(new LinkArea((int) width[1], (int) Math.ceil(width[0]), book.resolve(open[0])));
				open[0] = null;
			}
			if (insertion != null && open[0] == null) {
				open[0] = insertion;
				width[1] = width[0];
			}
			width[0] += font.getSplitter().stringWidth(FormattedCharSequence.codepoint(codepoint, style));
			return true;
		});
		if (open[0] != null) {
			areas.add(new LinkArea((int) width[1], (int) Math.ceil(width[0]), book.resolve(open[0])));
		}
		return areas;
	}

	@Override
	public void extract(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY) {
		Book.Link hovered = linkAt(mouseX - left, mouseY - top);
		for (int i = 0; i < this.lines.size(); i++) {
			Line line = this.lines.get(i);
			int lineY = top + this.y + i * BookPage.LINE_HEIGHT;
			graphics.text(font, line.text(), left + this.x, lineY, this.color, false);
			for (LinkArea area : line.links()) {
				if (area.link() == hovered) {
					graphics.fill(left + this.x + area.x0(), lineY + 8, left + this.x + area.x1(), lineY + 9, LINK_HOVER_COLOR);
				}
			}
		}
	}

	@Nullable
	@Override
	public Book.Link linkAt(int x, int y) {
		int row = Math.floorDiv(y - this.y, BookPage.LINE_HEIGHT);
		if (row >= 0 && row < this.lines.size()) {
			for (LinkArea area : this.lines.get(row).links()) {
				if (x >= this.x + area.x0() && x < this.x + area.x1()) {
					return area.link();
				}
			}
		}
		return null;
	}
}
