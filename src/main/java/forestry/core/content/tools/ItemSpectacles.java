package forestry.core.content.tools;

import forestry.api.ForestryConstants;
import forestry.api.core.ISpectacleVision;
import forestry.core.platform.capabilities.SpectacleVision;
import forestry.core.platform.config.Constants;
import forestry.core.platform.util.ItemTooltipUtil;
import forestry.core.platform.util.TooltipList;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.*;
import java.util.List;

public class ItemSpectacles extends ArmorItem {
	public static final String TEXTURE_NATURALIST_ARMOR_PRIMARY = ForestryConstants.MOD_ID + ":" + Constants.TEXTURE_PATH_ITEM + "/naturalist_armor_1.png";
	public static final ISpectacleVision SPECTACLE_VISION = SpectacleVision.INSTANCE;

	public ItemSpectacles() {
		super(ArmorMaterials.LEATHER, Type.HELMET, (new Item.Properties()).durability(100));
	}

	@Override
	public Identifier getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		return Identifier.parse(TEXTURE_NATURALIST_ARMOR_PRIMARY);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag advanced) {
		List<Component> tooltip = new TooltipList(adder);
		ItemTooltipUtil.addInformation(stack, tooltip);
	}
}
