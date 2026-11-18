package forestry.gametest.client;

import com.mojang.logging.LogUtils;
import forestry.core.platform.book.Book;
import forestry.core.platform.book.BookCategory;
import forestry.core.platform.book.BookEntry;
import forestry.core.platform.client.book.BookClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// dev check without input: -Dforestry.bookSnapshots=<world> opens that world, pages through the whole
// Forester's Manual in en_us and ru_ru and saves every spread to screenshots/, then closes the client
public final class BookSnapshots {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String[] LANGUAGES = {"en_us", "ru_ru"};
	// ticks between steps, enough for a frame with the new spread to be drawn
	private static final int STEP_TICKS = 4;

	private static String world = "";
	private static boolean worldRequested;
	private static int wait = 100;
	private static int language = -1;
	private static CompletableFuture<Void> reload;
	private static final Deque<Runnable> steps = new ArrayDeque<>();

	private BookSnapshots() {
	}

	public static void register(String worldName) {
		world = worldName;
		NeoForge.EVENT_BUS.addListener(BookSnapshots::tick);
	}

	private static void tick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			if (!worldRequested && minecraft.gui.screen() instanceof TitleScreen) {
				worldRequested = true;
				minecraft.createWorldOpenFlows().openWorld(world, () -> LOGGER.error("book snapshots: cannot open world {}", world));
			}
			return;
		}
		if (reload != null && !reload.isDone()) {
			return;
		}
		if (--wait > 0) {
			return;
		}
		wait = STEP_TICKS;
		if (!steps.isEmpty()) {
			steps.poll().run();
			return;
		}
		language++;
		if (language >= LANGUAGES.length) {
			LOGGER.info("book snapshots: done");
			minecraft.getLanguageManager().setSelected(LANGUAGES[0]);
			minecraft.options.languageCode = LANGUAGES[0];
			minecraft.stop();
			return;
		}
		String code = LANGUAGES[language];
		if (!code.equals(minecraft.getLanguageManager().getSelected())) {
			minecraft.getLanguageManager().setSelected(code);
			minecraft.options.languageCode = code;
			reload = minecraft.reloadResourcePacks();
			wait = 40;
		}
		steps.add(() -> plan(code));
	}

	// after a language switch the book is read again, so the targets are listed only now
	private static void plan(String code) {
		Book book = book();
		List<Target> targets = new ArrayList<>();
		targets.add(new Target("landing", Book.Link.LANDING));
		for (BookCategory category : book.categories().values().stream().sorted(Comparator.comparing(c -> c.id().toString())).toList()) {
			targets.add(new Target("c_" + name(category.id()), new Book.Link(true, category.id(), "")));
		}
		for (BookEntry entry : book.entries().values().stream().sorted(Comparator.comparing(e -> e.id().toString())).toList()) {
			targets.add(new Target("e_" + name(entry.id()), new Book.Link(false, entry.id(), "")));
		}
		LOGGER.info("book snapshots: {} targets in {}", targets.size(), code);
		for (Target target : targets) {
			steps.add(() -> open(code, target));
		}
		// reopening lands on the last spread seen; BACK must then climb to the landing page
		steps.add(() -> {
			BookClient.open();
			grab(code + "_z_reopen_0.png");
		});
		for (int i = 1; i <= 4; i++) {
			int step = i;
			steps.add(() -> Minecraft.getInstance().gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_BACKSPACE, 0, 0)));
			steps.add(() -> grab(code + "_z_reopen_back_" + step + ".png"));
		}
	}

	private static void open(String code, Target target) {
		BookClient.open();
		Screen screen = Minecraft.getInstance().gui.screen();
		invoke(screen, "follow", new Class<?>[]{Book.Link.class}, target.link());
		int spreads = (int) invoke(screen, "spreads", new Class<?>[0]);
		List<Runnable> shots = new ArrayList<>();
		for (int spread = 0; spread < spreads; spread++) {
			int index = spread;
			shots.add(() -> invoke(screen, "turnTo", new Class<?>[]{int.class}, index));
			shots.add(() -> grab(code + "_" + target.name() + "_" + index + ".png"));
		}
		// the new spread runs before the remaining targets
		for (int i = shots.size() - 1; i >= 0; i--) {
			steps.addFirst(shots.get(i));
		}
	}

	private static void grab(String fileName) {
		Minecraft minecraft = Minecraft.getInstance();
		// Screenshot makes only screenshots/ itself
		new File(minecraft.gameDirectory, "screenshots/book").mkdirs();
		Screenshot.grab(minecraft.gameDirectory, "book/" + fileName, minecraft.gameRenderer.mainRenderTarget(), 1, message -> {});
	}

	private static String name(Identifier id) {
		return id.getPath().replace('/', '-');
	}

	private static Book book() {
		try {
			Method method = BookClient.class.getDeclaredMethod("book");
			method.setAccessible(true);
			return (Book) method.invoke(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
		try {
			Method method = target.getClass().getDeclaredMethod(name, types);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private record Target(String name, Book.Link link) {
	}
}
