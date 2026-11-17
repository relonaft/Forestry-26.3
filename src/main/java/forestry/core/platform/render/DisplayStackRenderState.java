package forestry.core.platform.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class DisplayStackRenderState extends BlockEntityRenderState {
	public final ItemStackRenderState item = new ItemStackRenderState();
	public float smoothTick;
	public float groundScale;
}
