package forestry.core.platform.compat.patchouli.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;
import org.joml.Matrix3x2fStack;

@SuppressWarnings("unused")
public class FluidComponent implements ICustomComponent {
	public IVariable fluid;
	public IVariable amount;
	public IVariable max;
	public IVariable width;
	public IVariable height;

	private transient int x, y, w, h, level, maxLevel;
	private transient FluidStack fluidStack;

	@Override
	public void build(int componentX, int componentY, int pageNum) {
		this.x = componentX;
		this.y = componentY;
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
		Matrix3x2fStack stack = graphics.pose();
		stack.pushMatrix();
		if (!this.fluidStack.isEmpty()) {
			FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(this.fluidStack.getFluid().defaultFluidState());
			TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();
			FluidTintSource tintSource = fluidModel.fluidTintSource();
			Identifier spriteLocation = sprite.contents().name();
			Identifier fluidTexture = Identifier.fromNamespaceAndPath(spriteLocation.getNamespace(), "textures/" + spriteLocation.getPath() + ".png");
			int color = ARGB.opaque(tintSource == null ? 0xffffff : tintSource.colorAsStack(this.fluidStack));

			// MatrixStack transform, int x, int y, float u, float v, int width, int height, int ?, int ?
			graphics.blit(RenderPipelines.GUI_TEXTURED, fluidTexture, this.x, (int) (this.y + this.h - Math.floor(this.h * ((float) this.level / this.maxLevel))), sprite.getU0(), sprite.getV0(), this.w, this.h * this.level / this.maxLevel, 8, 8, color);

			if (context.isAreaHovered(mouseX, mouseY, this.x, this.y, this.w, this.h)) {
				List<Component> toolTips = new ArrayList<>();
				toolTips.add(this.fluidStack.getHoverName());
				toolTips.add(Component.translatable("for.gui.tooltip.liquid.amount", this.level, this.maxLevel));

				context.setHoverTooltipComponents(toolTips);
			}
		}

		stack.popMatrix();
	}

	@Override
	public void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
		int mb = lookup.apply(this.amount).asNumber().intValue();

		try {
			// the fluid is absent when the processor found no recipe to show
			Identifier id = Identifier.parse(lookup.apply(this.fluid).asString());
			this.fluidStack = new FluidStack(BuiltInRegistries.FLUID.get(id), mb);
		} catch (Exception e) {
			this.fluidStack = FluidStack.EMPTY;
		}

		this.w = lookup.apply(this.width).asNumber().intValue();
		this.h = lookup.apply(this.height).asNumber().intValue();
		this.level = lookup.apply(this.amount).asNumber().intValue();
		this.maxLevel = lookup.apply(this.max).asNumber().intValue();
	}
}
