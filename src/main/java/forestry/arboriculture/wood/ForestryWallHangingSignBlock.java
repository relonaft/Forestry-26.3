package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.features.ArboricultureBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class ForestryWallHangingSignBlock extends WallHangingSignBlock implements IWoodTyped {
	private final ForestryWoodType type;

	public ForestryWallHangingSignBlock(ForestryWoodType type) {
		super(type.getWoodType(), wallVariant(ArboricultureBlocks.HANGING_SIGN.get(type).block()).mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());

		this.type = type;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.WALL_HANGING_SIGN;
	}

	@Override
	public boolean isFireproof() {
		return false;
	}

	@Override
	public IWoodType getWoodType() {
		return this.type;
	}

	// newBlockEntity / getTicker are inherited from WallHangingSignBlock and use vanilla
	// BlockEntityType.HANGING_SIGN. Forestry registers this block as a valid block for that
	// BE type via BlockEntityTypeAddBlocksEvent in ModuleArboriculture.

	// same as Blocks.wallVariant, the wall block drops and is named like the standing block
	private static Properties wallVariant(Block standing) {
		return Properties.of().overrideLootTable(standing.getLootTable()).overrideDescription(standing.getDescriptionId());
	}
}
