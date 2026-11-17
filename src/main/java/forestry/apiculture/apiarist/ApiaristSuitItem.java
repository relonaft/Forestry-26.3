package forestry.apiculture.apiarist;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.IBeeProtection;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.core.platform.registration.PendingId;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ApiaristSuitItem extends Item {
	// an armor material names its repair items by tag since 1.21.2, the tag holds woven silk
	private static final TagKey<Item> REPAIR_ITEMS = TagKey.create(Registries.ITEM, ForestryConstants.forestry("repairs_apiarist_armor"));
	// the durability factor is unused, the constructor skips humanoidArmor
	private static final ArmorMaterial APIARIST_ARMOR_MATERIAL = new ArmorMaterial(
		5,
		Map.of(
			ArmorType.BOOTS, 1,
			ArmorType.LEGGINGS, 2,
			ArmorType.CHESTPLATE, 3,
			ArmorType.HELMET, 1,
			ArmorType.BODY, 3
		),
		15,
		SoundEvents.ARMOR_EQUIP_LEATHER,
		0.0F,
		0.0F,
		REPAIR_ITEMS,
		ResourceKey.create(EquipmentAssets.ROOT_ID, ForestryConstants.forestry("apiarist_armor"))
	);

	public enum BeeProtection implements IBeeProtection {
		INSTANCE;

		@Override
		public boolean protectEntity(LivingEntity entity, ItemStack armor, @Nullable IBeeEffect cause, boolean doProtect) {
			return true;
		}
	}

	public ApiaristSuitItem(ArmorType type) {
		// quirk kept: Community Edition 1.21.1 set no durability, so the suit never wears out and stacks to 64
		super(PendingId.item(new Item.Properties()
			.attributes(APIARIST_ARMOR_MATERIAL.createAttributes(type))
			.enchantable(APIARIST_ARMOR_MATERIAL.enchantmentValue())
			.component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot())
				.setEquipSound(APIARIST_ARMOR_MATERIAL.equipSound())
				.setAsset(APIARIST_ARMOR_MATERIAL.assetId())
				.build())
			.repairable(APIARIST_ARMOR_MATERIAL.repairIngredient())));
	}
}
