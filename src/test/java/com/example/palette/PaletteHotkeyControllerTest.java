package com.example.palette;

import java.awt.KeyEventDispatcher;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;
import org.junit.Test;

import static org.junit.Assert.*;

public class PaletteHotkeyControllerTest
{
	private final JPanel game = new JPanel();
	private final JPanel searchField = new JPanel();
	private final AtomicReference<Keybind> configured = new AtomicReference<>(
		new Keybind(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
	private final FakePalette palette = new FakePalette();
	private final List<KeyListener> registered = new ArrayList<>();
	private final List<KeyEventDispatcher> dispatchers = new ArrayList<>();
	private int registerCalls;
	private int unregisterCalls;
	private int dispatcherAdds;
	private int dispatcherRemoves;

	@Test
	public void globalHotkeyOpensOnceAndPaletteFocusedHotkeyClosesWithoutRouting()
	{
		PaletteHotkeyController controller = controller();
		controller.start();
		KeyEvent open = press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
		registered.get(0).keyPressed(open);
		assertEquals(1, palette.opens);
		assertTrue(palette.isOpen());
		KeyEvent openingRepeat = press(searchField, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
		assertTrue(dispatchers.get(0).dispatchKeyEvent(openingRepeat));
		assertEquals(0, palette.closes);
		// The release of the opening chord can be delivered to the dialog.
		assertFalse(dispatchers.get(0).dispatchKeyEvent(release(searchField, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK)));

		KeyEvent close = press(searchField, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
		assertTrue(dispatchers.get(0).dispatchKeyEvent(close));
		assertTrue(close.isConsumed());
		registered.get(0).keyPressed(close); // same event reaching RuneLite cannot reopen
		assertEquals(1, palette.opens);
		assertEquals(1, palette.closes);
		assertEquals(0, palette.routes);
		assertFalse(palette.isOpen());

		KeyEvent repeat = press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
		assertTrue(dispatchers.get(0).dispatchKeyEvent(repeat));
		registered.get(0).keyPressed(repeat);
		assertEquals(1, palette.opens);
		assertTrue(dispatchers.get(0).dispatchKeyEvent(release(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK)));
		KeyEvent nextPress = press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
		assertFalse(dispatchers.get(0).dispatchKeyEvent(nextPress));
		registered.get(0).keyPressed(nextPress);
		assertEquals(2, palette.opens);
	}

	@Test
	public void configuredKeybindChangesWithoutRestartAndUnsetDisablesOpening()
	{
		PaletteHotkeyController controller = controller();
		controller.start();
		configured.set(new Keybind(KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK));
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(0, palette.opens);
		registered.get(0).keyPressed(press(game, KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK));
		assertEquals(1, palette.opens);
		dispatchers.get(0).dispatchKeyEvent(release(game, KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK));
		registered.get(0).keyReleased(release(game, KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK));
		configured.set(Keybind.NOT_SET);
		KeyEvent unset = press(searchField, KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK);
		assertFalse(dispatchers.get(0).dispatchKeyEvent(unset));
		assertTrue(palette.isOpen());
		registered.get(0).keyPressed(press(game, KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK));
		assertEquals(0, palette.closes);
	}

	@Test
	public void remappingWhileOldChordIsHeldReleasesListenerState()
	{
		PaletteHotkeyController controller = controller();
		controller.start();
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(1, palette.opens);
		configured.set(new Keybind(KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK));
		controller.configurationChanged();
		registered.get(0).keyPressed(press(game, KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK));
		assertEquals(1, palette.closes);
		assertFalse(palette.isOpen());
	}

	@Test
	public void paletteFocusedCloseUsesNewConfiguredChord()
	{
		PaletteHotkeyController controller = controller();
		controller.start();
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		dispatchers.get(0).dispatchKeyEvent(release(searchField, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		configured.set(new Keybind(KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK));
		controller.configurationChanged();
		assertFalse(dispatchers.get(0).dispatchKeyEvent(press(searchField, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK)));
		assertTrue(palette.isOpen());
		assertTrue(dispatchers.get(0).dispatchKeyEvent(press(searchField, KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK)));
		assertEquals(1, palette.closes);
		assertEquals(0, palette.routes);
	}

	@Test
	public void startupShutdownAndReenableRegisterAndDisposeExactlyOnce()
	{
		PaletteHotkeyController first = controller();
		first.start();
		first.start();
		assertEquals(1, registerCalls);
		assertEquals(1, dispatcherAdds);
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		first.stop();
		first.stop();
		assertEquals(1, unregisterCalls);
		assertEquals(1, dispatcherRemoves);
		assertEquals(1, palette.disposals);
		assertFalse(palette.isOpen());
		assertTrue(registered.isEmpty());
		assertTrue(dispatchers.isEmpty());

		PaletteHotkeyController second = controller();
		second.start();
		assertEquals(2, registerCalls);
		assertEquals(1, registered.size());
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(2, palette.opens);
		second.stop();
		assertEquals(2, unregisterCalls);
		assertEquals(2, palette.disposals);
	}

	private PaletteHotkeyController controller()
	{
		return new PaletteHotkeyController(configured::get, palette,
			listener -> { registerCalls++; registered.add(listener); },
			listener -> { unregisterCalls++; registered.remove(listener); },
			dispatcher -> { dispatcherAdds++; dispatchers.add(dispatcher); },
			dispatcher -> { dispatcherRemoves++; dispatchers.remove(dispatcher); });
	}

	private static KeyEvent press(JPanel source, int code, int modifiers)
	{
		return event(source, KeyEvent.KEY_PRESSED, code, modifiers);
	}

	private static KeyEvent release(JPanel source, int code, int modifiers)
	{
		return event(source, KeyEvent.KEY_RELEASED, code, modifiers);
	}

	private static KeyEvent event(JPanel source, int type, int code, int modifiers)
	{
		// Synthetic AWT events have extendedKeyCode=0 unless supplied by the platform.
		return new KeyEvent(source, type, 1L, modifiers, code, KeyEvent.CHAR_UNDEFINED)
		{
			@Override public int getExtendedKeyCode() { return code; }
		};
	}

	private final class FakePalette implements PaletteHotkeyController.PaletteWindow
	{
		private boolean open;
		private int opens;
		private int closes;
		private int disposals;
		private int routes;

		@Override public boolean isOpen() { return open; }
		@Override public void open() { open = true; opens++; }
		@Override public void closeWithoutSelection() { open = false; closes++; }
		@Override public void disposePalette() { open = false; disposals++; }
		@Override public boolean ownsKeyEvent(KeyEvent event) { return event.getSource() == searchField; }
	}
}
