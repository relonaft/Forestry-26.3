package forestry.core.platform.client.book;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import forestry.api.ForestryConstants;
import forestry.core.platform.book.Book;
import forestry.core.platform.book.BookCategory;
import forestry.core.platform.book.BookEntry;
import forestry.core.platform.book.BookPageData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// lays out the pages of one place in the book: the landing page, a category or an entry
final class BookPages {
	private static final int TEXTURE_WIDTH = 512;
	private static final int TEXTURE_HEIGHT = 256;
	// below a header and its line
	private static final int BODY_Y = 20;

	private record Row(ItemStack icon, Component name, Book.Link link) {
	}

	private final Book book;
	private final Font font;
	private final List<BookPage> pages = new ArrayList<>();

	private BookPages(Book book, Font font) {
		this.book = book;
		this.font = font;
	}

	static List<BookPage> build(Book book, Font font, Book.Link target) {
		BookPages builder = new BookPages(book, font);
		BookCategory category = target.category() ? book.categories().get(target.id()) : null;
		BookEntry entry = target.category() ? null : book.entries().get(target.id());
		if (category != null) {
			builder.category(category);
		} else if (entry != null) {
			builder.entry(entry);
		} else {
			builder.landing();
		}
		return builder.pages;
	}

	private void landing() {
		BookPage page = new BookPage("");
		// the ribbon hangs over the left edge of the book
		page.add(new ImageElement(this.book.texture(), -23, -6, 0, 180, 140, 31, TEXTURE_WIDTH, TEXTURE_HEIGHT, 1.0f));
		page.add(LabelElement.left(Component.translatable(this.book.name()), -2, 2, 110, 0xFFFFFFFF, true));
		flow(page, this.book.landingText(), 30);

		// the list opens on a right page
		if (this.pages.size() % 2 == 0) {
			this.pages.add(new BookPage(""));
		}
		BookPage list = header("", Component.translatable("for.gui.book.categories"));
		rows(list, BODY_Y, categoryRows(null));
	}

	private void category(BookCategory category) {
		flow(header("", Component.translatable(category.name())), category.description(), BODY_Y);

		List<Row> rows = new ArrayList<>(categoryRows(category.id()));
		// the order of Patchouli: priority entries, then sortnum, then the name in the language of the game
		this.book.entriesIn(category.id()).stream()
			.sorted(Comparator.comparing((BookEntry entry) -> !entry.priority())
				.thenComparingInt(BookEntry::sortnum)
				.thenComparing(entry -> translate(entry.name()), String::compareToIgnoreCase))
			.forEach(entry -> rows.add(new Row(BookClient.stack(entry.icon()), Component.translatable(entry.name()), new Book.Link(false, entry.id(), ""))));
		rows(new BookPage(""), 0, rows);
	}

	private List<Row> categoryRows(@Nullable Identifier parent) {
		return this.book.categoriesIn(parent).stream()
			.map(category -> new Row(BookClient.stack(category.icon()), Component.translatable(category.name()), new Book.Link(true, category.id(), "")))
			.toList();
	}

	private void rows(BookPage page, int y, List<Row> rows) {
		this.pages.add(page);
		for (Row row : rows) {
			if (y + RowElement.HEIGHT > BookPage.HEIGHT) {
				page = new BookPage("");
				this.pages.add(page);
				y = 0;
			}
			page.add(new RowElement(y, row.icon(), row.name(), row.link()));
			y += RowElement.HEIGHT;
		}
	}

	private void entry(BookEntry entry) {
		for (BookPageData data : entry.pages()) {
			int before = this.pages.size();
			try {
				page(entry, data, before == 0);
			} catch (RuntimeException e) {
				BookClient.warnOnce("Cannot build a " + data.typeId() + " page of book entry " + entry.id() + ": " + e);
				this.pages.subList(before, this.pages.size()).clear();
				error(data, Component.translatable("for.gui.book.error.page", e.toString()));
			}
		}
	}

