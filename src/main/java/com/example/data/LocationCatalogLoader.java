package com.example.data;

import com.example.model.CategoryDefinition;
import com.example.model.LocationCatalog;
import com.example.model.LocationDefinition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class LocationCatalogLoader
{
	private static final String BASE_PATH = "/takemethere/";
	private static final String CATEGORIES_FILE = "categories.json";

	private final Gson gson;

	public LocationCatalogLoader(Gson gson)
	{
		this.gson = Objects.requireNonNull(gson, "gson");
	}

	public LocationCatalog load()
	{
		List<CategoryDefinition> categories = loadCategories();

		Map<String, List<LocationDefinition>> byCategory = new LinkedHashMap<>();
		Map<String, LocationDefinition> byId = new LinkedHashMap<>();

		for (CategoryDefinition category : categories)
		{
			String categoryId = category.getId();
			String filename = categoryId + ".json";

			List<LocationDefinition> locations = loadLocationsFile(filename);

			for (LocationDefinition loc : locations)
			{
				if (byId.containsKey(loc.getId()))
				{
					throw new IllegalStateException("Duplicate location id: " + loc.getId());
				}
				byId.put(loc.getId(), loc);
			}

			byCategory.put(categoryId, List.copyOf(locations));
		}

		return new LocationCatalog(categories, byCategory, byId);
	}

	private List<CategoryDefinition> loadCategories()
	{
		Type type = new TypeToken<List<CategoryDefinition>>() {}.getType();
		List<CategoryDefinition> categories = readJson(CATEGORIES_FILE, type);

		if (categories.isEmpty())
		{
			throw new IllegalStateException("No categories found in " + CATEGORIES_FILE);
		}

		Set<String> seen = new HashSet<>();
		for (CategoryDefinition c : categories)
		{
			if (!seen.add(c.getId()))
			{
				throw new IllegalStateException("Duplicate category id: " + c.getId());
			}
		}

		return List.copyOf(categories);
	}

	private List<LocationDefinition> loadLocationsFile(String filename)
	{
		Type type = new TypeToken<List<LocationDefinition>>() {}.getType();
		return List.copyOf(readJson(filename, type));
	}

	private <T> T readJson(String filename, Type type)
	{
		String resourcePath = BASE_PATH + filename;

		InputStream in = LocationCatalogLoader.class.getResourceAsStream(resourcePath);
		if (in == null)
		{
			throw new IllegalStateException("Missing resource: " + resourcePath);
		}

		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return gson.fromJson(reader, type);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Failed to read resource: " + resourcePath, e);
		}
	}
}
