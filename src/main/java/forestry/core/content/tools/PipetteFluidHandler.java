package forestry.core.content.tools;

import forestry.core.features.CoreDataComponents;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;

// fluid handler of the pipette, one bucket in any amounts. Until 1.21.9 this was a FluidHandlerItemStack
public class PipetteFluidHandler extends ItemAccessFluidHandler {
	public PipetteFluidHandler(ItemAccess pipette) {
		super(pipette, CoreDataComponents.FLUID_CONTENT.get(), FluidType.BUCKET_VOLUME);
	}

	// the old handler did nothing for a stack of several pipettes
	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		return this.itemAccess.getAmount() == 1 ? super.insert(index, resource, amount, transaction) : 0;
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
		return this.itemAccess.getAmount() == 1 ? super.extract(index, resource, amount, transaction) : 0;
	}

	@Nullable
	@Override
	protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
		if (newAmount == 0) {
			// an emptied pipette has no content component, so it stacks with a new one
			return accessResource.without(this.component);
		}
		return super.update(accessResource, index, newResource, newAmount);
	}
}
