package forestry.core.content.backpacks;

import com.google.common.base.Preconditions;
import forestry.api.core.backpacks.EnumBackpackType;
import forestry.api.core.backpacks.IBackpackDefinition;
import forestry.api.core.backpacks.IBackpackInterface;
import forestry.core.content.backpacks.items.BackpackItem;
import forestry.core.content.backpacks.items.NaturalistBackpackItem;
import forestry.core.platform.registration.PendingId;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.function.Predicate;

public class BackpackInterface implements IBackpackInterface {
	@Override
	public Item createBackpack(IBackpackDefinition definition, EnumBackpackType type) {
		Preconditions.checkNotNull(definition, "definition must not be null");
		Preconditions.checkNotNull(type, "type must not be null");
		Preconditions.checkArgument(type != EnumBackpackType.NATURALIST, "type must not be NATURALIST. Use createNaturalistBackpack instead.");

		return new BackpackItem(definition, type, new Item.Properties()
			.stacksTo(1)
			.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
	}

	@Override
	public Item createNaturalistBackpack(IBackpackDefinition definition, Identifier speciesTypeId) {
		Preconditions.checkNotNull(definition, "definition must not be null");
		Preconditions.checkNotNull(speciesTypeId, "rootUid must not be null");

		return new NaturalistBackpackItem(speciesTypeId, definition, new Item.Properties()
			.stacksTo(1)
			.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
	}

	// deviation: the id overloads are new, a 26.3 item needs its key while it is constructed
	@Override
	public Item createBackpack(Identifier id, IBackpackDefinition definition, EnumBackpackType type) {
		Preconditions.checkNotNull(id, "id must not be null");
		return PendingId.construct(Registries.ITEM, id, () -> createBackpack(definition, type));
	}

	@Override
	public Item createNaturalistBackpack(Identifier id, IBackpackDefinition definition, Identifier speciesTypeId) {
		Preconditions.checkNotNull(id, "id must not be null");
		return PendingId.construct(Registries.ITEM, id, () -> createNaturalistBackpack(definition, speciesTypeId));
	}

	@Override
	public Predicate<ItemStack> createBackpackFilter(TagKey<Item> allowTag, TagKey<Item> rejectTag) {
		Preconditions.checkNotNull(allowTag, "allowTag must not be null");
		Preconditions.checkNotNull(rejectTag, "rejectTag must not be null");

		return new BackpackFilter(allowTag, rejectTag);
	}

	@Override
	public Predicate<ItemStack> createNaturalistBackpackFilter(Identifier speciesRootUid) {
		return new NaturalistBackpackFilter(speciesRootUid);
	}
}
