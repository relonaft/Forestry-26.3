package forestry.core.platform.block;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class SpecialRendererBlock<P extends IBlockType> extends BaseBlock<P> {
	public SpecialRendererBlock(P blockType, Properties properties) {
		super(properties.noOcclusion(), blockType);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		// ENTITYBLOCK_ANIMATED is gone, the block model holds only the particle texture
		return RenderShape.MODEL;
	}
}
