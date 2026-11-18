package forestry.gametest;

import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import forestry.api.ForestryConstants;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.ForestryTags;
import forestry.api.IForestryApi;
import forestry.api.core.backpacks.EnumBackpackType;
import forestry.api.core.backpacks.IBackpackDefinition;
import forestry.api.core.backpacks.IBackpackInterface;
import forestry.core.content.backpacks.items.BackpackItem;
import forestry.core.content.backpacks.items.NaturalistBackpackItem;

// registers two backpacks the way an addon would, through the API and its own DeferredRegister
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class AddonBackpackTest {
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForestryGameTests.MOD_ID);
	private static final DeferredItem<Item> BACKPACK = ITEMS.register("addon_backpack",
			id -> api().createBackpack(id, new Definition(api().createBackpackFilter(ForestryTags.Items.MINER_ALLOW, ForestryTags.Items.MINER_REJECT)), EnumBackpackType.WOVEN));
	private static final DeferredItem<Item> NATURALIST = ITEMS.register("addon_naturalist_backpack",
			id -> api().createNaturalistBackpack(id, new Definition(api().createNaturalistBackpackFilter(ForestrySpeciesTypes.BEE)), ForestrySpeciesTypes.BEE));

	static void register(IEventBus modBus) {
		ITEMS.register(modBus);
	}

	private static IBackpackInterface api() {
		return IForestryApi.INSTANCE.getBackpackInterface();
	}

	@GameTest(template = "empty")
	public static void addonBackpacksRegisterWithTheirOwnIds(GameTestHelper helper) {
		check(helper, BACKPACK, BackpackItem.class);
		check(helper, NATURALIST, NaturalistBackpackItem.class);
		helper.succeed();
	}

	private static void check(GameTestHelper helper, DeferredItem<Item> holder, Class<?> expected) {
		Item item = holder.get();
		Identifier id = holder.getId();
		helper.assertTrue(expected.isInstance(item), id + " is " + item.getClass().getName());
		helper.assertTrue(id.equals(BuiltInRegistries.ITEM.getKey(item)), id + " registered as " + BuiltInRegistries.ITEM.getKey(item));
		String descriptionId = "item." + id.getNamespace() + "." + id.getPath();
		helper.assertTrue(descriptionId.equals(item.getDescriptionId()), id + " has description " + item.getDescriptionId());
	}

	private record Definition(Predicate<ItemStack> filter) implements IBackpackDefinition {
		@Override
		public Component getName(ItemStack backpack) {
			return backpack.getItem().getName(backpack);
		}

		@Override
		public int getPrimaryColour() {
			return 0x808080;
		}

		@Override
		public int getSecondaryColour() {
			return 0xffffff;
		}

		@Override
		public Predicate<ItemStack> getFilter() {
			return this.filter;
		}
	}
}
