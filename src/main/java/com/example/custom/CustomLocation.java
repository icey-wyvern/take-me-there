package com.example.custom;

import java.util.Objects;

public final class CustomLocation
{
	private final String id;
	private final String name;
	private final String categoryId;
	private final int x;
	private final int y;
	private final int plane;

	public CustomLocation(String id, String name, String categoryId, int x, int y, int plane)
	{
		this.id = Objects.requireNonNull(id, "id");
		this.name = Objects.requireNonNull(name, "name");
		this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
		this.x = x;
		this.y = y;
		this.plane = plane;
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getCategoryId()
	{
		return categoryId;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getPlane()
	{
		return plane;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof CustomLocation)) return false;
		CustomLocation that = (CustomLocation) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}
}
