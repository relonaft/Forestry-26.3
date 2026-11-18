package forestry.core.platform.client.book;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import forestry.Forestry;
import forestry.api.ForestryConstants;
import forestry.core.platform.book.Book;
import forestry.core.platform.book.BookLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// the Forester's Manual of this client, read once per resource reload
public final class BookClient {
	private static final Set<String> WARNED = new HashSet<>();
	@Nullable
	private static Book book;

	private BookClient() {
	}

	public static void open() {
		Minecraft.getInstance().gui.setScreen(new BookScreen(book()));
	}

	public static void clear() {
		book = null;
		WARNED.clear();
	}

	private static Book book() {
		if (book == null) {
			book = load();
		}
		return book;
	}

	private static Book load() {
		// without the slash that ends CONTENT_PATH, the resource manager lists a folder by its name
		String folder = BookLoader.CONTENT_PATH.substring(0, BookLoader.CONTENT_PATH.length() - 1);
		Map<String, JsonObject> files = new HashMap<>();
		Minecraft.getInstance().getResourceManager()
			.listResources(folder, id -> id.getNamespace().equals(ForestryConstants.MOD_ID) && id.getPath().endsWith(".json"))
			.forEach((id, resource) -> {
				try (Reader reader = resource.openAsReader()) {
					files.put(id.getPath().substring(BookLoader.CONTENT_PATH.length()), GsonHelper.parse(reader));
				} catch (IOException | JsonParseException e) {
					warnOnce("Cannot read book file " + id + ": " + e.getMessage());
				}
			});

		JsonObject bookJson = new JsonObject();
		try {
			bookJson = BookLoader.readBookJson();
		} catch (IOException | JsonParseException e) {
			warnOnce("Cannot read book.json: " + e.getMessage());
		}
		return BookLoader.load(bookJson, files, problem -> warnOnce("Book problem: " + problem));
	}

	// a page is built again each time it opens, its problems are logged once
	static void warnOnce(String message) {
		if (WARNED.add(message)) {
			Forestry.LOGGER.warn(message);
		}
	}

	// empty when the id names no item
	static ItemStack stack(String id) {
		Identifier key = Identifier.tryParse(id);
		// the item registry answers an unknown key with air
		if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
			warnOnce("Book names the unknown item " + id);
			return ItemStack.EMPTY;
		}
		return new ItemStack(BuiltInRegistries.ITEM.getValue(key));
	}
}
