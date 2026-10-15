package com.example;

import com.example.data.LocationCatalogLoader;
import com.example.model.LocationCatalog;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

public class CatalogIntegrityTest
{
	private static final String BASE_PATH = "/takemethere/";

	@Test
	public void allReferencedCatalogsAreValid() throws Exception
	{
		JsonArray categories = readArray("categories.json");
		assertFalse("No categories", categories.size() == 0);

		Set<String> categoryIds = new HashSet<>();
		Set<String> locationIds = new HashSet<>();
		for (JsonElement element : categories)
		{
			JsonObject category = element.getAsJsonObject();
			assertEquals(Set.of("id", "displayName"), category.keySet());
			String categoryId = requiredString(category, "id");
			assertTrue("Invalid category ID: " + categoryId, categoryId.matches("[a-z0-9-]+"));
			assertTrue("Duplicate category ID: " + categoryId, categoryIds.add(categoryId));
			requiredString(category, "displayName");

			JsonArray locations = readArray(categoryId + ".json");
			assertTrue("Empty category: " + categoryId, locations.size() > 0);
			Set<String> visibleNames = new HashSet<>();
			for (JsonElement locationElement : locations)
			{
				JsonObject location = locationElement.getAsJsonObject();
				assertEquals(categoryId + " has unexpected or missing fields",
					Set.of("id", "name", "x", "y", "plane"), location.keySet());
				String id = requiredString(location, "id");
				assertTrue("Location ID has wrong category: " + id,
					id.startsWith(categoryId + ":") && id.length() > categoryId.length() + 1);
				assertTrue("Duplicate location ID: " + id, locationIds.add(id));
				String name = requiredString(location, "name");
				assertTrue("Duplicate visible name in " + categoryId + ": " + name,
					visibleNames.add(name.trim().toLowerCase(Locale.ROOT)));
				int x = requiredInt(location, "x");
				int y = requiredInt(location, "y");
				int plane = requiredInt(location, "plane");
				assertTrue(id + " has invalid x", x >= 1000 && x <= 5000);
				assertTrue(id + " has invalid y", y >= 2000 && y <= 16000);
				assertTrue(id + " has invalid plane", plane >= 0 && plane <= 3);
			}
		}

		LocationCatalog loaded = new LocationCatalogLoader(new Gson()).load();
		assertEquals(categoryIds.size(), loaded.getCategories().size());
		assertEquals(locationIds.size(), loaded.getLocationsById().size());
	}

	private static JsonArray readArray(String filename) throws Exception
	{
		String path = BASE_PATH + filename;
		InputStream stream = CatalogIntegrityTest.class.getResourceAsStream(path);
		assertNotNull("Missing catalog resource: " + path, stream);
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			JsonElement parsed = new JsonParser().parse(reader);
			assertTrue("Expected JSON array: " + path, parsed.isJsonArray());
			return parsed.getAsJsonArray();
		}
	}

	private static String requiredString(JsonObject object, String field)
	{
		JsonElement value = object.get(field);
		assertNotNull("Missing " + field, value);
		assertTrue("Expected string: " + field, value.isJsonPrimitive() && value.getAsJsonPrimitive().isString());
		String text = value.getAsString();
		assertFalse("Blank " + field, text.trim().isEmpty());
		return text;
	}

	private static int requiredInt(JsonObject object, String field)
	{
		JsonElement value = object.get(field);
		assertNotNull("Missing " + field, value);
		assertTrue("Expected integer: " + field,
			value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
				&& value.getAsString().matches("-?[0-9]+"));
		return value.getAsInt();
	}
}
