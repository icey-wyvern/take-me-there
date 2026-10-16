package com.example.palette;

import com.example.custom.CustomLocation;
import com.example.model.CategoryDefinition;
import com.example.model.LocationCatalog;
import com.example.model.LocationDefinition;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PaletteDestinationSourceTest
{
	@Test
	public void resolvesBuiltInAndCustomDestinationsByStableId()
	{
		CategoryDefinition category = new CategoryDefinition("shops", "Shops");
		LocationDefinition builtIn = new LocationDefinition("built-in", "Magic Shop", 3200, 3201, 0);
		LocationCatalog catalog = new LocationCatalog(List.of(category),
			Map.of("shops", List.of(builtIn)), Map.of("built-in", builtIn));
		CustomLocation custom = new CustomLocation("custom:uuid", "Magic Shop", "custom", 3300, 3301, 0);
		List<PaletteDestination> resolved = PaletteDestinationSource.from(catalog, List.of(custom));
		assertEquals(List.of("built-in", "custom:uuid"),
			List.of(resolved.get(0).getId(), resolved.get(1).getId()));
		assertEquals(new WorldPoint(3200, 3201, 0), resolved.get(0).getTarget());
		assertEquals(new WorldPoint(3300, 3301, 0), resolved.get(1).getTarget());
	}
}
