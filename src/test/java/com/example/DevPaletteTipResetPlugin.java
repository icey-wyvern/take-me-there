package com.example;

import java.util.Objects;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/** Dev-launch hook: all plugins are instantiated before Take Me There starts. */
@PluginDescriptor(name = "Take Me There Dev Tip Reset", hidden = true)
public class DevPaletteTipResetPlugin extends Plugin
{
	@Inject
	void resetAtInstantiation(ConfigManager configManager)
	{
		resetPaletteTip(configManager::unsetConfiguration);
	}

	static void resetPaletteTip(BiConsumer<String, String> unset)
	{
		Objects.requireNonNull(unset, "unset").accept("takemethere", "paletteTipDismissed");
	}
}
