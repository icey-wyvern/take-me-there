package com.example.custom;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.client.config.ConfigManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CustomLocationsStore
{
	private static final String CONFIG_GROUP = "takemethere";
	private static final String KEY_CUSTOM_LOCATIONS = "customLocations";

	private static final Type LIST_TYPE = new TypeToken<ArrayList<CustomLocation>>() {}.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	public CustomLocationsStore(ConfigManager configManager)
	{
		this(configManager, new Gson());
	}

	public CustomLocationsStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = Objects.requireNonNull(configManager, "configManager");
		this.gson = Objects.requireNonNull(gson, "gson");
	}

	public List<CustomLocation> load()
	{
		String raw = configManager.getConfiguration(CONFIG_GROUP, KEY_CUSTOM_LOCATIONS);
		if (raw == null || raw.isBlank())
		{
			return new ArrayList<>();
		}

		try
		{
			List<CustomLocation> parsed = gson.fromJson(raw, LIST_TYPE);
			return parsed == null ? new ArrayList<>() : new ArrayList<>(parsed);
		}
		catch (Exception e)
		{
			return new ArrayList<>();
		}
	}

	public void save(List<CustomLocation> locations)
	{
		String raw = gson.toJson(Objects.requireNonNull(locations, "locations"), LIST_TYPE);
		configManager.setConfiguration(CONFIG_GROUP, KEY_CUSTOM_LOCATIONS, raw);
	}
}
