package forestry.core.platform.book;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

public record BookCategory(Identifier id, String name, String description, String icon, @Nullable Identifier parent, int sortnum) {
}
