package com.example.favourites;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.client.config.ConfigManager;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class FavouritesStore
{
	private static final String CONFIG_GROUP = "takemethere";
	private static final String KEY_FAVOURITES = "favourites";

	private static final Type SET_TYPE = new TypeToken<LinkedHashSet<String>>() {}.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	public FavouritesStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = Objects.requireNonNull(configManager, "configManager");
		this.gson = Objects.requireNonNull(gson, "gson");
	}

	public LinkedHashSet<String> load()
	{
		String raw = configManager.getConfiguration(CONFIG_GROUP, KEY_FAVOURITES);
		if (raw == null || raw.isBlank())
		{
			return new LinkedHashSet<>();
		}

		try
		{
			LinkedHashSet<String> parsed = gson.fromJson(raw, SET_TYPE);
			return parsed == null ? new LinkedHashSet<>() : new LinkedHashSet<>(parsed);
		}
		catch (Exception e)
		{
			// If config is corrupted, don't crash the plugin; reset to empty.
			return new LinkedHashSet<>();
		}
	}

	public void save(Set<String> favouriteIds)
	{
		LinkedHashSet<String> stable = new LinkedHashSet<>(Objects.requireNonNull(favouriteIds, "favouriteIds"));
		String raw = gson.toJson(stable, SET_TYPE);
		configManager.setConfiguration(CONFIG_GROUP, KEY_FAVOURITES, raw);
	}

	public boolean isFavourite(String locationId)
	{
		return load().contains(Objects.requireNonNull(locationId, "locationId"));
	}

	public void add(String locationId)
	{
		Objects.requireNonNull(locationId, "locationId");
		LinkedHashSet<String> favourites = load();
		if (favourites.add(locationId))
		{
			save(favourites);
		}
	}

	public void remove(String locationId)
	{
		Objects.requireNonNull(locationId, "locationId");
		LinkedHashSet<String> favourites = load();
		if (favourites.remove(locationId))
		{
			save(favourites);
		}
	}
}
