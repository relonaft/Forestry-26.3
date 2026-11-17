package forestry.api.core;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public interface IItemProvider<I extends Item> {
	I item();

	default ItemStack stack() {
		return stack(1);
	}

	default ItemStack stack(int amount) {
		return new ItemStack(item(), amount);
	}

	/**
	 * Used to describe a stack before the game binds item components, when {@link #stack()} cannot be called.
	 *
	 * @return The template of a stack of one item
	 */
	default ItemStackTemplate template() {
		return template(1);
	}

	/**
	 * Used to describe a stack before the game binds item components, when {@link #stack(int)} cannot be called.
	 *
	 * @param amount The number of items
	 * @return The template of a stack of the given size
	 */
	default ItemStackTemplate template(int amount) {
		return new ItemStackTemplate(item(), amount);
	}

	default boolean itemEqual(ItemStack stack) {
		return !stack.isEmpty() && itemEqual(stack.getItem());
	}

	default boolean itemEqual(Item item) {
		return item() == item;
	}
}
