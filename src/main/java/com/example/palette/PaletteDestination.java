package com.example.palette;

import com.example.search.SearchCandidate;
import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

public final class PaletteDestination
{
	private final String id;
	private final String name;
	private final String category;
	private final WorldPoint target;
	private final SearchCandidate searchCandidate;

	public PaletteDestination(String id, String name, String category, WorldPoint target)
	{
		this.id = Objects.requireNonNull(id, "id");
		this.name = Objects.requireNonNull(name, "name");
		this.category = Objects.requireNonNull(category, "category");
		this.target = Objects.requireNonNull(target, "target");
		this.searchCandidate = new SearchCandidate(id, name, category);
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getCategory()
	{
		return category;
	}

	public WorldPoint getTarget()
	{
		return target;
	}

	public SearchCandidate getSearchCandidate()
	{
		return searchCandidate;
	}
}
