package com.example.palette;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;
import net.runelite.client.util.HotkeyListener;

/** Coordinates RuneLite's canvas listener with a palette-focused Swing close path. */
public final class PaletteHotkeyController
{
	public interface PaletteWindow
	{
		boolean isOpen();
		void open();
		void closeWithoutSelection();
		void disposePalette();
		boolean ownsKeyEvent(KeyEvent event);
	}

	private final Supplier<Keybind> configuredKeybind;
	private final PaletteWindow palette;
	private final Consumer<KeyListener> register;
	private final Consumer<KeyListener> unregister;
	private final Consumer<KeyEventDispatcher> addDispatcher;
	private final Consumer<KeyEventDispatcher> removeDispatcher;
	private final KeyEventDispatcher dispatcher = this::dispatchLocalKey;
	private final HotkeyListener listener;
	private boolean started;
	private boolean localCloseHeld;
	private int heldKeyCode;
	private boolean globalPressHeld;
	private int globalKeyCode;
	private int matchingGlobalEventCode;

	public PaletteHotkeyController(Supplier<Keybind> configuredKeybind, PaletteWindow palette,
		Consumer<KeyListener> register, Consumer<KeyListener> unregister,
		Consumer<KeyEventDispatcher> addDispatcher, Consumer<KeyEventDispatcher> removeDispatcher)
	{
		this.configuredKeybind = Objects.requireNonNull(configuredKeybind, "configuredKeybind");
		this.palette = Objects.requireNonNull(palette, "palette");
		this.register = Objects.requireNonNull(register, "register");
		this.unregister = Objects.requireNonNull(unregister, "unregister");
		this.addDispatcher = Objects.requireNonNull(addDispatcher, "addDispatcher");
		this.removeDispatcher = Objects.requireNonNull(removeDispatcher, "removeDispatcher");
		this.listener = new HotkeyListener(this::keybind)
		{
			@Override
			public void keyPressed(KeyEvent event)
			{
				if (localCloseHeld || palette.ownsKeyEvent(event))
				{
					return;
				}
				if (keybind().matches(event))
				{
					matchingGlobalEventCode = event.getKeyCode();
				}
				super.keyPressed(event);
			}

			@Override
			public void hotkeyPressed()
			{
				if (started && !localCloseHeld && !globalPressHeld)
				{
					globalPressHeld = true;
					globalKeyCode = matchingGlobalEventCode;
					if (palette.isOpen())
					{
						palette.closeWithoutSelection();
					}
					else
					{
						palette.open();
					}
					// The release can land in the newly focused dialog, away from KeyManager.
					SwingUtilities.invokeLater(() ->
					{
						if (started)
						{
							listener.focusLost();
						}
					});
				}
			}
		};
	}

	public void start()
	{
		if (started)
		{
			return;
		}
		started = true;
		addDispatcher.accept(dispatcher);
		register.accept(listener);
	}

	public void stop()
	{
		if (!started)
		{
			return;
		}
		started = false;
		unregister.accept(listener);
		removeDispatcher.accept(dispatcher);
		listener.focusLost();
		localCloseHeld = false;
		globalPressHeld = false;
		palette.disposePalette();
	}

	/** Release pressed-key state when the configured chord changes. */
	public void configurationChanged()
	{
		listener.focusLost();
		localCloseHeld = false;
		globalPressHeld = false;
	}

	private Keybind keybind()
	{
		Keybind configured = configuredKeybind.get();
		return configured == null ? Keybind.NOT_SET : configured;
	}

	private boolean dispatchLocalKey(KeyEvent event)
	{
		if (!started)
		{
			return false;
		}
		if (globalPressHeld && event.getKeyCode() == globalKeyCode)
		{
			if (event.getID() == KeyEvent.KEY_RELEASED)
			{
				globalPressHeld = false;
			}
			else if (event.getID() == KeyEvent.KEY_PRESSED)
			{
				event.consume();
				return true;
			}
		}
		if (localCloseHeld && event.getKeyCode() == heldKeyCode)
		{
			if (event.getID() == KeyEvent.KEY_RELEASED)
			{
				localCloseHeld = false;
			}
			if (event.getID() == KeyEvent.KEY_PRESSED || event.getID() == KeyEvent.KEY_RELEASED)
			{
				event.consume();
				return true;
			}
		}
		if (event.getID() != KeyEvent.KEY_PRESSED || !palette.isOpen()
			|| !palette.ownsKeyEvent(event) || !keybind().matches(event))
		{
			return false;
		}
		localCloseHeld = true;
		heldKeyCode = event.getKeyCode();
		event.consume();
		listener.focusLost(); // RuneLite may not receive the opening chord's release in the dialog.
		palette.closeWithoutSelection();
		return true;
	}
}
