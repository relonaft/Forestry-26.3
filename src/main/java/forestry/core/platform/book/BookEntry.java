package forestry.core.platform.book;

import net.minecraft.resources.Identifier;

import java.util.List;

public record BookEntry(Identifier id, String name, Identifier category, String icon, int sortnum, boolean priority, List<BookPageData> pages) {
}
