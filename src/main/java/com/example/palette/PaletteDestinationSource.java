package com.example.palette;

import com.example.custom.CustomLocation;
import com.example.custom.CustomLocationsStore;
import com.example.model.CategoryDefinition;
import com.example.model.LocationCatalog;
import com.example.model.LocationDefinition;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/** Resolves current built-in and custom destinations each time the palette opens. */
public final class PaletteDestinationSource
{
	private PaletteDestinationSource()
	{
	}

	public static List<PaletteDestination> load(LocationCatalog catalog, CustomLocationsStore customStore)
	{
		return from(catalog, customStore == null ? List.of() : customStore.load());
	}

	static List<PaletteDestination> from(LocationCatalog catalog, List<CustomLocation> customLocations)
	{
		List<PaletteDestination> destinations = new ArrayList<>();
		for (CategoryDefinition category : catalog.getCategories())
		{
			for (LocationDefinition location : catalog.getLocationsByCategoryId().getOrDefault(category.getId(), List.of()))
			{
				destinations.add(new PaletteDestination(location.getId(), location.getName(),
					category.getDisplayName(), new WorldPoint(location.getX(), location.getY(), location.getPlane())));
			}
		}
		for (CustomLocation location : customLocations)
		{
			if (location != null)
			{
				destinations.add(new PaletteDestination(location.getId(), location.getName(),
					"Custom Locations", new WorldPoint(location.getX(), location.getY(), location.getPlane())));
			}
		}
		return destinations;
	}
}
