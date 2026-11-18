package forestry.gametest.client;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import forestry.core.content.machines.blocks.FactoryBlockType;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.content.worktable.features.WorktableBlocks;
import forestry.core.platform.recipes.jei.ForestryRecipeType;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.RecipeTransferResult;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;

// dev check without input: -Dforestry.jeiSnapshots=<world> opens that world, shows every Forestry JEI category,
// then transfers a recipe into a worktable, a carpenter and a fabricator and saves screenshots/jei/
public final class JeiSnapshots {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int STEP_TICKS = 10;

	private static String world = "";
	private static boolean worldRequested;
	private static boolean planned;
	private static int wait = 200;
	private static final Deque<Runnable> steps = new ArrayDeque<>();

	private JeiSnapshots() {
	}

	public static void register(String worldName) {
		world = worldName;
		NeoForge.EVENT_BUS.addListener(JeiSnapshots::tick);
	}

	private static void tick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			if (!worldRequested && minecraft.gui.screen() instanceof TitleScreen) {
				worldRequested = true;
				minecraft.createWorldOpenFlows().openWorld(world, () -> LOGGER.error("jei snapshots: cannot open world {}", world));
			}
			return;
		}
		if (JeiSnapshotPlugin.runtime == null || --wait > 0) {
			return;
		}
		wait = STEP_TICKS;
		if (!planned) {
			planned = true;
			plan(JeiSnapshotPlugin.runtime);
		}
		if (!steps.isEmpty()) {
			try {
				steps.poll().run();
			} catch (RuntimeException e) {
				LOGGER.error("jei snapshots: step failed", e);
			}
			return;
		}
		LOGGER.info("jei snapshots: done");
		minecraft.stop();
	}

	private static void plan(IJeiRuntime runtime) {
		List<IRecipeCategory<?>> categories = runtime.getRecipeManager().createRecipeCategoryLookup().get()
				.filter(category -> category.getRecipeType().getUid().getNamespace().equals("forestry"))
				.sorted(Comparator.comparing(category -> category.getRecipeType().getUid().toString()))
				.toList();
		LOGGER.info("jei snapshots: {} forestry categories", categories.size());
		for (IRecipeCategory<?> category : categories) {
			IRecipeType<?> type = category.getRecipeType();
			long recipes = runtime.getRecipeManager().createRecipeLookup(type).get().count();
			List<String> stations = runtime.getRecipeManager().createCraftingStationLookup(type).getItemStack()
					.map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).toList();
			LOGGER.info("jei snapshots: category {} recipes {} stations {}", type.getUid(), recipes, stations);
			steps.add(() -> runtime.getRecipesGui().showTypes(List.of(type)));
			steps.add(() -> grab("category_" + type.getUid().getPath().replace('/', '-') + ".png"));
		}
		steps.add(() -> Minecraft.getInstance().gui.setScreen(null));
		transfer(runtime, "worktable", WorktableBlocks.WORKTABLE.defaultState(), 2, RecipeTypes.CRAFTING);
		transfer(runtime, "carpenter", FactoryBlocks.PLAIN.get(FactoryBlockType.CARPENTER).defaultState(), -2, ForestryRecipeType.CARPENTER);
		transfer(runtime, "fabricator", FactoryBlocks.PLAIN.get(FactoryBlockType.FABRICATOR).defaultState(), 3, ForestryRecipeType.FABRICATOR);
	}

	private static <R> void transfer(IJeiRuntime runtime, String name, BlockState state, int offset, IRecipeType<R> type) {
		BlockPos[] pos = new BlockPos[1];
		steps.add(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.gui.setScreen(null);
			pos[0] = minecraft.player.blockPosition().offset(offset, 1, 0);
			BlockPos target = pos[0];
			minecraft.getSingleplayerServer().execute(() -> minecraft.getSingleplayerServer().overworld().setBlock(target, state, 3));
		});
		steps.add(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos[0]), Direction.UP, pos[0], false);
			minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
		});
		steps.add(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
				LOGGER.error("jei snapshots: {} did not open, screen is {}", name, minecraft.gui.screen());
				return;
			}
			doTransfer(runtime, name, screen, type);
		});
		steps.add(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen) {
				logSlots(name, screen.getMenu());
			}
			grab("transfer_" + name + ".png");
		});
	}

	@SuppressWarnings("unchecked")
	private static <C extends AbstractContainerMenu, R> void doTransfer(IJeiRuntime runtime, String name, AbstractContainerScreen<?> rawScreen, IRecipeType<R> type) {
		AbstractContainerScreen<C> screen = (AbstractContainerScreen<C>) rawScreen;
		C menu = screen.getMenu();
		IRecipeCategory<R> category = runtime.getRecipeManager().createRecipeCategoryLookup().limitTypes(List.of(type)).get()
				.map(c -> (IRecipeCategory<R>) c).findFirst().orElseThrow();
		Optional<IRecipeTransferHandler<C, R>> handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, category);
		if (handler.isEmpty()) {
			LOGGER.error("jei snapshots: no transfer handler for {} in {}", type.getUid(), menu.getClass().getName());
			return;
		}
		LOGGER.info("jei snapshots: {} handler {}", name, handler.get().getClass().getName());
		R recipe = runtime.getRecipeManager().createRecipeLookup(type).get().findFirst().orElseThrow();
		IRecipeLayoutDrawable<R> layout = runtime.getRecipeManager()
				.createRecipeLayoutDrawable(category, recipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
		Player player = Minecraft.getInstance().player;
		IRecipeSlotsView slots = layout.getRecipeSlotsView();
		IRecipeTransferError check = handler.get().transferRecipe(context(screen, menu, type, recipe, slots, player), false);
		IRecipeTransferError done = handler.get().transferRecipe(context(screen, menu, type, recipe, slots, player), true);
		LOGGER.info("jei snapshots: {} transfer of {} check {} done {}", name, layout.getSourceRecipeIds(),
				check == null ? "ok" : check.getType(), done == null ? "ok" : done.getType());
	}

	private static <C extends AbstractContainerMenu, R> IRecipeTransferContext<R, C> context(AbstractContainerScreen<C> screen, C menu, IRecipeType<R> type, R recipe, IRecipeSlotsView slots, Player player) {
		return new IRecipeTransferContext<>() {
			@Override
			public int getTransferId() {
				return 0;
			}

			@Override
			public void completeRecipeTransfer(RecipeTransferResult result) {
			}

			@Override
			public R getRecipe() {
				return recipe;
			}

			@Override
			public IRecipeType<R> getRecipeType() {
				return type;
			}

			@Override
			public C getContainer() {
				return menu;
			}

			@Override
			public AbstractContainerScreen<C> getScreen() {
				return screen;
			}

			@Override
			public IRecipeSlotsView getRecipeSlots() {
				return slots;
			}

			@Override
			public Player getPlayer() {
				return player;
			}

			@Override
			public boolean isMaxTransfer() {
				return false;
			}
		};
	}

	private static void logSlots(String name, AbstractContainerMenu menu) {
		StringBuilder builder = new StringBuilder();
		for (Slot slot : menu.slots) {
			if (slot.container instanceof Inventory || !slot.hasItem()) {
				continue;
			}
			builder.append(' ').append(slot.index).append('=').append(BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()));
		}
		LOGGER.info("jei snapshots: {} slots{}", name, builder);
	}

	private static void grab(String fileName) {
		Minecraft minecraft = Minecraft.getInstance();
		new File(minecraft.gameDirectory, "screenshots/jei").mkdirs();
		Screenshot.grab(minecraft.gameDirectory, "jei/" + fileName, minecraft.gameRenderer.mainRenderTarget(), 1, message -> {});
	}
}
