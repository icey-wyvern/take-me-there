package com.example;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
		DevTakeMeTherePlugin.resetPaletteTip((group, key) -> config.remove(group + "." + key));
		assertFalse(config.containsKey("takemethere.paletteTipDismissed"));
		assertEquals("history", config.get("takemethere.learnedSearchHistory"));
		assertEquals("favourites", config.get("takemethere.favourites"));
		assertEquals("custom", config.get("takemethere.customLocations"));
		assertEquals("custom hotkey", config.get("takemethere.paletteHotkey"));
	}
}
