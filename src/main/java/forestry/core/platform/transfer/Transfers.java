package forestry.core.platform.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

// the border between the handlers Forestry works with and the transfer API of NeoForge
public final class Transfers {
	private Transfers() {
	}

	// the handler must be ITransactional, the tank managers are
	@Nullable
	public static ResourceHandler<FluidResource> exposeFluid(@Nullable IFluidHandler handler) {
		if (handler == null) {
			return null;
		}
		if (handler instanceof ResourceFluidHandler view) {
			return view.handler;
		}
		return new ExposedFluidHandler(handler);
	}

	// container wrappers go out as the container wrappers of NeoForge, any other handler must be ITransactional
	@Nullable
	public static ResourceHandler<ItemResource> exposeItems(@Nullable IItemHandler handler) {
		if (handler == null) {
			return null;
		}
		if (handler instanceof ResourceItemHandler view) {
			return view.handler;
		}
		// exact class, a subclass can change what the wrapper does
		if (handler.getClass() == SidedInvWrapper.class) {
			SidedInvWrapper sided = (SidedInvWrapper) handler;
			return new ExposedContainer(new WorldlyContainerWrapper(sided.inv, sided.side), handler);
		}
		if (handler.getClass() == InvWrapper.class) {
			return new ExposedContainer(VanillaContainerWrapper.of(((InvWrapper) handler).getInv()), handler);
		}
		return new ExposedItemHandler(handler);
	}

	// for a handler whose insert cannot be taken back: it runs when the transaction is committed
	// rejected gets what the handler then refuses against its own simulation
	public static ResourceHandler<ItemResource> exposeItemsDeferred(IItemHandler handler, Consumer<ItemStack> rejected) {
		return new DeferredInsertHandler(handler, rejected);
	}

	// the storage must be ITransactional, EnergyStorage is
	@Nullable
	public static EnergyHandler exposeEnergy(@Nullable IEnergyStorage storage) {
		if (storage == null) {
			return null;
		}
		if (storage instanceof EnergyHandlerStorage view) {
			return view.handler;
		}
		return new ExposedEnergyStorage(storage);
	}

	// a handler that Forestry exposed itself comes back as the Forestry handler, without any transaction
	@Nullable
	public static IFluidHandler viewFluid(@Nullable ResourceHandler<FluidResource> handler) {
		if (handler == null) {
			return null;
		}
		if (handler instanceof ExposedFluidHandler exposed) {
			return exposed.handler;
		}
		return new ResourceFluidHandler(handler);
	}

	@Nullable
	public static IItemHandler viewItems(@Nullable ResourceHandler<ItemResource> handler) {
		if (handler == null) {
			return null;
		}
		if (handler instanceof ExposedContainer exposed) {
			return exposed.source;
		}
		if (handler instanceof ExposedItemHandler exposed) {
			return exposed.handler;
		}
		return new ResourceItemHandler(handler);
	}

	@Nullable
	public static IEnergyStorage viewEnergy(@Nullable EnergyHandler handler) {
		if (handler == null) {
			return null;
		}
		if (handler instanceof ExposedEnergyStorage exposed) {
			return exposed.storage;
		}
		return new EnergyHandlerStorage(handler);
	}

	// fluid handler of a stack that does not come from the item capability
	public static IFluidHandlerItem viewFluid(ItemStack stack, Function<ItemAccess, ResourceHandler<FluidResource>> factory) {
		ItemStackAccess access = new ItemStackAccess(stack);
		return new ItemStackFluidHandler(access, factory.apply(access));
	}

	// Forestry code can run inside the transaction of a mod that called one of the exposed handlers
	// a root transaction would throw there
	@SuppressWarnings("deprecation")
	static Transaction openTransaction() {
		return Transaction.open(Transaction.getCurrentOpenedTransaction());
	}
}
