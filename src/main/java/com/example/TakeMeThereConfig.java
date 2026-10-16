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
	@ConfigItem(
		keyName = "paletteHotkey",
		name = "Search palette hotkey",
		description = "Open or close the destination search palette.",
		position = 0
	)
	default Keybind paletteHotkey()
	{
		return new Keybind(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
	}
}
