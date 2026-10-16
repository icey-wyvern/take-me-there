package com.example.onboarding;

import com.example.TakeMeThereConfig;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;

/** One-way, local onboarding state for users still on the default palette shortcut. */
public final class PaletteTipState
{
	private static final String GROUP = "takemethere";
	private static final String KEY = "paletteTipDismissed";

	private final Consumer<String> write;
	private boolean dismissed;

	public PaletteTipState(ConfigManager configManager)
	{
		this(() -> configManager.getConfiguration(GROUP, KEY),
			value -> configManager.setConfiguration(GROUP, KEY, value));
	}

	public PaletteTipState(Supplier<String> read, Consumer<String> write)
	{
		this.write = Objects.requireNonNull(write, "write");
		this.dismissed = Boolean.parseBoolean(Objects.requireNonNull(read, "read").get());
	}

	public synchronized boolean isDismissed()
	{
		return dismissed;
	}

	public synchronized boolean shouldShow(Keybind configuredHotkey)
	{
		return !dismissed && TakeMeThereConfig.DEFAULT_PALETTE_HOTKEY.equals(configuredHotkey);
	}

	/** Dismiss custom or unset bindings, including those already present at startup. */
	public synchronized boolean reconcile(Keybind configuredHotkey)
	{
		if (!TakeMeThereConfig.DEFAULT_PALETTE_HOTKEY.equals(configuredHotkey))
		{
			dismiss();
		}
		return shouldShow(configuredHotkey);
	}

	/** Called only after a hotkey press actually opened the palette. */
	public synchronized void openedWith(Keybind usedHotkey)
	{
		if (TakeMeThereConfig.DEFAULT_PALETTE_HOTKEY.equals(usedHotkey))
		{
			dismiss();
		}
	}

	public synchronized void dismiss()
	{
		if (!dismissed)
		{
			write.accept("true");
			dismissed = true;
		}
	}
}
