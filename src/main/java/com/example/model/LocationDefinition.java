package com.example.model;

import java.util.Objects;

public final class LocationDefinition
{
	private final String id;
	private final String name;
	private final int x;
	private final int y;
	private final int plane;

	public LocationDefinition(String id, String name, int x, int y, int plane)
	{
		this.id = Objects.requireNonNull(id, "id");
		this.name = Objects.requireNonNull(name, "name");
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
}
