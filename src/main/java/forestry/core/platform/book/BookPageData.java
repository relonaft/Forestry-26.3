package forestry.core.platform.book;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;

// type is null when the engine does not know typeId
public record BookPageData(String typeId, @Nullable PageType type, JsonObject json) {
	public boolean has(String key) {
		return this.json.has(key);
	}

	// empty for a missing key and for an array or object, a resource pack can put anything here
	public String string(String key) {
		JsonElement value = this.json.get(key);
		return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
	}

	// the anchors of this book are numbers, a link names them as text
	public String anchor() {
		return string("anchor");
	}
}
