package forestry.core.content.lighting;

import forestry.core.platform.registration.PendingId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class WaterloggableTorchBlock extends TorchBlock implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	// deviation: dust color is packed 8-bit rgb since 1.21.2, was (0.68, 1.0, 0.89)
	private static final DustParticleOptions PHOSPHOR_PARTICLE = new DustParticleOptions(0xADFFE3, 0.8f);

	public WaterloggableTorchBlock(BlockBehaviour.Properties properties) {
		super(ParticleTypes.SOUL_FIRE_FLAME, PendingId.block(properties));
		this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WATERLOGGED);
	}

	// Phosphor Torches don't 'burn' per se so don't emit smoke
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		double d0 = (double) pos.getX() + 0.5D;
		double d1 = (double) pos.getY() + 0.65D;
		double d2 = (double) pos.getZ() + 0.5D;
		level.addParticle(PHOSPHOR_PARTICLE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTicks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			scheduledTicks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, scheduledTicks, pos, direction, neighborPos, neighborState, random);
	}
}
