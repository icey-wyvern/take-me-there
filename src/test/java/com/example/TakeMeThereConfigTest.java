package com.example;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TakeMeThereConfigTest
{
	@Test
	public void paletteHotkeyUsesExistingGroupAndVerifiedDefault() throws Exception
	{
		assertEquals("takemethere", TakeMeThereConfig.class.getAnnotation(ConfigGroup.class).value());
		ConfigItem item = TakeMeThereConfig.class.getMethod("paletteHotkey").getAnnotation(ConfigItem.class);
		assertEquals("paletteHotkey", item.keyName());
		assertEquals("Quick search hotkey", item.name());
		assertEquals("Open or close quick destination search.", item.description());
		assertEquals(new Keybind(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK),
			new TakeMeThereConfig() {}.paletteHotkey());
		assertFalse(Keybind.NOT_SET.matches(new KeyEvent(new javax.swing.JPanel(),
			KeyEvent.KEY_PRESSED, 1L, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_T, 'T')));
	}
}
