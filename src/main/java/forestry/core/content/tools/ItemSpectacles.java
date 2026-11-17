package forestry.core.content.tools;

import forestry.api.ForestryConstants;
import forestry.api.core.ISpectacleVision;
import forestry.core.platform.capabilities.SpectacleVision;
import forestry.core.platform.config.Constants;
import forestry.core.platform.util.ItemTooltipUtil;
import forestry.core.platform.util.TooltipList;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.*;
import java.util.List;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class ItemSpectacles extends Item {
	// leather armor with its own texture, which is an equipment asset since 1.21.2
	private static final ArmorMaterial NATURALIST_ARMOR_MATERIAL = new ArmorMaterial(
		ArmorMaterials.LEATHER.durability(),
		ArmorMaterials.LEATHER.defense(),
		ArmorMaterials.LEATHER.enchantmentValue(),
		ArmorMaterials.LEATHER.equipSound(),
		ArmorMaterials.LEATHER.toughness(),
		ArmorMaterials.LEATHER.knockbackResistance(),
		ArmorMaterials.LEATHER.repairIngredient(),
		ResourceKey.create(EquipmentAssets.ROOT_ID, ForestryConstants.forestry("naturalist_armor"))
	);

	public static final String TEXTURE_NATURALIST_ARMOR_PRIMARY = ForestryConstants.MOD_ID + ":" + Constants.TEXTURE_PATH_ITEM + "/naturalist_armor_1.png";
	public static final ISpectacleVision SPECTACLE_VISION = SpectacleVision.INSTANCE;

	public ItemSpectacles() {
		super(new Item.Properties().humanoidArmor(NATURALIST_ARMOR_MATERIAL, ArmorType.HELMET).durability(100));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag advanced) {
		List<Component> tooltip = new TooltipList(adder);
		ItemTooltipUtil.addInformation(stack, tooltip);
	}
}
