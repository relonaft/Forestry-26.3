package forestry.core.platform.villager;

import forestry.api.ForestryConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.trading.TradeSet;

public class VillagerTradeSets {
	// the sets and their trades are data: data/forestry/trade_set and data/forestry/villager_trade
	public static Int2ObjectMap<ResourceKey<TradeSet>> levels(String profession, int maxLevel) {
		Int2ObjectOpenHashMap<ResourceKey<TradeSet>> sets = new Int2ObjectOpenHashMap<>();
		for (int level = 1; level <= maxLevel; level++) {
			sets.put(level, ResourceKey.create(Registries.TRADE_SET, ForestryConstants.forestry(profession + "/level_" + level)));
		}
		return sets;
	}
}
