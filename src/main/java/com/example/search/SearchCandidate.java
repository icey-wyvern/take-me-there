package com.example.search;

import java.util.Objects;

public final class SearchCandidate
{
	private final String id;
	private final String displayName;
	private final String normalizedName;

	public SearchCandidate(String id, String displayName)
	{
		this.id = Objects.requireNonNull(id, "id");
		this.displayName = Objects.requireNonNull(displayName, "displayName");
		this.normalizedName = SearchService.normalize(displayName);
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
}
