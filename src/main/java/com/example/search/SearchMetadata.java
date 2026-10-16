package com.example.search;

import com.example.model.LocationCatalog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Curated search terms keyed by existing catalog IDs. Shared aliases may name several destinations. */
public final class SearchMetadata
{
	private static final String RESOURCE = "/takemethere/search-aliases.json";
	private static final Type ENTRIES_TYPE = new TypeToken<List<Entry>>() {}.getType();

	private final Map<String, List<String>> aliasesById;

	public SearchMetadata(Map<String, List<String>> aliasesById)
	{
		Objects.requireNonNull(aliasesById, "aliasesById");
		Map<String, List<String>> normalized = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : aliasesById.entrySet())
		{
			String id = Objects.requireNonNull(entry.getKey(), "destination ID");
			Set<String> seen = new HashSet<>();
			List<String> aliases = new ArrayList<>();
			for (String raw : Objects.requireNonNull(entry.getValue(), "aliases for " + id))
			{
				String alias = SearchService.normalize(raw);
				if (alias.isEmpty() || !seen.add(alias))
				{
					throw new IllegalArgumentException("Blank or duplicate alias for " + id + ": " + raw);
				}
				aliases.add(alias);
			}
			normalized.put(id, List.copyOf(aliases));
		}
		this.aliasesById = Map.copyOf(normalized);
	}

	public static SearchMetadata load(Gson gson, LocationCatalog catalog)
	{
		Objects.requireNonNull(gson, "gson");
		Objects.requireNonNull(catalog, "catalog");
		InputStream stream = SearchMetadata.class.getResourceAsStream(RESOURCE);
		if (stream == null)
		{
			throw new IllegalStateException("Missing search metadata: " + RESOURCE);
		}
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			List<Entry> entries = gson.fromJson(reader, ENTRIES_TYPE);
			if (entries == null)
			{
				throw new IllegalStateException("Empty search metadata: " + RESOURCE);
			}
			Map<String, List<String>> aliases = new HashMap<>();
			for (Entry entry : entries)
			{
				if (entry == null || entry.id == null || !catalog.getLocationsById().containsKey(entry.id))
				{
					throw new IllegalStateException("Unknown destination ID in search metadata: " + (entry == null ? null : entry.id));
				}
				if (aliases.putIfAbsent(entry.id, entry.aliases) != null)
				{
					throw new IllegalStateException("Duplicate search metadata ID: " + entry.id);
				}
			}
			return new SearchMetadata(aliases);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Failed to load search metadata: " + RESOURCE, e);
		}
	}

	public List<String> aliasesFor(String id)
	{
		return aliasesById.getOrDefault(id, List.of());
	}

	public Set<String> destinationIds()
	{
		return aliasesById.keySet();
	}

	private static final class Entry
	{
		private String id;
		private List<String> aliases;
	}
}
