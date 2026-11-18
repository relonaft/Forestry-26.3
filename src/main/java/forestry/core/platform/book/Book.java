package forestry.core.platform.book;

import forestry.api.ForestryConstants;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// name, landingText and every name or text below are translation keys
public record Book(String name, String landingText, Identifier texture, Map<Identifier, BookCategory> categories, Map<Identifier, BookEntry> entries) {
	// id is null for the landing page
	public record Link(boolean category, @Nullable Identifier id, String anchor) {
		public static final Link LANDING = new Link(true, null, "");
	}

	// a link names an entry, then a category, as "path" or "namespace:path" with an optional "#anchor"
	@Nullable
	public Link resolve(String target) {
		int hash = target.indexOf('#');
		String anchor = hash < 0 ? "" : target.substring(hash + 1);
		String path = hash < 0 ? target : target.substring(0, hash);
		Identifier id = path.indexOf(':') < 0 ? Identifier.tryBuild(ForestryConstants.MOD_ID, path) : Identifier.tryParse(path);
		if (id == null) {
			return null;
		}
		BookEntry entry = this.entries.get(id);
		if (entry != null) {
			return anchor.isEmpty() || entry.pages().stream().anyMatch(page -> page.anchor().equals(anchor)) ? new Link(false, id, anchor) : null;
		}
		return this.categories.containsKey(id) ? new Link(true, id, "") : null;
	}

	public List<BookCategory> categoriesIn(@Nullable Identifier parent) {
		return this.categories.values().stream()
			.filter(category -> Objects.equals(category.parent(), parent))
			.sorted(Comparator.comparingInt(BookCategory::sortnum))
			.toList();
	}

	public List<BookEntry> entriesIn(Identifier category) {
		return this.entries.values().stream()
			.filter(entry -> entry.category().equals(category))
			.toList();
	}
}
