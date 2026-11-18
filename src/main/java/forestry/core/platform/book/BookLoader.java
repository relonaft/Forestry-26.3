package forestry.core.platform.book;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import forestry.api.ForestryConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

// reads the Forester's Manual from its Patchouli format files
public final class BookLoader {
	public static final String BOOK_PATH = "patchouli_books/foresters_manual/";
	// the book is i18n, en_us is the only content folder and the texts are translation keys
	public static final String CONTENT_PATH = BOOK_PATH + "en_us/";
	private static final Identifier DEFAULT_TEXTURE = ForestryConstants.forestry("textures/gui/almanac/foresters_manual.png");
	private static final String CATEGORIES = "categories/";
	private static final String ENTRIES = "entries/";
	private static final String JSON = ".json";

	private BookLoader() {
	}

	// files are keyed by their path below the content folder, ex. "entries/core/ores.json"
	public static Book load(JsonObject bookJson, Map<String, JsonObject> files, Consumer<String> problems) {
		Map<Identifier, BookCategory> categories = new LinkedHashMap<>();
		Map<Identifier, BookEntry> entries = new LinkedHashMap<>();

		// sorted, the order of the files must not depend on the pack order
		for (Map.Entry<String, JsonObject> file : new TreeMap<>(files).entrySet()) {
			String path = file.getKey();
			JsonObject json = file.getValue();
			try {
				if (path.startsWith(CATEGORIES)) {
					Identifier id = id(path, CATEGORIES);
					Identifier parent = json.has("parent") ? Identifier.parse(GsonHelper.getAsString(json, "parent")) : null;
					categories.put(id, new BookCategory(id, GsonHelper.getAsString(json, "name"), GsonHelper.getAsString(json, "description", ""),
						GsonHelper.getAsString(json, "icon", ""), parent, GsonHelper.getAsInt(json, "sortnum", 0)));
				} else if (path.startsWith(ENTRIES)) {
					Identifier id = id(path, ENTRIES);
					List<BookPageData> pages = new ArrayList<>();
					for (JsonElement element : GsonHelper.getAsJsonArray(json, "pages")) {
						JsonObject page = GsonHelper.convertToJsonObject(element, "page");
						String type = GsonHelper.getAsString(page, "type");
						pages.add(new BookPageData(type, PageType.byId(type), page));
					}
					entries.put(id, new BookEntry(id, GsonHelper.getAsString(json, "name"), Identifier.parse(GsonHelper.getAsString(json, "category")),
						GsonHelper.getAsString(json, "icon", ""), GsonHelper.getAsInt(json, "sortnum", 0), GsonHelper.getAsBoolean(json, "priority", false), pages));
				}
			} catch (RuntimeException e) {
				problems.accept(path + ": " + e.getMessage());
			}
		}

		// a category must lead up to the landing page, a loop of parents would never end
		Map<Identifier, BookCategory> all = Map.copyOf(categories);
		categories.values().removeIf(category -> {
			BookCategory ancestor = category;
			for (int steps = 0; ancestor != null && ancestor.parent() != null && steps <= all.size(); steps++) {
				ancestor = all.get(ancestor.parent());
			}
			boolean orphan = ancestor == null || ancestor.parent() != null;
			if (orphan) {
				problems.accept("category " + category.id() + " has the unknown or looped parent " + category.parent());
			}
			return orphan;
		});
		entries.values().removeIf(entry -> {
			boolean orphan = !categories.containsKey(entry.category());
			if (orphan) {
				problems.accept("entry " + entry.id() + " has the unknown category " + entry.category());
			}
			return orphan;
		});

		// an empty text is no texture, it would parse as an identifier with an empty path
		BookPageData book = new BookPageData("", null, bookJson);
		Identifier texture = book.string("book_texture").isEmpty() ? null : Identifier.tryParse(book.string("book_texture"));
		return new Book(book.string("name"), book.string("landing_text"), texture == null ? DEFAULT_TEXTURE : texture, categories, entries);
	}

	private static Identifier id(String path, String folder) {
		return ForestryConstants.forestry(path.substring(folder.length(), path.length() - JSON.length()));
	}

	// book.json is in the data folder, a client resource manager cannot see it
	public static JsonObject readBookJson() throws IOException {
		String path = "data/forestry/" + BOOK_PATH + "book.json";
		JarResource resource = ModList.get().getModFileById(ForestryConstants.MOD_ID).getFile().getContents().get(path);
		if (resource == null) {
			throw new FileNotFoundException(path);
		}
		try (Reader reader = resource.bufferedReader()) {
			return GsonHelper.parse(reader);
		}
	}

	// a server has no resource manager for assets, a client reads these files from its resource packs
	public static Map<String, JsonObject> readModFiles(Consumer<String> problems) {
		String root = "assets/forestry/" + CONTENT_PATH;
		Map<String, JsonObject> files = new TreeMap<>();
		ModList.get().forEachModFile(modFile -> modFile.getContents().visitContent(root, (path, resource) -> {
			int start = path.indexOf(root);
			if (start >= 0 && path.endsWith(JSON)) {
				try (Reader reader = resource.bufferedReader()) {
					files.put(path.substring(start + root.length()), GsonHelper.parse(reader));
				} catch (IOException | JsonParseException e) {
					problems.accept(path + ": " + e.getMessage());
				}
			}
		}));
		return files;
	}
}
