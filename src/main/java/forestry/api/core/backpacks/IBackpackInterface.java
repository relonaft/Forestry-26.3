
package forestry.api.core.backpacks;

import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * The Backpack Interface allows you to create your own backpacks.
 * Get the instance from {@link forestry.api.IForestryApi#getBackpackInterface()}.
 * <p>
 * To create your own backpack, create an {@link IBackpackDefinition}.
 * Backpack definitions have a filter, which you can create here with
 * {@link #createBackpackFilter(TagKey, TagKey)}
 * or {@link #createNaturalistBackpackFilter(Identifier)}
 * or create your own.
 * <p>
 * After you've created your backpack definition, create the item with
 * {@link #createBackpack(Identifier, IBackpackDefinition, EnumBackpackType)}
 * or {@link #createNaturalistBackpack(Identifier, IBackpackDefinition, Identifier)}
 * inside your item registration, for example
 * {@code ITEMS.register("my_backpack", id -> api.createBackpack(id, definition, type))}.
 */
public interface IBackpackInterface {

	/**
	 * Creates a backpack with the given UID and type, returning the item.
	 * Only works inside Forestry's own item registration, addons use
	 * {@link #createBackpack(Identifier, IBackpackDefinition, EnumBackpackType)}.
	 *
	 * @param definition The backpack definition.
	 * @param type       Type of backpack.
	 * @return Created backpack item.
	 */
	Item createBackpack(IBackpackDefinition definition, EnumBackpackType type);

	/**
	 * Creates a backpack item that will be registered under the given id.
	 * Since Minecraft 1.21.2 an item must know its id when it is constructed, so addons call this
	 * from their item registration instead of {@link #createBackpack(IBackpackDefinition, EnumBackpackType)},
	 * which only works inside Forestry's own registration.
	 *
	 * @param id         The registry name the item will be registered under.
	 * @param definition The backpack definition.
	 * @param type       Type of backpack.
	 * @return Created backpack item.
	 */
	Item createBackpack(Identifier id, IBackpackDefinition definition, EnumBackpackType type);

	/**
	 * Create a backpack that can hold items from a specific {@link ISpeciesType}.
	 * Only works inside Forestry's own item registration, addons use
	 * {@link #createNaturalistBackpack(Identifier, IBackpackDefinition, Identifier)}.
	 *
	 * @param definition    The backpack definition.
	 * @param speciesTypeId The species root.
	 * @return Created backpack item.
	 */
	Item createNaturalistBackpack(IBackpackDefinition definition, Identifier speciesTypeId);

	/**
	 * Creates a naturalist backpack item that will be registered under the given id.
	 * See {@link #createBackpack(Identifier, IBackpackDefinition, EnumBackpackType)} for why the id is needed.
	 *
	 * @param id            The registry name the item will be registered under.
	 * @param definition    The backpack definition.
	 * @param speciesTypeId The species root.
	 * @return Created backpack item.
	 */
	Item createNaturalistBackpack(Identifier id, IBackpackDefinition definition, Identifier speciesTypeId);

	/**
	 * Makes a new backpack filter that accepts items in the allow tag, unless they are also in the reject tag.
	 * Forestry's own backpacks use this filter with tags like {@code forestry:backpack/allow/miner}.
	 * Item tags load with data packs, so the filter reads the tags lazily on its first test.
	 *
	 * @param allowTag  The tag of items the backpack accepts
	 * @param rejectTag The tag of items the backpack rejects, even if they are in the allow tag
	 * @return a new backpack filter for the specified tags
	 * @since 3.0.0
	 */
	Predicate<ItemStack> createBackpackFilter(TagKey<Item> allowTag, TagKey<Item> rejectTag);

	/**
	 * Makes a new naturalist backpack filter. Only accepts items from a specific {@link ISpeciesType}.
	 * Useful for implementing {@link IBackpackDefinition} for naturalist's backpacks.
	 *
	 * @param speciesTypeId The species root's unique ID.
	 * @return a new backpack filter for the specified species root
	 * @see #createNaturalistBackpack(IBackpackDefinition, Identifier)
	 */
	Predicate<ItemStack> createNaturalistBackpackFilter(Identifier speciesTypeId);
}
