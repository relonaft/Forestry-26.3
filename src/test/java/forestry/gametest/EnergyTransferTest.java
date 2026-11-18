package forestry.gametest;

import forestry.api.ForestryConstants;
import forestry.core.content.energy.blocks.EngineBlockType;
import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.energy.inventory.PeatEngineInventory;
import forestry.core.content.energy.tiles.PeatEngineBlockEntity;
import forestry.core.content.machines.blocks.FactoryBlockType;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.features.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

// machines take energy through the NeoForge energy capability, so generators of other mods can feed them
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class EnergyTransferTest {
	private static final BlockPos ENGINE = new BlockPos(8, 1, 8);
	private static final BlockPos MACHINE = ENGINE.above();

	// what a generator of another mod does: insert energy through the capability and commit
	@GameTest(template = "empty")
	public static void machineAcceptsEnergyThroughTheCapability(GameTestHelper helper) {
		helper.setBlock(MACHINE, FactoryBlocks.PLAIN.get(FactoryBlockType.CARPENTER).block());
		EnergyHandler machine = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(MACHINE), Direction.DOWN);
		helper.assertTrue(machine != null, "the carpenter exposes no energy handler");

		long before = machine.getAmountAsLong();
		int inserted;
		try (Transaction transaction = Transaction.openRoot()) {
			inserted = machine.insert(500, transaction);
			transaction.commit();
		}
		helper.assertTrue(inserted > 0, "the carpenter accepted no energy");
		helper.assertTrue(machine.getAmountAsLong() == before + inserted, "stored energy " + machine.getAmountAsLong() + " does not match the " + inserted + " inserted");

		// an aborted transaction must leave the machine as it was
		long committed = machine.getAmountAsLong();
		try (Transaction transaction = Transaction.openRoot()) {
			machine.insert(500, transaction);
		}
		helper.assertTrue(machine.getAmountAsLong() == committed, "an aborted insert changed the stored energy");
		helper.succeed();
	}

	// a peat engine with fuel and a redstone signal pushes energy into the machine it faces
	@GameTest(template = "empty", timeoutTicks = 1200)
	public static void peatEngineFeedsTheMachineAbove(GameTestHelper helper) {
		helper.setBlock(ENGINE, EnergyBlocks.ENGINES.get(EngineBlockType.PEAT).block());
		helper.setBlock(MACHINE, FactoryBlocks.PLAIN.get(FactoryBlockType.CARPENTER).block());
		helper.setBlock(ENGINE.east(), Blocks.REDSTONE_BLOCK);
		PeatEngineBlockEntity engine = (PeatEngineBlockEntity) helper.getBlockEntity(ENGINE, BlockEntity.class);
		engine.getInternalInventory().setItem(PeatEngineInventory.SLOT_FUEL, new ItemStack(CoreItems.PEAT.item(), 8));

		helper.succeedWhen(() -> {
			EnergyHandler machine = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(MACHINE), Direction.DOWN);
			helper.assertTrue(machine != null && machine.getAmountAsLong() > 0, "the carpenter got no energy from the peat engine yet");
		});
	}
}
