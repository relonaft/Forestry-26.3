package forestry.core.platform.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.core.features.CoreDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.HolderSetCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.stream.Stream;

// a can or capsule that holds the given fluid. Not a components ingredient: recipes load before fluid
// components are bound, so a fluid stack cannot be written into recipe JSON
public record FluidContainerIngredient(HolderSet<Item> itemSet, Holder<Fluid> fluid, int amount) implements ICustomIngredient {
	public static final MapCodec<FluidContainerIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false).fieldOf("items").forGetter(FluidContainerIngredient::itemSet),
		BuiltInRegistries.FLUID.holderByNameCodec().fieldOf("fluid").forGetter(FluidContainerIngredient::fluid),
		ExtraCodecs.POSITIVE_INT.optionalFieldOf("amount", FluidType.BUCKET_VOLUME).forGetter(FluidContainerIngredient::amount)
	).apply(instance, FluidContainerIngredient::new));

	@Override
	public boolean test(ItemStack stack) {
		if (!this.itemSet.contains(stack.typeHolder())) {
			return false;
		}
		SimpleFluidContent content = stack.get(CoreDataComponents.FLUID_CONTENT.get());
		return content != null && content.is(this.fluid) && content.getAmount() == this.amount;
	}

	@Override
	public Stream<Holder<Item>> items() {
		return this.itemSet.stream();
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IngredientType<?> getType() {
		return ForestryIngredientTypes.FLUID_CONTAINER.get();
	}

	@Override
	public SlotDisplay display() {
		// damage 1 is the filled container model, as FluidHandlerItemForestry sets it
		DataComponentPatch filled = DataComponentPatch.builder()
			.set(CoreDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(new FluidStack(this.fluid, this.amount)))
			.set(DataComponents.DAMAGE, 1)
			.build();
		return new SlotDisplay.Composite(this.itemSet.stream()
			.<SlotDisplay>map(item -> new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(item, 1, filled)))
			.toList());
	}
}
