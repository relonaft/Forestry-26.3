package forestry.core.platform.book;

import javax.annotation.Nullable;
import java.util.Set;

// the page types of the Patchouli format this book uses, with the keys the pages read
public enum PageType {
	TEXT("text", "text"),
	CRAFTING("crafting", "recipe", "recipe2", "title", "text"),
	// link_recipe is accepted and unused, the book has no lookup from a recipe item to its entry
	SPOTLIGHT("spotlight", "item", "title", "text", "link_recipe"),
	IMAGE("image", "images", "title", "text"),
	MULTIBLOCK("multiblock", "name", "multiblock", "text"),
	CARPENTER_SINGLE("forestry:carpenter/single", "item", "text"),
	CARPENTER_DOUBLE("forestry:carpenter/double", "item", "item2"),
	FABRICATOR_SINGLE("forestry:fabricator/single", "item", "text"),
	FABRICATOR_DOUBLE("forestry:fabricator/double", "item", "item2"),
	// the farm layout templates never read the size keys their pages carry
	FARM_LAYOUT("forestry:farm_layout", "image", "text", "width", "height", "texture_width", "texture_height"),
	FARM_LAYOUT_LARGE("forestry:farm_layout_large", "image", "text", "width", "height", "texture_width", "texture_height"),
	FARM_GUI("forestry:farm_gui", "text");

	private static final String PATCHOULI_PREFIX = "patchouli:";

	private final String id;
	private final Set<String> keys;

	PageType(String id, String... keys) {
		this.id = id;
		this.keys = Set.of(keys);
	}

	public Set<String> keys() {
		return this.keys;
	}

	@Nullable
	public static PageType byId(String id) {
		String name = id.startsWith(PATCHOULI_PREFIX) ? id.substring(PATCHOULI_PREFIX.length()) : id;
		for (PageType type : values()) {
			if (type.id.equals(name)) {
				return type;
			}
		}
		return null;
	}
}
