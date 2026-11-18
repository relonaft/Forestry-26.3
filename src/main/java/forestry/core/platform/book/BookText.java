package forestry.core.platform.book;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

// the Patchouli text markup: $(br), $(l:target) and the one-letter style and color codes
public final class BookText {
	public static final int NO_COLOR = -1;

	public enum Format {
		OBFUSCATED, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC
	}

	// a line break is a newline in the text
	public record Span(String text, Set<Format> formats, int color, @Nullable String link) {
	}

	private BookText() {
	}

	public static List<Span> parse(String raw, Consumer<String> unknownMarkup) {
		List<Span> spans = new ArrayList<>();
		StringBuilder text = new StringBuilder();
		EnumSet<Format> formats = EnumSet.noneOf(Format.class);
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

			// also at a line break: a legacy code in the text ends with its span, as it did in Patchouli
			if (!text.isEmpty()) {
				spans.add(new Span(text.toString(), Set.copyOf(formats), color, link));
				text.setLength(0);
			}
			ChatFormatting code = command.length() == 1 ? ChatFormatting.getByCode(command.charAt(0)) : null;
			TextColor legacyColor = code == null ? null : TextColor.fromLegacyFormat(code);
			if (command.equals("br")) {
				text.append('\n');
			} else if (command.equals("br2")) {
				text.append("\n\n");
			} else if (command.isEmpty()) {
				formats.clear();
				color = NO_COLOR;
				link = null;
			} else if (command.equals("bold") || command.equals("l")) {
				formats.add(Format.BOLD);
			} else if (command.equals("italic") || command.equals("o")) {
				formats.add(Format.ITALIC);
			} else if (command.equals("k")) {
				formats.add(Format.OBFUSCATED);
			} else if (command.equals("m")) {
				formats.add(Format.STRIKETHROUGH);
			} else if (command.equals("n")) {
				formats.add(Format.UNDERLINE);
			} else if (legacyColor != null) {
				color = legacyColor.getValue();
			} else if (command.matches("#[0-9a-fA-F]{6}")) {
				color = Integer.parseInt(command.substring(1), 16);
			} else if (command.startsWith("l:")) {
				link = command.substring(2);
			} else if (command.equals("/l")) {
				link = null;
			} else {
				unknownMarkup.accept(command);
			}
		}
		if (!text.isEmpty()) {
			spans.add(new Span(text.toString(), Set.copyOf(formats), color, link));
		}
		return spans;
	}
}
