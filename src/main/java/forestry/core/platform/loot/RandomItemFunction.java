package forestry.core.platform.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

// replaces the item with a random one from a set, for villager trades that sell a random item
public class RandomItemFunction extends LootItemConditionalFunction {
	public static final MapCodec<RandomItemFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.and(RegistryCodecs.holderSet(Registries.ITEM).fieldOf("items").forGetter(function -> function.items))
		.apply(instance, RandomItemFunction::new));

	private final HolderSet<Item> items;

	private RandomItemFunction(Optional<Holder<LootItemCondition>> condition, HolderSet<Item> items) {
		super(condition);
		this.items = items;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		return this.items.getRandomElement(context.getRandom())
			.map(item -> stack.transmuteCopy(item.value()))
			.orElse(ItemStack.EMPTY);
	}

	@Override
	public MapCodec<RandomItemFunction> codec() {
		return CODEC;
	}
}
