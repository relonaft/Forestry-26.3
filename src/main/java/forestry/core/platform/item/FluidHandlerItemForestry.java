package forestry.core.platform.item;

import forestry.core.features.CoreDataComponents;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;

// fluid handler of a can or capsule. It holds one bucket, takes and gives only the whole bucket, and the
// container is used up when it is emptied. Until 1.21.9 this was a FluidHandlerItemStackSimple.Consumable
public class FluidHandlerItemForestry extends ItemAccessFluidHandler {
	private final FluidContainerType containerType;

	public FluidHandlerItemForestry(ItemAccess container, FluidContainerType containerType) {
		super(container, CoreDataComponents.FLUID_CONTENT.get(), FluidType.BUCKET_VOLUME);
		this.containerType = containerType;
	}

	// capsules can't contain fluid that is hotter or equal to the melting point of wax
	private boolean contentsAllowed(FluidResource fluid) {
		if (this.containerType == FluidContainerType.WAX_CAPSULE) {
			return fluid.getFluidType().getTemperature(fluid.toStack(this.capacity)) < 310.15; // melting point of wax in kelvin
		}
		return true;
	}

	@Override
	public boolean isValid(int index, FluidResource resource) {
		return super.isValid(index, resource) && contentsAllowed(resource);
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		// the old handler did nothing for a stack of several containers, and only filled an empty one to the brim
		if (this.itemAccess.getAmount() != 1 || amount < this.capacity || getAmountFrom(this.itemAccess.getResource(), index) != 0) {
			return 0;
		}
		return super.insert(index, resource, this.capacity, transaction);
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
		if (this.itemAccess.getAmount() != 1 || amount < this.capacity || !contentsAllowed(resource)
				|| getAmountFrom(this.itemAccess.getResource(), index) != this.capacity) {
			return 0;
		}
		return super.extract(index, resource, this.capacity, transaction);
	}

	@Nullable
	@Override
	protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
		if (newAmount == 0) {
			// null uses the container up
			return null;
		}
		// damage 1 shows the filled container model
		return super.update(accessResource, index, newResource, newAmount).with(DataComponents.DAMAGE, 1);
	}
}
