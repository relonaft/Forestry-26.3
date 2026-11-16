package forestry.core.platform.util;

import net.minecraft.network.chat.Component;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// since 1.21.5 an item appends its tooltip to a consumer. Forestry tooltip code adds to a list, so this
// list hands every appended line straight to the consumer, which keeps the order around super calls
public final class TooltipList extends AbstractList<Component> {
	private final Consumer<Component> adder;
	private final List<Component> added = new ArrayList<>();

	public TooltipList(Consumer<Component> adder) {
		this.adder = adder;
	}

	@Override
	public void add(int index, Component line) {
		if (index != this.added.size()) {
			throw new UnsupportedOperationException("a tooltip line can only be appended");
		}
		this.added.add(line);
		this.adder.accept(line);
	}

	@Override
	public Component get(int index) {
		return this.added.get(index);
	}

	@Override
	public int size() {
		return this.added.size();
	}
}
