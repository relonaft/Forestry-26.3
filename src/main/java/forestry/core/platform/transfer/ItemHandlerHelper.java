package forestry.core.platform.transfer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class ItemHandlerHelper {
	private ItemHandlerHelper() {
	}

	private static ItemStack insertItem(@Nullable IItemHandler dest, ItemStack stack, boolean simulate) {
		if (dest == null || stack.isEmpty()) {
			return stack;
		}

		for (int slot = 0; slot < dest.getSlots(); slot++) {
			stack = dest.insertItem(slot, stack, simulate);
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return stack;
	}

	// fills up stacks that are already there before it uses empty slots
	public static ItemStack insertItemStacked(@Nullable IItemHandler inventory, ItemStack stack, boolean simulate) {
		if (inventory == null || stack.isEmpty()) {
			return stack;
		}
		if (!stack.isStackable()) {
			return insertItem(inventory, stack, simulate);
		}

		int slots = inventory.getSlots();
		for (int slot = 0; slot < slots; slot++) {
			if (ItemStack.isSameItemSameComponents(inventory.getStackInSlot(slot), stack)) {
				stack = inventory.insertItem(slot, stack, simulate);
				if (stack.isEmpty()) {
					break;
				}
			}
		}

		if (!stack.isEmpty()) {
			for (int slot = 0; slot < slots; slot++) {
				if (inventory.getStackInSlot(slot).isEmpty()) {
					stack = inventory.insertItem(slot, stack, simulate);
					if (stack.isEmpty()) {
						break;
					}
				}
			}
		}
		return stack;
	}

	// what does not fit into the main inventory drops at the feet of the player
	public static void giveItemToPlayer(Player player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		Level level = player.level();
		ItemStack remainder = insertItemStacked(new PlayerMainInventory(player.getInventory()), stack, false);

		if (remainder.isEmpty() || remainder.getCount() != stack.getCount()) {
			float pitch = ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F;
			level.playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, pitch);
		}

		if (!remainder.isEmpty() && !level.isClientSide()) {
			ItemEntity entity = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), remainder);
			entity.setPickUpDelay(40);
			entity.setDeltaMovement(entity.getDeltaMovement().multiply(0, 1, 0));
			level.addFreshEntity(entity);
		}
	}

	// the 36 main slots, without armor and offhand
	private static final class PlayerMainInventory extends InvWrapper {
		private final Inventory inventory;

		private PlayerMainInventory(Inventory inventory) {
			super(inventory);
			this.inventory = inventory;
		}

		private static boolean isMainSlot(int slot) {
			return slot < Inventory.INVENTORY_SIZE;
		}

		@Override
		public int getSlots() {
			return Inventory.INVENTORY_SIZE;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return isMainSlot(slot) ? super.getStackInSlot(slot) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!isMainSlot(slot)) {
				return stack;
			}
			ItemStack rest = super.insertItem(slot, stack, simulate);
			if (rest.getCount() != stack.getCount()) {
				ItemStack inSlot = getStackInSlot(slot);
				if (!inSlot.isEmpty()) {
					Player player = this.inventory.player;
					if (player.level().isClientSide()) {
						inSlot.setPopTime(Inventory.POP_TIME_DURATION);
					} else if (player instanceof ServerPlayer) {
						player.containerMenu.broadcastChanges();
					}
				}
			}
			return rest;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return isMainSlot(slot) ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return isMainSlot(slot) ? super.getSlotLimit(slot) : 0;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return isMainSlot(slot) && super.isItemValid(slot, stack);
		}
	}
}
