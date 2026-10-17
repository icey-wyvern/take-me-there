package com.example.palette;

import com.example.onboarding.PaletteTipState;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
	private int registerCalls;
	private int unregisterCalls;
	private Consumer<Keybind> onOpened = ignored -> {};

	@Test
	public void globalHotkeyOpensOnceAndIgnoresEventsWhileOpenOrFromDialog()
	{
		PaletteHotkeyController controller = controller();
		controller.start();
		assertEquals(configured.get(), palette.localHotkey);
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(1, palette.opens);
		assertTrue(palette.isOpen());
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(1, palette.opens);
		assertEquals(0, palette.closes);

		palette.closeLocally();
		registered.get(0).keyPressed(press(searchField, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(1, palette.opens); // The same dialog-owned press cannot reopen it.
		assertEquals(1, palette.closes);
		assertEquals(0, palette.routes);
	}

	@Test
	public void remapUpdatesRuneLiteAndLocalBindingWithoutRestart()
	{
		PaletteHotkeyController controller = controller();
		controller.start();
		Keybind remapped = new Keybind(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK);
		configured.set(remapped);
		controller.configurationChanged();
		assertEquals(remapped, palette.localHotkey);
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(0, palette.opens);
		registered.get(0).keyPressed(press(game, KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
		assertEquals(1, palette.opens);
	}

	@Test
	public void unsetKeybindDisablesGlobalOpeningAndLocalBinding()
	{
		PaletteHotkeyController controller = controller();
		controller.start();
		configured.set(Keybind.NOT_SET);
		controller.configurationChanged();
		assertEquals(Keybind.NOT_SET, palette.localHotkey);
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(0, palette.opens);
	}

	@Test
	public void startupShutdownAndReenableRegisterAndDisposeExactlyOnce()
	{
		PaletteHotkeyController first = controller();
		first.start();
		first.start();
		assertEquals(1, registerCalls);
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		first.stop();
		first.stop();
		assertEquals(1, unregisterCalls);
		assertEquals(1, palette.disposals);
		assertFalse(palette.isOpen());
		assertTrue(registered.isEmpty());

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

	@Test
	public void firstSuccessfulDefaultOpeningDismissesTipWithoutRouting()
	{
		Map<String, String> configValues = new HashMap<>();
		PaletteTipState tip = new PaletteTipState(() -> configValues.get("paletteTipDismissed"),
			value -> configValues.put("paletteTipDismissed", value));
		onOpened = tip::openedWith;
		PaletteHotkeyController controller = controller();
		controller.start();
		assertTrue(tip.shouldShow(configured.get()));
		palette.failOpen = true;
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertTrue(tip.shouldShow(configured.get()));
		assertFalse(configValues.containsKey("paletteTipDismissed"));
		controller.configurationChanged();
		palette.failOpen = false;
		registered.get(0).keyPressed(press(game, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		assertEquals(2, palette.opens);
		assertEquals("true", configValues.get("paletteTipDismissed"));
		assertFalse(tip.shouldShow(configured.get()));
		assertEquals(0, palette.routes);
	}

	private PaletteHotkeyController controller()
	{
		return new PaletteHotkeyController(configured::get, palette, onOpened,
			listener -> { registerCalls++; registered.add(listener); },
			listener -> { unregisterCalls++; registered.remove(listener); });
	}

	private static KeyEvent press(JPanel source, int code, int modifiers)
	{
		// Passed directly to a listener under test; never posted to an event queue.
		return new KeyEvent(source, KeyEvent.KEY_PRESSED, 1L, modifiers, code, KeyEvent.CHAR_UNDEFINED)
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
		private boolean failOpen;
		private Keybind localHotkey;

		@Override public boolean isOpen() { return open; }
		@Override public void open() { if (!failOpen) open = true; opens++; }
		@Override public void disposePalette() { open = false; disposals++; }
		@Override public boolean ownsKeyEvent(KeyEvent event) { return event.getSource() == searchField; }
		@Override public void configureHotkey(Keybind keybind) { localHotkey = keybind; }
		private void closeLocally() { open = false; closes++; }
	}
}
