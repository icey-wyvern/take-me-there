package com.example;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("takemethere")
public interface TakeMeThereConfig extends Config
{
	Keybind DEFAULT_PALETTE_HOTKEY = new Keybind(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);

	@ConfigItem(
		keyName = "paletteHotkey",
		name = "Search palette hotkey",
		description = "Open or close the destination search palette.",
		position = 0
	)
	default Keybind paletteHotkey()
	{
		return DEFAULT_PALETTE_HOTKEY;
	}
}
