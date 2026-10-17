package com.example;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;

public class TakeMeTherePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(developmentPlugins());
		RuneLite.main(args);
	}

	static Class<? extends Plugin>[] developmentPlugins()
	{
		return new Class[]{TakeMeTherePlugin.class, DevPaletteTipResetPlugin.class};
	}
}
