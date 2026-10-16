package com.example.search;

import java.util.Objects;

public final class SearchCandidate
{
	private final String id;
	private final String displayName;
	private final String normalizedName;
	private final String normalizedCategory;

	public SearchCandidate(String id, String displayName)
	{
		this(id, displayName, null);
	}

	public SearchCandidate(String id, String displayName, String categoryName)
	{
		this.id = Objects.requireNonNull(id, "id");
		this.displayName = Objects.requireNonNull(displayName, "displayName");
		this.normalizedName = SearchService.normalize(displayName);
		this.normalizedCategory = SearchService.normalize(categoryName);
	}

	public String getId()
	{
		return id;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getNormalizedName()
	{
		return normalizedName;
	}

	public String getNormalizedCategory()
	{
		return normalizedCategory;
	}
}
