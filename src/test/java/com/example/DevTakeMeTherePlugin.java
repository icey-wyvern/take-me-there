package com.example;

import java.util.Objects;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginDescriptor;

/** Test-classpath plugin used by the local RuneLite development launcher only. */
@PluginDescriptor(name = "Take Me There")
public class DevTakeMeTherePlugin extends TakeMeTherePlugin
{
	@Inject
	private ConfigManager configManager;

	@Override
	protected void startUp()
	{
		resetPaletteTip(configManager::unsetConfiguration);
		super.startUp();
	}

	static void resetPaletteTip(BiConsumer<String, String> unset)
	{
		Objects.requireNonNull(unset, "unset").accept("takemethere", "paletteTipDismissed");
	}
}
