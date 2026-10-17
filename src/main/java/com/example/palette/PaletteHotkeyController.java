package com.example.palette;

import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;
import net.runelite.client.util.HotkeyListener;

/** Opens the palette through RuneLite's keyboard listener. Dialog keys stay local to Swing. */
public final class PaletteHotkeyController
{
	public interface PaletteWindow
	{
		boolean isOpen();
		void open();
		void disposePalette();
		boolean ownsKeyEvent(KeyEvent event);
		void configureHotkey(Keybind keybind);
	}

	private final Supplier<Keybind> configuredKeybind;
	private final PaletteWindow palette;
	private final Consumer<Keybind> onOpened;
	private final Consumer<KeyListener> register;
	private final Consumer<KeyListener> unregister;
	private final HotkeyListener listener;
	private boolean started;

	public PaletteHotkeyController(Supplier<Keybind> configuredKeybind, PaletteWindow palette,
		Consumer<Keybind> onOpened, Consumer<KeyListener> register, Consumer<KeyListener> unregister)
	{
		this.configuredKeybind = Objects.requireNonNull(configuredKeybind, "configuredKeybind");
		this.palette = Objects.requireNonNull(palette, "palette");
		this.onOpened = Objects.requireNonNull(onOpened, "onOpened");
		this.register = Objects.requireNonNull(register, "register");
		this.unregister = Objects.requireNonNull(unregister, "unregister");
		this.listener = new HotkeyListener(this::keybind)
		{
			@Override
			public void keyPressed(KeyEvent event)
			{
				// Dialog-owned events must never reopen the palette after its local close action.
				if (palette.isOpen() || palette.ownsKeyEvent(event))
				{
					return;
				}
				super.keyPressed(event);
			}

			@Override
			public void hotkeyPressed()
			{
				if (started && !palette.isOpen())
				{
					Keybind usedHotkey = keybind();
					palette.open();
					if (palette.isOpen())
					{
						onOpened.accept(usedHotkey);
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
		palette.configureHotkey(keybind());
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
		listener.focusLost();
		palette.disposePalette();
	}

	public void configurationChanged()
	{
		listener.focusLost();
		palette.configureHotkey(keybind());
	}

	private Keybind keybind()
	{
		Keybind configured = configuredKeybind.get();
		return configured == null ? Keybind.NOT_SET : configured;
	}

}
