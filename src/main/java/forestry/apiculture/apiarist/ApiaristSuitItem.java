package forestry.apiculture.apiarist;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.IBeeProtection;
import forestry.api.apiculture.genetics.IBeeEffect;
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
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ApiaristSuitItem extends Item {
	// an armor material names its repair items by tag since 1.21.2, the tag holds woven silk
	private static final TagKey<Item> REPAIR_ITEMS = TagKey.create(Registries.ITEM, ForestryConstants.forestry("repairs_apiarist_armor"));
	// 5 is the durability factor of the armor material in Forestry 5.8.2. The 1.21.1 code set no durability
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
		super(new Item.Properties().humanoidArmor(APIARIST_ARMOR_MATERIAL, type));
	}
}
