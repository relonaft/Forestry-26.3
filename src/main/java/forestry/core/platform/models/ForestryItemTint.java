package forestry.core.platform.models;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.core.platform.item.ITintedItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

// one tint layer of an ITintedItem, listed under "tints" in its item model definition
public record ForestryItemTint(int index) implements ItemTintSource {
	public static final MapCodec<ForestryItemTint> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.INT.optionalFieldOf("index", 0).forGetter(ForestryItemTint::index)
	).apply(instance, ForestryItemTint::new));

	public static int getColor(ItemStack stack, int tintIndex) {
		int color = 0xffffff;
		if (stack.getItem() instanceof ITintedItem tintedItem) {
			color = tintedItem.getColorFromItemStack(stack, tintIndex);
		}
		// ITintedItem returns 0xRRGGBB, alpha 0 would draw the layer invisible
		return color | 0xff000000;
	}

	@Override
	public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
		return getColor(stack, this.index);
	}

	@Override
	public MapCodec<ForestryItemTint> type() {
		return MAP_CODEC;
	}
}
