package forestry.core.platform.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.ForestryConstants;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A global loot modifier used by forestry to inject the additional chest loot to the vanilla loot tables.
 */
public class ConditionLootModifier extends LootModifier {
	public static final MapCodec<ConditionLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).and(instance.group(
		Identifier.CODEC.fieldOf("table").forGetter(lm -> lm.tableLocation),
		Codec.list(Codec.STRING).fieldOf("extensions").forGetter(o -> o.extensions)
	)).apply(instance, ConditionLootModifier::new));

	private final Identifier tableLocation;
	private final List<String> extensions;

	/**
	 * todo is this still necessary?
	 * Helper field to prevent an endless method loop caused by forge in {@link LootTable#getRandomItems(LootContext, Consumer)}
	 * which calls this method again, since it keeps the {@link LootContext#getQueriedLootTableId()} value, which causes
	 * "getRandomItems" to calling this method again, because the conditions still met even that it is an other loot
	 * table.
	 */
	private boolean operates = false;

	public ConditionLootModifier(Identifier location, List<String> extensions) {
		this(Optional.empty(), DEFAULT_PRIORITY, location, extensions);
	}

	private static Optional<Holder<LootItemCondition>> merge(Optional<Holder<LootItemCondition>> condition, Identifier location) {
		Holder<LootItemCondition> tableCondition = Holder.direct(LootTableIdCondition.builder(location).build());
		return ConditionUserBuilder.buildCondition(condition.map(c -> List.of(c, tableCondition)).orElse(List.of(tableCondition)));
	}

	private ConditionLootModifier(Optional<Holder<LootItemCondition>> condition, int priority, Identifier location, List<String> extensions) {
		super(merge(condition, location), priority);
		this.tableLocation = location;
		this.extensions = extensions;
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		if (this.operates) {
			return generatedLoot;
		}

        this.operates = true;

		for (String extension : this.extensions) {
			Identifier location = ForestryConstants.forestry(this.tableLocation.getPath() + "/" + extension);
			ResourceKey<LootTable> tableKey = ResourceKey.create(Registries.LOOT_TABLE, location);
			LootTable table = context.getResolver().lookup(Registries.LOOT_TABLE)
				.flatMap(r -> r.get(tableKey))
				.map(Holder::value)
				.orElse(LootTable.EMPTY);

			if (table != LootTable.EMPTY) {
				table.getRandomItems(context, generatedLoot::add);
			}
		}

        this.operates = false;
		return generatedLoot;
	}

	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
