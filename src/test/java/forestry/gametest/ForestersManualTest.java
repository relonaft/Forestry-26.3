package forestry.gametest;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;

import forestry.api.ForestryConstants;
import forestry.api.core.machines.IForestryRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import forestry.core.features.CoreItems;
import forestry.core.platform.book.Book;
import forestry.core.platform.book.BookCategory;
import forestry.core.platform.book.BookEntry;
import forestry.core.platform.book.BookLoader;
import forestry.core.platform.book.BookPageData;
import forestry.core.platform.book.BookText;
import forestry.core.platform.book.PageType;
import forestry.core.platform.registration.FeatureRecipeType;
import forestry.core.platform.util.RecipeUtils;

// the Forester's Manual has no screen on a server, this checks all the screen would need to show every page
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class ForestersManualTest {
	// quirk kept: Forestry: Community Edition names three recipes and one texture it never had, the book shows an error there
	private static final Set<String> KNOWN_BROKEN = Set.of(
		"recipe forestry:fabricator",
		"recipe forestry:apatite_from_resource_storage_apatite",
		"recipe forestry:ingot_bronze_from_resource_storage_bronze",
		"texture forestry:textures/gui/almanac/scraps/alveary_2.png",
		// and two page titles are English text in place of a translation key
		"untranslated text Circuit Boards",
		"untranslated text Electron Tubes"
	);
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
	private final Map<String, String> english = readLang("en_us");
	private final Map<String, String> russian = readLang("ru_ru");
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

	// the item opens a client screen, a server must get through use without the client classes
	@GameTest(template = "empty")
	public static void useOnAServerLoadsNoClientCode(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, CoreItems.FORESTERS_MANUAL.stack());
		CoreItems.FORESTERS_MANUAL.item().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
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

	// a text is a translation key. Both languages must have it, with markup the engine knows and links that lead somewhere
	private void checkText(String where, String key) {
		if (key.isEmpty()) {
			return;
		}
		// not a key: Patchouli and the engine show such a text as it is, in every language
		if (!key.matches("[a-z0-9_.]+")) {
			this.problems.add("untranslated text " + key);
			return;
		}
		for (Map<String, String> lang : List.of(this.english, this.russian)) {
			String name = lang == this.english ? "en_us" : "ru_ru";
			String text = lang.get(key);
			if (text == null) {
				this.problems.add("text " + key + " is not in " + name + " (" + where + ")");
				continue;
			}
			for (BookText.Span span : BookText.parse(text, command -> this.problems.add("unknown markup $(" + command + ") in " + name + " " + key))) {
				if (span.link() != null && this.book.resolve(span.link()) == null) {
					this.problems.add("link " + span.link() + " leads nowhere, in " + name + " " + key);
				}
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
		if (key == null || RecipeUtils.getRecipes(recipes, type).noneMatch(recipe -> BuiltInRegistries.ITEM.getKey(recipe.getResultItem().getItem()).equals(key))) {
			this.problems.add(BuiltInRegistries.RECIPE_TYPE.getKey(type.type()) + " recipe for " + item);
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

	// the union over every jar, as a client merges the lang files of its packs
	private static Map<String, String> readLang(String lang) {
		String path = "assets/forestry/lang/" + lang + ".json";
		Map<String, String> texts = new HashMap<>();
		ModList.get().forEachModFile(modFile -> modFile.getContents().visitContent("assets/forestry/lang", (file, resource) -> {
			if (file.endsWith(path)) {
				try (Reader reader = resource.bufferedReader()) {
					GsonHelper.parse(reader).entrySet().forEach(entry -> texts.put(entry.getKey(), entry.getValue().getAsString()));
				} catch (Exception e) {
					throw new IllegalStateException("cannot read " + file, e);
				}
			}
		}));
		return texts;
	}
}
