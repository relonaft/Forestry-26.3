package forestry.core.platform.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;

import java.util.Optional;

public final class FuelUtil {
	private FuelUtil() {
	}

	public static boolean isFuel(ItemStack stack) {
		return stack.has(DataComponents.COOKING_FUEL);
	}

	// the same context a furnace uses, so a provider that reads the block sees the machine
	public static <T extends BlockEntity & Container> int getBurnTime(ServerLevel level, T tile, ItemStack stack) {
		LootContext context = new LootContext.Builder(new LootParams.Builder(level)
			.withParameter(LootContextParams.BLOCK_STATE, tile.getBlockState())
			.withParameter(LootContextParams.BLOCK_ENTITY, tile)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(tile.getBlockPos()))
			.withParameter(LootContextParams.CONTAINER, tile)
			.withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, stack.isEmpty() ? null : stack)
			.create(LootContextParamSets.CONTAINER_PROCESS))
			.create(Optional.empty());
		return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
	}
}
