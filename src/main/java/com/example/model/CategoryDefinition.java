package com.example.model;

import java.util.Objects;

public final class CategoryDefinition
{
	private final String id;
	private final String displayName;

	public CategoryDefinition(String id, String displayName)
	{
		this.id = Objects.requireNonNull(id, "id");
		this.displayName = Objects.requireNonNull(displayName, "displayName");
	}

	public String getId()
	{
		return id;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
