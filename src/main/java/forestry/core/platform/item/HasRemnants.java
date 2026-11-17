package forestry.core.platform.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface HasRemnants {
	// since 1.21.5 a tool is an Item whose properties carry the tool, weapon and attribute components

	class Pickaxe extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Pickaxe(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.pickaxe(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<ItemStack> onBroken) {
			if (stack.getDamageValue() + amount >= stack.getMaxDamage()) {
				if (entity instanceof Player player) {
					// make sure it's really broken
					stack.shrink(1);
					player.getInventory().add(this.remnants.get());
				}
			}
			return super.damageItem(stack, amount, entity, onBroken);
		}
	}

	class Shovel extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Shovel(ToolMaterial material, float damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.shovel(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<ItemStack> onBroken) {
			if (stack.getDamageValue() + amount >= stack.getMaxDamage()) {
				if (entity instanceof Player player) {
					// make sure it's really broken
					stack.shrink(1);
					player.getInventory().add(this.remnants.get());
				}
			}
			return super.damageItem(stack, amount, entity, onBroken);
		}
	}

	class Axe extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Axe(ToolMaterial material, float damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.axe(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<ItemStack> onBroken) {
			if (stack.getDamageValue() + amount >= stack.getMaxDamage()) {
				if (entity instanceof Player player) {
					// make sure it's really broken
					stack.shrink(1);
					player.getInventory().add(this.remnants.get());
				}
			}
			return super.damageItem(stack, amount, entity, onBroken);
		}
	}

	class Sword extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Sword(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.sword(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<ItemStack> onBroken) {
			if (stack.getDamageValue() + amount >= stack.getMaxDamage()) {
				if (entity instanceof Player player) {
					// make sure it's really broken
					stack.shrink(1);
					player.getInventory().add(this.remnants.get());
				}
			}
			return super.damageItem(stack, amount, entity, onBroken);
		}
	}

	class Hoe extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Hoe(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.hoe(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<ItemStack> onBroken) {
			if (stack.getDamageValue() + amount >= stack.getMaxDamage()) {
				if (entity instanceof Player player) {
					// make sure it's really broken
					stack.shrink(1);
					player.getInventory().add(this.remnants.get());
				}
			}
			return super.damageItem(stack, amount, entity, onBroken);
		}
	}
}
