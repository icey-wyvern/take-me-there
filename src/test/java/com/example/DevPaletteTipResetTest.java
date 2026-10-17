package com.example;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class DevPaletteTipResetTest
{
	@Test
	public void devLaunchClearsOnlyTheTipDismissalKey()
	{
		Map<String, String> config = new HashMap<>();
		config.put("takemethere.paletteTipDismissed", "true");
		config.put("takemethere.learnedSearchHistory", "history");
		config.put("takemethere.favourites", "favourites");
		config.put("takemethere.customLocations", "custom");
		config.put("takemethere.paletteHotkey", "custom hotkey");
		DevPaletteTipResetPlugin.resetPaletteTip((group, key) -> config.remove(group + "." + key));
		assertFalse(config.containsKey("takemethere.paletteTipDismissed"));
		assertEquals("history", config.get("takemethere.learnedSearchHistory"));
		assertEquals("favourites", config.get("takemethere.favourites"));
		assertEquals("custom", config.get("takemethere.customLocations"));
		assertEquals("custom hotkey", config.get("takemethere.paletteHotkey"));
	}

	@Test
	public void normalDevLauncherLoadsProductionPluginAndDirectResetPlugin() throws Exception
	{
		Class<? extends Plugin>[] launched = TakeMeTherePluginTest.developmentPlugins();
		assertEquals(TakeMeTherePlugin.class, launched[0]);
		assertEquals(DevPaletteTipResetPlugin.class, launched[1]);
		assertEquals(Plugin.class, DevPaletteTipResetPlugin.class.getSuperclass());
		assertEquals(Plugin.class, TakeMeTherePlugin.class.getSuperclass());
		assertEquals(true, DevPaletteTipResetPlugin.class.getAnnotation(PluginDescriptor.class).hidden());
		assertNotNull(DevPaletteTipResetPlugin.class.getDeclaredMethod("resetAtInstantiation", ConfigManager.class)
			.getAnnotation(Inject.class));
	}
}