	private void page(BookEntry entry, BookPageData data, boolean first) {
		if (data.type() == null) {
			BookClient.warnOnce("Unknown page type " + data.typeId() + " in book entry " + entry.id());
			error(data, Component.translatable("for.gui.book.error.page_type", data.typeId()));
			return;
		}
		BookPage page = new BookPage(data.anchor());
		switch (data.type()) {
			case TEXT -> {
				// Patchouli puts the name of the entry over a text page that opens it
				if (first) {
					page = header(data.anchor(), Component.translatable(entry.name()));
				}
				flow(page, data.string("text"), first ? BODY_Y : 0);
			}
			case CRAFTING -> crafting(page, data);
			case SPOTLIGHT -> {
				ItemStack stack = BookClient.stack(data.string("item"));
				page.add(LabelElement.header(data.has("title") ? Component.translatable(data.string("title")) : stack.getHoverName(), 0));
				page.add(RecipePages.slot(49, 13));
				page.add(new ItemElement(50, 14, List.of(stack)));
				flow(page, data.string("text"), 38);
			}
			case IMAGE -> {
				int y = 0;
				if (data.has("title")) {
					page.add(LabelElement.header(Component.translatable(data.string("title")), 0));
					y = 12;
				}
				// Patchouli shows the 200 by 200 corner of a 256 texture at half size. Only the first image is shown
				image(page, GsonHelper.getAsJsonArray(data.json(), "images").get(0).getAsString(), 8, y, 200, 200, 256, 256, 0.5f);
				flow(page, data.string("text"), y + 106);
			}
			case MULTIBLOCK -> multiblock(page, data);
			// the positions of the six machine pages come from templates/carpenter and templates/fabricator
			case CARPENTER_SINGLE -> {
				RecipePages.carpenter(this.font, page, 4, BookClient.stack(data.string("item")));
				flow(page, data.string("text"), 69);
			}
			case CARPENTER_DOUBLE -> {
				RecipePages.carpenter(this.font, page, 4, BookClient.stack(data.string("item")));
				if (data.has("item2")) {
					RecipePages.carpenter(this.font, page, 84, BookClient.stack(data.string("item2")));
				}
				this.pages.add(page);
			}
			case FABRICATOR_SINGLE -> {
				RecipePages.fabricator(this.font, page, 4, BookClient.stack(data.string("item")));
				flow(page, data.string("text"), 69);
			}
			case FABRICATOR_DOUBLE -> {
				RecipePages.fabricator(this.font, page, 4, BookClient.stack(data.string("item")));
				if (data.has("item2")) {
					RecipePages.fabricator(this.font, page, 84, BookClient.stack(data.string("item2")));
				}
				this.pages.add(page);
			}
			// the positions, sizes and scales come from templates/farm_layout, farm_layout_large and farm_gui
			case FARM_LAYOUT -> {
				image(page, data.string("image"), 5, -10, 512, 512, 512, 512, 0.25f);
				flow(page, data.string("text"), 90);
			}
			case FARM_LAYOUT_LARGE -> {
				image(page, data.string("image"), 5, -10, 512, 512, 512, 512, 0.4f);
				flow(page, data.string("text"), 90);
			}
			case FARM_GUI -> {
				page.add(LabelElement.header(Component.translatable("for.gui.book.patchouli.template.farm_gui.header"), -5));
				image(page, "forestry:textures/gui/almanac/guis/multi_farm_1024.png", 5, 8, 512, 256, 648, 648, 0.25f);
				flow(page, data.string("text"), 78);
			}
		}
	}

	// one or two recipes, each under the name of its result, then the text
	private void crafting(BookPage page, BookPageData data) {
		int y = 0;
		for (String key : List.of("recipe", "recipe2")) {
			if (data.has(key)) {
				ItemStack result = RecipePages.crafting(this.font, page, (BookPage.WIDTH - RecipePages.CRAFTING_WIDTH) / 2, y + 12, data.string(key));
				boolean titled = y == 0 && data.has("title");
				if (titled || !result.isEmpty()) {
					page.add(LabelElement.header(titled ? Component.translatable(data.string("title")) : result.getHoverName(), y));
				}
				y += 12 + RecipePages.HEIGHT + 6;
			}
		}
		flow(page, data.string("text"), y);
	}

