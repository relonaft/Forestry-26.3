package forestry.core.platform.compat.curios.client;

import com.mojang.blaze3d.vertex.PoseStack;
import forestry.api.ForestryConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class SpectaclesCurioRenderer implements ICurioRenderer {
	private static final Identifier TEXTURE = ForestryConstants.forestry("textures/item/naturalist_armor_1.png");

	private final HumanoidModel<AvatarRenderState> armorModel;

	public SpectaclesCurioRenderer() {
		EntityModelSet models = Minecraft.getInstance().getEntityModels();

		// we don't care about slim model since we're just rendered on the head
		// the head armor layer holds only the head and hat parts
		this.armorModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_ARMOR.head()));
	}

	@Override
	public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(ItemStack stack, SlotContext context, PoseStack poseStack, SubmitNodeCollector collector, int light, S state, RenderLayerParent<S, M> parent, EntityRendererProvider.Context ctx, float yRotation, float xRotation) {
		if (state instanceof AvatarRenderState avatar) {
			// EquipmentLayerRenderer.renderLayers (with known args inlined), the model poses itself from the render state
			RenderType renderType = stack.hasFoil() ? RenderTypes.armorCutoutNoCullGlint(TEXTURE) : RenderTypes.armorCutoutNoCull(TEXTURE);
			collector.order(1).submitModel(this.armorModel, avatar, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, avatar.outlineColor);
		}
	}
}
