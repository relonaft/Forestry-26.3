package forestry.lepidopterology.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

public class ButterflyRenderState extends LivingEntityRenderState {
	@Nullable
	public Identifier texture;
	public float size = 1.0f;
	public float wingFlap;
	public boolean renderable;
}
