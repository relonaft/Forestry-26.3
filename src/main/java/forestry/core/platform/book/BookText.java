package forestry.core.platform.book;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// the subset of the Patchouli text markup this book uses
public final class BookText {
	public static final int NO_COLOR = -1;

	// a line break is a newline in the text
	public record Span(String text, boolean bold, boolean italic, int color, @Nullable String link) {
	}

	private BookText() {
	}

	public static List<Span> parse(String raw, Consumer<String> unknownMarkup) {
		List<Span> spans = new ArrayList<>();
		StringBuilder text = new StringBuilder();
		boolean bold = false;
		boolean italic = false;
		int color = NO_COLOR;
		String link = null;

		int pos = 0;
		while (pos < raw.length()) {
			int start = raw.indexOf("$(", pos);
			int end = start < 0 ? -1 : raw.indexOf(')', start);
			if (end < 0) {
				text.append(raw, pos, raw.length());
				break;
			}
			text.append(raw, pos, start);
			String command = raw.substring(start + 2, end);
			pos = end + 1;

			if (command.equals("br")) {
				text.append('\n');
				continue;
			} else if (command.equals("br2")) {
				text.append("\n\n");
				continue;
			}
			if (!text.isEmpty()) {
				spans.add(new Span(text.toString(), bold, italic, color, link));
				text.setLength(0);
			}
			if (command.isEmpty()) {
				bold = false;
				italic = false;
				color = NO_COLOR;
			} else if (command.equals("bold")) {
				bold = true;
			} else if (command.equals("italic")) {
				italic = true;
			} else if (command.startsWith("l:")) {
				link = command.substring(2);
			} else if (command.equals("/l")) {
				link = null;
			} else if (command.matches("#[0-9a-fA-F]{6}")) {
				color = Integer.parseInt(command.substring(1), 16);
			} else {
				unknownMarkup.accept(command);
			}
		}
		if (!text.isEmpty()) {
			spans.add(new Span(text.toString(), bold, italic, color, link));
		}
		return spans;
	}
}
