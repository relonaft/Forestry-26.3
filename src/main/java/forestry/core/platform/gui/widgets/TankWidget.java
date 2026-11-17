package forestry.core.platform.gui.widgets;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import forestry.api.core.IToolPipette;
import forestry.api.core.tooltips.ToolTip;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.gui.IContainerLiquidTanks;
import forestry.core.platform.gui.IContainerTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import forestry.core.platform.transfer.IFluidTank;
import org.joml.Matrix3x2f;

import javax.annotation.Nullable;

/**
 * Slot for liquid tanks
 */
public class TankWidget extends Widget {
	// the tank was drawn with blending turned off
	private static final RenderPipeline NO_BLEND = RenderPipelines.GUI_OPAQUE_TEXTURED_BACKGROUND;

	private int overlayTexX = 176;
	private int overlayTexY = 0;
	private final int slot;
	protected boolean drawOverlay = true;

	public TankWidget(WidgetManager manager, int xPos, int yPos, int slot) {
		super(manager, xPos, yPos);
		this.slot = slot;
		this.height = 58;
	}

	public TankWidget setOverlayOrigin(int x, int y) {
        this.overlayTexX = x;
        this.overlayTexY = y;
		return this;
	}

	@Nullable
	public IFluidTank getTank() {
		AbstractContainerMenu container = this.manager.gui.getMenu();
		if (container instanceof IContainerTank tanks) {
			return tanks.getTank(this.slot);
		}
		return null;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		IFluidTank tank = getTank();

		if (tank == null || tank.getCapacity() <= 0) {
			return;
		}

		FluidStack contents = tank.getFluid();

		if (!contents.isEmpty() && contents.getAmount() > 0 && contents.getFluid() != null) {
			Fluid fluid = contents.getFluid();

			if (fluid != null) {
				// the still texture and the tint moved from the fluid type extensions to the fluid model
				FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
				TextureAtlasSprite fluidStillSprite = fluidModel.stillMaterial().sprite();
				FluidTintSource tintSource = fluidModel.fluidTintSource();

				int fluidColor = ARGB.opaque(tintSource == null ? 0xffffff : tintSource.colorAsStack(contents));

				int scaledAmount = contents.getAmount() * this.height / tank.getCapacity();
				if (contents.getAmount() > 0 && scaledAmount < 1) {
					scaledAmount = 1;
				}
				if (scaledAmount > this.height) {
					scaledAmount = this.height;
				}

				final int xTileCount = this.width / 16;
				final int xRemainder = this.width - xTileCount * 16;
				final int yTileCount = scaledAmount / 16;
				final int yRemainder = scaledAmount - yTileCount * 16;

				final int yStart = startY + this.height;

				for (int xTile = 0; xTile <= xTileCount; xTile++) {
					for (int yTile = 0; yTile <= yTileCount; yTile++) {
						int width = xTile == xTileCount ? xRemainder : 16;
						int height = yTile == yTileCount ? yRemainder : 16;
						int x = startX + xTile * 16;
						int y = yStart - (yTile + 1) * 16;
						if (width > 0 && height > 0) {
							int maskTop = 16 - height;
							int maskRight = 16 - width;

							drawFluidTexture(graphics, x + this.xPos, y + this.yPos, fluidStillSprite, maskTop, maskRight, fluidColor);
						}
					}
				}
			}
		}

		if (this.drawOverlay) {
			graphics.blit(NO_BLEND, this.manager.gui.textureFile, startX + this.xPos, startY + this.yPos, this.overlayTexX, this.overlayTexY, 16, 60, 256, 256);
		}
	}

	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		IFluidTank tank = getTank();
		if (!(tank instanceof StandardTank standardTank)) {
			return null;
		}
		return standardTank.getToolTip();
	}

	private static void drawFluidTexture(GuiGraphicsExtractor graphics, int xCoord, int yCoord, TextureAtlasSprite textureSprite, int maskTop, int maskRight, int color) {
		float uMin = textureSprite.getU0();
		float uMax = textureSprite.getU1();
		float vMin = textureSprite.getV0();
		float vMax = textureSprite.getV1();
		uMax = uMax - maskRight / 16.0F * (uMax - uMin);
		vMax = vMax - maskTop / 16.0F * (vMax - vMin);

		// a plain blit takes either custom uv or a tint, not both
		AbstractTexture atlas = Minecraft.getInstance().getTextureManager().getTexture(textureSprite.atlasLocation());
		graphics.submitGuiElementRenderState(new BlitRenderState(
			NO_BLEND,
			TextureSetup.singleTexture(atlas.getTextureView(), atlas.getSampler()),
			new Matrix3x2f(graphics.pose()),
			xCoord, yCoord + maskTop, xCoord + 16 - maskRight, yCoord + 16,
			uMin, uMax, vMin, vMax,
			color,
			graphics.peekScissorStack()
		));
	}

	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
		Player player = this.manager.minecraft.player;
		ItemStack itemstack = player.containerMenu.getCarried();
		if (itemstack.isEmpty()) {
			return;
		}

		Item held = itemstack.getItem();
		AbstractContainerMenu container = this.manager.gui.getMenu();
		if (held instanceof IToolPipette && container instanceof IContainerLiquidTanks) {
			((IContainerLiquidTanks) container).handlePipetteClickClient(this.slot, player);
		}
	}
}