	// the layers from the bottom up, each a flat grid of items. Patchouli drew the structure in 3D
	private void multiblock(BookPage page, BookPageData data) {
		JsonObject multiblock = GsonHelper.getAsJsonObject(data.json(), "multiblock");
		JsonObject mapping = GsonHelper.getAsJsonObject(multiblock, "mapping");
		List<JsonElement> layers = GsonHelper.getAsJsonArray(multiblock, "pattern").asList();
		int room = (BookPage.WIDTH - 8) / 2;

		page.add(LabelElement.header(Component.translatable(data.string("name")), 0));
		this.pages.add(page);
		int x = 0;
		int y = 14;
		for (int layer = layers.size() - 1; layer >= 0; layer--) {
			List<JsonElement> rows = layers.get(layer).getAsJsonArray().asList();
			int columns = rows.stream().mapToInt(row -> row.getAsString().length()).max().orElse(1);
			float scale = Math.min(1.0f, room / (16.0f * Math.max(columns, rows.size())));
			int height = BookPage.LINE_HEIGHT + 2 + (int) Math.ceil(rows.size() * 16 * scale) + 6;
			if (y + height > BookPage.HEIGHT) {
				page = new BookPage("");
				this.pages.add(page);
				y = 0;
			}

			page.add(LabelElement.left(Component.translatable("for.gui.book.layer", layers.size() - layer), x, y, room, LabelElement.HEADER_COLOR, false));
			for (int row = 0; row < rows.size(); row++) {
				String cells = rows.get(row).getAsString();
				for (int column = 0; column < cells.length(); column++) {
					List<ItemStack> stacks = blockStacks(GsonHelper.getAsString(mapping, cells.substring(column, column + 1), ""));
					page.add(new ItemElement(x + (int) (column * 16 * scale), y + BookPage.LINE_HEIGHT + 2 + (int) (row * 16 * scale), scale, stacks, null));
				}
			}
			// two layers side by side
			if (x == 0) {
				x = room + 8;
			} else {
				x = 0;
				y += height;
			}
		}
		if (data.has("text")) {
			flow(new BookPage(""), data.string("text"), 0);
		}
	}

	// a mapping value is a block, with or without a state, or a block tag after #
	private static List<ItemStack> blockStacks(String value) {
		List<ItemStack> stacks = new ArrayList<>();
		if (value.startsWith("#")) {
			Identifier tag = Identifier.tryParse(value.substring(1));
			if (tag != null) {
				for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(TagKey.create(Registries.BLOCK, tag))) {
					stacks.add(new ItemStack(block.value()));
				}
			}
		} else if (!value.isEmpty()) {
			int state = value.indexOf('[');
			Identifier id = Identifier.tryParse(state < 0 ? value : value.substring(0, state));
			if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
				stacks.add(new ItemStack(BuiltInRegistries.BLOCK.getValue(id)));
			}
		}
		stacks.removeIf(ItemStack::isEmpty);
		return stacks;
	}

	// not I18n, it formats the text and a page with a percent sign comes out as a format error
	private static String translate(String key) {
		return Language.getInstance().getOrDefault(key);
	}

	// a page with a centered name and a line under it
	private BookPage header(String anchor, Component name) {
		return new BookPage(anchor)
			.add(LabelElement.header(name, 0))
			.add(new ImageElement(this.book.texture(), 3, 12, 140, 180, 110, 3, TEXTURE_WIDTH, TEXTURE_HEIGHT, 1.0f));
	}

	private void image(BookPage page, String texture, int x, int y, int width, int height, int textureWidth, int textureHeight, float scale) {
		Identifier id = Identifier.tryParse(texture);
		if (id == null || Minecraft.getInstance().getResourceManager().getResource(id).isEmpty()) {
			BookClient.warnOnce("Book image not found: " + texture);
			page.add(TextElement.error(this.font, Component.translatable("for.gui.book.error.image", texture), 0, Math.max(0, y), BookPage.WIDTH));
		} else {
			page.add(new ImageElement(id, x, y, 0, 0, width, height, textureWidth, textureHeight, scale));
		}
	}

	private void error(BookPageData data, Component message) {
		this.pages.add(new BookPage(data.anchor()).add(TextElement.error(this.font, message, 0, 0, BookPage.WIDTH)));
	}

	// adds the page with the text from y down
	// deviation: Patchouli lets a long text run off its page, here it goes on to as many pages as it needs
	private void flow(BookPage page, String key, int y) {
		this.pages.add(page);
		if (key.isEmpty()) {
			return;
		}
		List<TextElement.Line> lines = TextElement.layout(this.font, this.book, translate(key), BookPage.WIDTH);
		int from = 0;
		while (from < lines.size()) {
			int to = Math.min(lines.size(), from + Math.max(0, (BookPage.HEIGHT - y) / BookPage.LINE_HEIGHT));
			if (to > from) {
				page.add(new TextElement(0, y, TextElement.TEXT_COLOR, lines.subList(from, to)));
			}
			from = to;
			// a page does not open with the blank line of a paragraph break
			while (from < lines.size() && this.font.width(lines.get(from).text()) == 0) {
				from++;
			}
			if (from < lines.size()) {
				page = new BookPage("");
				this.pages.add(page);
				y = 0;
			}
		}
	}
}
