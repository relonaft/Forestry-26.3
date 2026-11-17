package forestry.core.content.decorative;

import forestry.core.platform.registration.PendingId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

// see BurnableBlock for copied fire code, datagen axisBlock needs a RotatedPillarBlock
public class PlywoodBlock extends RotatedPillarBlock {
	public PlywoodBlock(Properties properties) {
		super(PendingId.block(properties));
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 5;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 20;
	}

	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return true;
	}
}
