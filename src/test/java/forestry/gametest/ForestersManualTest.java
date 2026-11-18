package forestry.gametest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;

import forestry.api.ForestryConstants;
import forestry.api.core.machines.IFabricatorRecipe;
import forestry.api.core.machines.IForestryRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import forestry.core.features.CoreItems;
import forestry.core.platform.book.Book;
import forestry.core.platform.book.BookCategory;
import forestry.core.platform.book.BookEntry;
import forestry.core.platform.book.BookLoader;
import forestry.core.platform.book.BookPageData;
import forestry.core.platform.book.BookRecipes;
import forestry.core.platform.book.BookText;
import forestry.core.platform.book.PageType;
import forestry.core.platform.registration.FeatureRecipeType;

// the Forester's Manual has no screen on a server, this checks all the screen would need to show every page
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class ForestersManualTest {
	// quirk kept: Forestry: Community Edition names a texture it never had, the book shows an error there
	private static final Set<String> KNOWN_BROKEN = Set.of(
		"texture forestry:textures/gui/almanac/scraps/alveary_2.png",
		// quirk kept: two page titles are English text in place of a translation key
		"untranslated text Circuit Boards",
		"untranslated text Electron Tubes"
	);
	// only these must have every text, where another language lacks one the game shows the English text
	private static final List<String> COMPLETE_LANGUAGES = List.of("en_us", "ru_ru");
	private static final String BOOK_KEY = "for.gui.book.patchouli.";
	// the keys the engine reads, a file with any other key would lose it without a word
	private static final Set<String> CATEGORY_KEYS = Set.of("name", "description", "icon", "parent", "sortnum");
	private static final Set<String> ENTRY_KEYS = Set.of("name", "category", "icon", "pages", "sortnum", "priority");
	private static final Set<String> PAGE_KEYS = Set.of("type", "anchor");
	// texts the engine itself shows
	private static final List<String> ENGINE_KEYS = List.of(
		"for.gui.book.categories", "for.gui.book.layer", "for.gui.book.error.page_type", "for.gui.book.error.page",
		"for.gui.book.error.recipe", "for.gui.book.error.image", "for.gui.book.patchouli.template.farm_gui.header", "for.gui.tooltip.liquid.amount"
	);
	private static final List<String> ENGINE_TEXTURES = List.of(
		"forestry:textures/gui/almanac/crafting.png", "forestry:textures/gui/almanac/guis/multi_farm_1024.png"
	);

	private final GameTestHelper helper;
	private final Book book;
	private final Map<String, Map<String, String>> languages = readBookLanguages();
	// sorted so the failure message is stable
	private final Set<String> problems = new TreeSet<>();

	private ForestersManualTest(GameTestHelper helper, Book book) {
		this.helper = helper;
		this.book = book;
	}

	@GameTest(template = "empty")
	public static void everyPageOfTheManualResolves(GameTestHelper helper) throws Exception {
		List<String> loadProblems = new ArrayList<>();
		Map<String, JsonObject> files = BookLoader.readModFiles(loadProblems::add);
		Book book = BookLoader.load(BookLoader.readBookJson(), files, loadProblems::add);
		ForestersManualTest test = new ForestersManualTest(helper, book);
		loadProblems.forEach(test.problems::add);

		long categoryFiles = files.keySet().stream().filter(path -> path.startsWith("categories/")).count();
		long entryFiles = files.keySet().stream().filter(path -> path.startsWith("entries/")).count();
		if (categoryFiles == 0 || entryFiles == 0 || categoryFiles != book.categories().size() || entryFiles != book.entries().size()) {
			test.problems.add("the book has " + book.categories().size() + " categories and " + book.entries().size() + " entries from " + categoryFiles + " and " + entryFiles + " files");
		}
		files.forEach((path, json) -> {
			if (path.startsWith("categories/")) {
				test.checkKeys(path, json, CATEGORY_KEYS);
			} else if (path.startsWith("entries/")) {
				test.checkKeys(path, json, ENTRY_KEYS);
			}
		});

		for (String language : COMPLETE_LANGUAGES) {
			if (!test.languages.containsKey(language)) {
				test.problems.add("language " + language + " has no book texts");
			}
		}
		// a translation can carry markup in a text no page uses any more
		test.languages.forEach((language, texts) -> texts.forEach((key, text) -> {
			if (key.startsWith(BOOK_KEY)) {
				test.checkMarkup(language, key, text);
			}
		}));

		test.checkText("book.json", book.name());
		test.checkText("book.json", book.landingText());
		test.checkTexture(book.texture().toString());
		ENGINE_KEYS.forEach(key -> test.checkText("the engine", key));
		ENGINE_TEXTURES.forEach(test::checkTexture);
		for (BookCategory category : book.categories().values()) {
			test.checkText(category.id().toString(), category.name());
			test.checkText(category.id().toString(), category.description());
			test.checkItem(category.icon());
		}
		for (BookEntry entry : book.entries().values()) {
			test.checkText(entry.id().toString(), entry.name());
			test.checkItem(entry.icon());
			for (BookPageData page : entry.pages()) {
				test.checkPage(entry.id() + " page " + (entry.pages().indexOf(page) + 1), page);
			}
		}

		Set<String> unexpected = new TreeSet<>(test.problems);
		unexpected.removeAll(KNOWN_BROKEN);
		Set<String> fixed = new TreeSet<>(KNOWN_BROKEN);
		fixed.removeAll(test.problems);
		if (!unexpected.isEmpty() || !fixed.isEmpty()) {
			helper.fail(unexpected.size() + " problem(s) in the Forester's Manual:\n  " + String.join("\n  ", unexpected)
				+ "\n" + fixed.size() + " entries of KNOWN_BROKEN that resolve now and must leave it:\n  " + String.join("\n  ", fixed));
		}
		helper.succeed();
	}

	// the screen is client code, on a server level use must end without it
	@GameTest(template = "empty")
	public static void useOnAServerLevelSucceeds(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, CoreItems.FORESTERS_MANUAL.stack());
		InteractionResult result = CoreItems.FORESTERS_MANUAL.item().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(result == InteractionResult.SUCCESS, "use on a server level returned " + result);
		helper.succeed();
	}

	// the base jar alone has the farm structure category and none of its entries
	@GameTest(template = "empty")
	public static void categoryWithoutEntriesIsHidden(GameTestHelper helper) throws Exception {
		Map<String, JsonObject> files = BookLoader.readModFiles(problem -> {
		});
		Identifier farming = ForestryConstants.forestry("farming");
		Identifier structure = ForestryConstants.forestry("structure");

		Book full = BookLoader.load(BookLoader.readBookJson(), files, problem -> {
		});
		helper.assertTrue(full.categoriesIn(farming).stream().anyMatch(category -> category.id().equals(structure)), "the structure category must show with its entries");

		files.keySet().removeIf(path -> path.startsWith("entries/farming/structure/"));
		Book base = BookLoader.load(BookLoader.readBookJson(), files, problem -> {
		});
		helper.assertTrue(base.categories().containsKey(structure), "the structure category must still load");
		helper.assertTrue(base.categoriesIn(farming).stream().noneMatch(category -> category.id().equals(structure)), "the structure category must hide without entries");
		helper.assertTrue(base.parent(new Book.Link(true, structure, "")).id().equals(farming), "the way back from structure must lead to farming");
		helper.succeed();
	}

	private void checkPage(String where, BookPageData page) {
		PageType type = page.type();
		if (type == null) {
			this.problems.add("unsupported page type " + page.typeId() + " (" + where + ")");
			return;
		}
		Set<String> keys = new HashSet<>(PAGE_KEYS);
		keys.addAll(type.keys());
		checkKeys(where, page.json(), keys);
		for (String key : List.of("text", "title", "name")) {
			if (page.has(key)) {
				checkText(where, page.string(key));
			}
		}

		switch (type) {
			case CRAFTING -> {
				checkCrafting(page.string("recipe"));
				if (page.has("recipe2")) {
					checkCrafting(page.string("recipe2"));
				}
			}
			case SPOTLIGHT -> checkItem(page.string("item"));
			case IMAGE -> {
				List<JsonElement> images = GsonHelper.getAsJsonArray(page.json(), "images").asList();
				if (images.size() != 1) {
					this.problems.add("the engine shows one image, the page has " + images.size() + " (" + where + ")");
				}
				images.forEach(image -> checkTexture(image.getAsString()));
			}
			case MULTIBLOCK -> checkMultiblock(where, GsonHelper.getAsJsonObject(page.json(), "multiblock"));
			case CARPENTER_SINGLE, CARPENTER_DOUBLE -> {
				checkMachine(FactoryRecipeTypes.CARPENTER, page.string("item"));
				if (page.has("item2")) {
					checkMachine(FactoryRecipeTypes.CARPENTER, page.string("item2"));
				}
			}
			case FABRICATOR_SINGLE, FABRICATOR_DOUBLE -> {
				checkMachine(FactoryRecipeTypes.FABRICATOR, page.string("item"));
				if (page.has("item2")) {
					checkMachine(FactoryRecipeTypes.FABRICATOR, page.string("item2"));
				}
			}
			case FARM_LAYOUT, FARM_LAYOUT_LARGE -> checkTexture(page.string("image"));
			case TEXT, FARM_GUI -> {
			}
		}
	}

	private void checkKeys(String where, JsonObject json, Set<String> known) {
		for (String key : json.keySet()) {
			// Patchouli skips keys it does not know, the pages use that for comments
			if (!known.contains(key) && !key.startsWith("_")) {
				this.problems.add("key " + key + " is not read by the engine (" + where + ")");
			}
		}
	}

	private void checkText(String where, String key) {
		if (key.isEmpty()) {
			return;
		}
		// not a key: Patchouli and the engine show such a text as it is, in every language
		if (!key.matches("[a-z0-9_.]+")) {
			this.problems.add("untranslated text " + key);
			return;
		}
		this.languages.forEach((language, texts) -> {
			String text = texts.get(key);
			if (text == null) {
				if (COMPLETE_LANGUAGES.contains(language)) {
					this.problems.add("text " + key + " is not in " + language + " (" + where + ")");
				}
			} else if (!key.startsWith(BOOK_KEY)) {
				checkMarkup(language, key, text);
			}
		});
	}

	private void checkMarkup(String language, String key, String text) {
		for (BookText.Span span : BookText.parse(text, command -> this.problems.add("unknown markup $(" + command + ") in " + language + " " + key))) {
			if (span.link() != null && this.book.resolve(span.link()) == null) {
				this.problems.add("link " + span.link() + " leads nowhere, in " + language + " " + key);
			}
		}
		// the font of the game reads these codes, the engine passes them on
		for (int at = text.indexOf(ChatFormatting.PREFIX_CODE); at >= 0; at = text.indexOf(ChatFormatting.PREFIX_CODE, at + 1)) {
			if (at + 1 >= text.length() || ChatFormatting.getByCode(text.charAt(at + 1)) == null) {
				this.problems.add("unknown formatting code at " + at + " in " + language + " " + key);
			}
		}
	}

	private void checkItem(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
			this.problems.add("item " + id);
		}
	}

	private void checkTexture(String id) {
		Identifier key = Identifier.tryParse(id);
		String path = key == null ? "" : "assets/" + key.getNamespace() + "/" + key.getPath();
		if (ModList.get().applyForEachModFile(modFile -> modFile.getContents().containsFile(path)).noneMatch(Boolean::booleanValue)) {
			this.problems.add("texture " + id);
		}
	}

	private void checkCrafting(String id) {
		Identifier key = Identifier.tryParse(id);
		RecipeMap recipes = this.helper.getLevel().recipeAccess().recipeMap();
		RecipeHolder<?> holder = key == null ? null : recipes.byKey(ResourceKey.create(Registries.RECIPE, key));
		if (holder == null || !(holder.value() instanceof CraftingRecipe)) {
			this.problems.add("recipe " + id);
		}
	}

	// the machine pages name the result, not the recipe
	private void checkMachine(FeatureRecipeType<? extends IForestryRecipe> type, String item) {
		checkItem(item);
		Identifier key = Identifier.tryParse(item);
		RecipeMap recipes = this.helper.getLevel().recipeAccess().recipeMap();
		IForestryRecipe recipe = key == null ? null : BookRecipes.byOutput(recipes, type, new ItemStack(BuiltInRegistries.ITEM.getValue(key)));
		if (recipe == null) {
			this.problems.add(BuiltInRegistries.RECIPE_TYPE.getKey(type.type()) + " recipe for " + item);
		} else if (recipe instanceof IFabricatorRecipe fabricator && BookRecipes.meltable(recipes, fabricator.getResultFluid()).isEmpty()) {
			this.problems.add("nothing melts into " + BuiltInRegistries.FLUID.getKey(fabricator.getResultFluid().getFluid()) + " for the fabricator recipe of " + item);
		}
	}

	private void checkMultiblock(String where, JsonObject multiblock) {
		JsonObject mapping = GsonHelper.getAsJsonObject(multiblock, "mapping");
		for (JsonElement layer : GsonHelper.getAsJsonArray(multiblock, "pattern")) {
			for (JsonElement row : layer.getAsJsonArray()) {
				for (char cell : row.getAsString().toCharArray()) {
					// a space is air and an underscore is any block, they need no mapping
					if (cell != ' ' && cell != '_' && !mapping.has(String.valueOf(cell))) {
						this.problems.add("multiblock cell " + cell + " has no mapping (" + where + ")");
					}
				}
			}
		}
		for (String cell : mapping.keySet()) {
			String value = mapping.get(cell).getAsString();
			boolean found;
			if (value.startsWith("#")) {
				Identifier tag = Identifier.tryParse(value.substring(1));
				found = tag != null && BuiltInRegistries.BLOCK.getTagOrEmpty(TagKey.create(Registries.BLOCK, tag)).iterator().hasNext();
			} else {
				int state = value.indexOf('[');
				Identifier block = Identifier.tryParse(state < 0 ? value : value.substring(0, state));
				found = block != null && BuiltInRegistries.BLOCK.containsKey(block) && !new ItemStack(BuiltInRegistries.BLOCK.getValue(block)).isEmpty();
			}
			if (!found) {
				this.problems.add("multiblock block " + value + " (" + where + ")");
			}
		}
	}

	// every language with book texts, each the union over every jar as a client merges the lang files of its packs
	private static Map<String, Map<String, String>> readBookLanguages() {
		String folder = "assets/forestry/lang/";
		Map<String, Map<String, String>> languages = new TreeMap<>();
		ModList.get().forEachModFile(modFile -> modFile.getContents().visitContent(folder, (file, resource) -> {
			int start = file.indexOf(folder);
			if (start >= 0 && file.endsWith(".json")) {
				String language = file.substring(start + folder.length(), file.length() - ".json".length());
				// the reader of the game, it takes files a strict parser refuses
				try (InputStream stream = resource.open()) {
					Language.loadFromJson(stream, languages.computeIfAbsent(language, name -> new HashMap<>())::put);
				} catch (Exception e) {
					throw new IllegalStateException("cannot read " + file, e);
				}
			}
		}));
		languages.values().removeIf(texts -> texts.keySet().stream().noneMatch(key -> key.startsWith(BOOK_KEY)));
		return languages;
	}
}
