package forestry.apiculture.apiary;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.hives.IHiveFrame;
import forestry.api.core.genetics.IGenome;
import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.util.TooltipList;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import net.minecraft.world.item.component.TooltipDisplay;

public class HiveFrameItem extends ItemForestry implements IHiveFrame {
	private final HiveFrameBeeModifier beeModifier;

	public HiveFrameItem(int maxDamage, float geneticDecay) {
		super(new Item.Properties().durability(maxDamage));

		this.beeModifier = new HiveFrameBeeModifier(geneticDecay);
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return 64;
	}

	@Override
	public ItemStack frameUsed(IBeeHousing housing, ItemStack frame, IBee queen, int wear) {
		if (housing.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			frame.hurtAndBreak(wear, serverLevel, null, item -> {
			});
		}
		return frame.isEmpty() ? ItemStack.EMPTY : frame;
	}

	@Override
	public IBeeModifier getBeeModifier(ItemStack frame) {
		return this.beeModifier;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag advanced) {
		List<Component> tooltip = new TooltipList(adder);
		super.appendHoverText(stack, context, display, adder, advanced);
		this.beeModifier.addInformation(tooltip);
		if (!stack.isDamaged()) {
			tooltip.add(Component.translatable("item.forestry.durability", stack.getMaxDamage()));
		}
	}

	private record HiveFrameBeeModifier(float geneticDecay) implements IBeeModifier {
		private static final float production = 2f;

		@Override
		public float modifyProductionSpeed(IGenome genome, float currentSpeed) {
			return currentSpeed < 10f ? currentSpeed * production : 1f;
		}

		@Override
		public float modifyGeneticDecay(IGenome genome, float currentDecay) {
			return this.geneticDecay;
		}

		public void addInformation(List<Component> tooltip) {
			tooltip.add(Component.translatable("item.forestry.bee.modifier.production", production));
			tooltip.add(Component.translatable("item.forestry.bee.modifier.genetic.decay", this.geneticDecay));
		}
	}
}
