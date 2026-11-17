package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.features.ArboricultureTiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import javax.annotation.Nullable;

public class ForestryWallSignBlock extends WallSignBlock implements IWoodTyped {
	private final ForestryWoodType type;

	public ForestryWallSignBlock(ForestryWoodType type) {
		super(type.getWoodType(), wallVariant(ArboricultureBlocks.SIGN.get(type).block()).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());

		this.type = type;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.WALL_SIGN;
	}

	@Override
	public boolean isFireproof() {
		return false;
	}

	@Override
	public IWoodType getWoodType() {
		return this.type;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actual) {
		return createTickerHelper(actual, ArboricultureTiles.SIGN.tileType(), SignBlockEntity::tick);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SignBlockEntity(ArboricultureTiles.SIGN.tileType(), pos, state);
	}

	// same as Blocks.wallVariant, the wall block drops and is named like the standing block
	private static Properties wallVariant(Block standing) {
		return Properties.of().overrideLootTable(standing.getLootTable()).overrideDescription(standing.getDescriptionId());
	}
}
