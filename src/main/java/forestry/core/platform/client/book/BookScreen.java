package forestry.core.platform.client.book;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import forestry.core.platform.book.Book;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

// the open book: two pages side by side, the page turn arrows and the way back
final class BookScreen extends Screen {
	private static final int TEXTURE_WIDTH = 512;
	private static final int TEXTURE_HEIGHT = 256;
	private static final int BOOK_WIDTH = 272;
	private static final int BOOK_HEIGHT = 180;
	private static final int LEFT_PAGE_X = 15;
	private static final int RIGHT_PAGE_X = 141;
	private static final int PAGE_Y = 18;
	// deviation: under the book, where no arrow can cover a page
	private static final int ARROW_Y = BOOK_HEIGHT + 3;
	private static final int ARROW_WIDTH = 18;
	private static final int ARROW_HEIGHT = 10;
	private static final int BACK_HEIGHT = 9;

	// u and v of the arrow in the book texture, its lit copy is one arrow width to the right
	private enum Arrow {
		PREVIOUS(14, 272, 10, ARROW_HEIGHT),
		NEXT(BOOK_WIDTH - 14 - ARROW_WIDTH, 272, 0, ARROW_HEIGHT),
		BACK((BOOK_WIDTH - ARROW_WIDTH) / 2, 308, 0, BACK_HEIGHT);

		private final int x;
		private final int u;
		private final int v;
		private final int height;

		Arrow(int x, int u, int v, int height) {
			this.x = x;
			this.u = u;
			this.v = v;
			this.height = height;
		}
	}

	// spread is the index of the pair of pages
	private record Location(Book.Link target, int spread) {
	}

	// where the reader was, kept for the session
	private static Location last = new Location(Book.Link.LANDING, 0);

	private final Book book;
	private final Deque<Location> history = new ArrayDeque<>();
	private Location location = last;
	private List<BookPage> pages = List.of();

	BookScreen(Book book) {
		super(Component.translatable(book.name()));
		this.book = book;
		open(last);
	}

	private int spreads() {
		return Math.max(1, (this.pages.size() + 1) / 2);
	}

	private void open(Location location) {
		this.pages = BookPages.build(this.book, this.font, location.target());
		this.location = location;
		turnTo(location.spread());
	}

	private void turnTo(int spread) {
		this.location = new Location(this.location.target(), Math.clamp(spread, 0, spreads() - 1));
		last = this.location;
	}

	private void follow(Book.Link link) {
		this.history.push(this.location);
		// the location keeps the spread, not the anchor
		open(new Location(new Book.Link(link.category(), link.id(), ""), 0));
		for (int i = 0; i < this.pages.size(); i++) {
			if (!link.anchor().isEmpty() && this.pages.get(i).anchor.equals(link.anchor())) {
				turnTo(i / 2);
				break;
			}
		}
		playTurn();
	}

	private boolean visible(Arrow arrow) {
		return switch (arrow) {
			case PREVIOUS -> this.location.spread() > 0;
			case NEXT -> this.location.spread() < spreads() - 1;
			case BACK -> !this.history.isEmpty() || this.location.target().id() != null;
		};
	}

	private void press(Arrow arrow) {
		if (!visible(arrow)) {
			return;
		}
		switch (arrow) {
			case PREVIOUS -> turnTo(this.location.spread() - 1);
			case NEXT -> turnTo(this.location.spread() + 1);
			// a book opened again has no history, last brought the reader here
			case BACK -> open(this.history.isEmpty() ? new Location(this.book.parent(this.location.target()), 0) : this.history.pop());
		}
		playTurn();
	}

	private void playTurn() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
	}

	private int bookX() {
		return (this.width - BOOK_WIDTH) / 2;
	}

	private int bookY() {
		return (this.height - BOOK_HEIGHT) / 2;
	}

	@Nullable
	private Arrow arrowAt(double mouseX, double mouseY) {
		for (Arrow arrow : Arrow.values()) {
			double x = mouseX - bookX() - arrow.x;
			double y = mouseY - bookY() - ARROW_Y;
			if (visible(arrow) && x >= 0 && x < ARROW_WIDTH && y >= 0 && y < arrow.height) {
				return arrow;
			}
		}
		return null;
	}

	@Nullable
	private Book.Link linkAt(double mouseX, double mouseY) {
		int y = (int) Math.floor(mouseY) - bookY() - PAGE_Y;
		for (int side = 0; side < 2; side++) {
			int index = this.location.spread() * 2 + side;
			if (index < this.pages.size()) {
				Book.Link link = this.pages.get(index).linkAt((int) Math.floor(mouseX) - bookX() - (side == 0 ? LEFT_PAGE_X : RIGHT_PAGE_X), y);
				if (link != null) {
					return link;
				}
			}
		}
		return null;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.book.texture(), bookX(), bookY(), 0, 0, BOOK_WIDTH, BOOK_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int bookX = bookX();
		int bookY = bookY();

		for (int side = 0; side < 2; side++) {
			int index = this.location.spread() * 2 + side;
			if (index < this.pages.size()) {
				this.pages.get(index).extract(graphics, this.font, bookX + (side == 0 ? LEFT_PAGE_X : RIGHT_PAGE_X), bookY + PAGE_Y, mouseX, mouseY);
			}
		}

		Arrow hovered = arrowAt(mouseX, mouseY);
		for (Arrow arrow : Arrow.values()) {
			if (visible(arrow)) {
				int u = arrow.u + (arrow == hovered ? ARROW_WIDTH : 0);
				graphics.blit(RenderPipelines.GUI_TEXTURED, this.book.texture(), bookX + arrow.x, bookY + ARROW_Y, u, arrow.v, ARROW_WIDTH, arrow.height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
			}
		}
		if (spreads() > 1) {
			graphics.centeredText(this.font, (this.location.spread() + 1) + "/" + spreads(), bookX + BOOK_WIDTH / 2, bookY + ARROW_Y + ARROW_HEIGHT + 3, 0xFFFFFFFF);
		}
		if (hovered != null || linkAt(mouseX, mouseY) != null) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			press(Arrow.BACK);
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			Arrow arrow = arrowAt(event.x(), event.y());
			Book.Link link = linkAt(event.x(), event.y());
			if (arrow != null) {
				press(arrow);
				return true;
			} else if (link != null) {
				follow(link);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY != 0.0) {
			press(scrollY < 0.0 ? Arrow.NEXT : Arrow.PREVIOUS);
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isLeft()) {
			press(Arrow.PREVIOUS);
		} else if (event.isRight()) {
			press(Arrow.NEXT);
		} else if (event.key() == InputConstants.KEY_BACKSPACE) {
			press(Arrow.BACK);
		} else if (this.minecraft.options.keyInventory.matches(event)) {
			onClose();
		} else {
			return super.keyPressed(event);
		}
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// the world stays visible behind the book, as behind a vanilla book
	@Override
	public boolean isInGameUi() {
		return true;
	}
}
