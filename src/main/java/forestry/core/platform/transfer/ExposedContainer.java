package forestry.core.platform.transfer;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

// the container wrapper of NeoForge, with the way back to the Forestry wrapper of the same container
final class ExposedContainer extends DelegatingResourceHandler<ItemResource> {
	final IItemHandler source;

	ExposedContainer(ResourceHandler<ItemResource> wrapper, IItemHandler source) {
		super(wrapper);
		this.source = source;
	}
}
