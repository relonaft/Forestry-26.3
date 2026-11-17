package forestry.arboriculture.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.IForestryApi;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.wood.ForestryWoodType;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

// replaces the item with a block of a random Forestry wood type, for the arborist trades
public class RandomWoodFunction extends LootItemConditionalFunction {
	public static final MapCodec<RandomWoodFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.and(StringRepresentable.fromEnum(WoodBlockKind::values).fieldOf("kind").forGetter(function -> function.kind))
		.apply(instance, RandomWoodFunction::new));

	private final WoodBlockKind kind;

	private RandomWoodFunction(Optional<Holder<LootItemCondition>> condition, WoodBlockKind kind) {
		super(condition);
		this.kind = kind;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		ForestryWoodType woodType = ForestryWoodType.getRandom(context.getRandom());
		return IForestryApi.INSTANCE.getTreeManager().getStack(woodType, this.kind, false);
	}

	@Override
	public MapCodec<RandomWoodFunction> codec() {
		return CODEC;
	}
}
