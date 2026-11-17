package forestry.api.core.machines.fuels;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Data map value for items usable as a resource by the Moistener.
 *
 * @param template       The template of the item that leaves the moistener's working slot (i.e. mouldy wheat,
 *                       decayed wheat, mulch). A template, because data maps are read before the game binds item
 *                       components and an item stack cannot be built then
 * @param stage          What stage this product represents. Resources with lower stage value are consumed first
 * @param moistenerValue How much this item contributes to the final product of the moistener (i.e. mycelium)
 */
public record MoistenerFuel(ItemStackTemplate template, int stage, int moistenerValue) {
	/**
	 * @param product The item that leaves the moistener's working slot. Must not be empty.
	 */
	public MoistenerFuel(ItemStack product, int stage, int moistenerValue) {
		this(ItemStackTemplate.fromNonEmptyStack(product), stage, moistenerValue);
	}

	/**
	 * @return A new stack of the item that leaves the moistener's working slot
	 */
	public ItemStack product() {
		return this.template.create();
	}

	public static final Codec<MoistenerFuel> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ItemStackTemplate.CODEC.fieldOf("product").forGetter(MoistenerFuel::template),
			Codec.INT.fieldOf("stage").forGetter(MoistenerFuel::stage),
			Codec.INT.fieldOf("moistener_value").forGetter(MoistenerFuel::moistenerValue)
	).apply(inst, MoistenerFuel::new));
}
