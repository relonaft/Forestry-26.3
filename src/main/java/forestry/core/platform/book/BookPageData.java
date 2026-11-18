package forestry.core.platform.book;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;

// type is null when the engine does not know typeId
public record BookPageData(String typeId, @Nullable PageType type, JsonObject json) {
	public boolean has(String key) {
		return this.json.has(key);
	}

	public String string(String key) {
		return GsonHelper.getAsString(this.json, key, "");
	}

	// the anchors of this book are numbers, a link names them as text
	public String anchor() {
		return this.json.has("anchor") ? this.json.get("anchor").getAsString() : "";
	}
}
