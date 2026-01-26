package com.example.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LocationCatalog
{
	private final List<CategoryDefinition> categories;
	private final Map<String, List<LocationDefinition>> locationsByCategoryId;
	private final Map<String, LocationDefinition> locationsById;

	public LocationCatalog(
		List<CategoryDefinition> categories,
		Map<String, List<LocationDefinition>> locationsByCategoryId,
		Map<String, LocationDefinition> locationsById)
	{
		this.categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
		this.locationsByCategoryId = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(locationsByCategoryId, "locationsByCategoryId")));
		this.locationsById = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(locationsById, "locationsById")));
	}

	public List<CategoryDefinition> getCategories()
	{
		return categories;
	}

	public Map<String, List<LocationDefinition>> getLocationsByCategoryId()
	{
		return locationsByCategoryId;
	}

	public Map<String, LocationDefinition> getLocationsById()
	{
		return locationsById;
	}
}
