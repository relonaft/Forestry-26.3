package forestry.apiculture.bees;

import forestry.core.platform.config.Constants;
import forestry.core.platform.item.ItemForestryFood;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

public class ItemAmbrosia extends ItemForestryFood {

	public ItemAmbrosia() {
		super(new Item.Properties()
			.food(new FoodProperties.Builder()
				.alwaysEdible()
				.nutrition(Constants.FOOD_AMBROSIA_HEAL)
				.saturationModifier(0.6f)
				.build(),
				// the effect of a food is part of its consumable component since 1.21.2
				Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0), 1.0F)).build()));
	}

	@Override
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

}
