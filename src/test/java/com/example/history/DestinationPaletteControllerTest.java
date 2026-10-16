package com.example.history;

import com.example.palette.DestinationPaletteController;
import com.example.palette.PaletteDestination;
import com.example.search.SearchMetadata;
import com.example.search.SearchService;
import com.google.gson.Gson;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class DestinationPaletteControllerTest
{
	private final Map<String, String> config = new HashMap<>();
	private final LearnedHistoryStore history = new LearnedHistoryStore(
		() -> config.get("history"), value -> config.put("history", value),
		() -> config.remove("history"), new Gson());
	private final List<Selection> selections = new ArrayList<>();
	private int closes;
	private List<PaletteDestination> destinations = List.of(
		destination("a", "Magic Shop"), destination("b", "Magic Store"),
		destination("c", "Sawmill"), destination("custom:uuid", "My Magic Shop"));

	@Test
	public void historySuggestionsAreOrderedDeduplicatedAndDoNotChangeHistory()
	{
		history.recordSelection("a", "");
		history.recordSelection("b", "");
		history.recordSelection("a", "");
		history.recordSelection("stale", "");
		DestinationPaletteController controller = controller();
		controller.open();
		assertEquals(List.of("a", "b"), ids(controller));
		assertEquals(-1, controller.getSelectedIndex()); // stale last ID cannot become an action
		assertEquals("stale", history.lastDestinationId(Set.of("stale")));
		controller.confirm();
		assertTrue(selections.isEmpty());
		assertEquals("stale", history.lastDestinationId(Set.of("stale")));
	}

	@Test
	public void lastDestinationDefaultsToRepeatAndRecentsPrecedeGlobalFrequency()
	{
		for (int i = 0; i < 4; i++) history.recordSelection("c", "");
		history.recordSelection("b", "");
		history.recordSelection("a", "");
		DestinationPaletteController controller = controller();
		controller.open();
		assertEquals(List.of("a", "b", "c"), ids(controller));
		assertEquals(0, controller.getSelectedIndex());
		controller.confirm();
		assertEquals(1, selections.size());
		assertEquals("a", selections.get(0).destination.getId());
		assertEquals("", selections.get(0).query);
		selections.get(0).complete.accept(true);
		assertEquals(1, closes);
		assertFalse(controller.isOpen());
	}

	@Test
	public void noLastRequiresNavigationAndArrowsWrap()
	{
		history.recordSelection("b", "");
		history.recordSelection("c", "");
		history.recordSelection("stale", "");
		DestinationPaletteController controller = controller();
		controller.open();
		assertEquals(-1, controller.getSelectedIndex());
		controller.confirm();
		assertTrue(selections.isEmpty());
		controller.moveSelection(-1);
		assertEquals(1, controller.getSelectedIndex());
		assertTrue(controller.hasMovedSelection());
		controller.moveSelection(1);
		assertEquals(0, controller.getSelectedIndex());
		controller.moveSelection(-1);
		assertEquals(1, controller.getSelectedIndex());
		controller.confirm();
		assertEquals("b", selections.get(0).destination.getId());
		assertEquals("", selections.get(0).query);
	}

	@Test
	public void emptyHistoryCannotRouteOnImmediateEnter()
	{
		DestinationPaletteController controller = controller();
		controller.open();
		assertTrue(controller.getVisible().isEmpty());
		assertEquals(-1, controller.getSelectedIndex());
		controller.confirm();
		assertTrue(selections.isEmpty());
	}

	@Test
	public void searchUsesRankingAndNormalizedQueryThenClearingRestoresLast()
	{
		history.recordSelection("b", "magic");
		history.recordSelection("a", "");
		DestinationPaletteController controller = controller();
		controller.open();
		controller.queryChanged("  MAGIC!!!  ");
		assertEquals("magic", controller.getQuery());
		assertEquals("b", controller.getVisible().get(0).getId());
		assertEquals(0, controller.getSelectedIndex());
		controller.moveSelection(1);
		assertEquals(1, controller.getSelectedIndex());
		controller.moveSelection(-1);
		assertEquals(0, controller.getSelectedIndex());
		controller.queryChanged("Sawmill");
		assertEquals("c", controller.getVisible().get(0).getId());
		assertEquals(0, controller.getSelectedIndex());
		controller.queryChanged(" ");
		assertEquals("a", controller.getVisible().get(0).getId());
		assertEquals(0, controller.getSelectedIndex());
		assertFalse(controller.hasMovedSelection());
	}

	@Test
	public void searchSelectionSupportsCustomIdAndClosesOnlyAfterSuccessfulCompletion()
	{
		DestinationPaletteController controller = controller();
		controller.open();
		controller.queryChanged("my magic shop");
		controller.confirm();
		controller.confirm(); // pending request cannot dispatch twice
		assertEquals(1, selections.size());
		assertEquals("custom:uuid", selections.get(0).destination.getId());
		assertEquals("my magic shop", selections.get(0).query);
		selections.get(0).complete.accept(false);
		assertTrue(controller.isOpen());
		assertEquals(0, closes);
		controller.confirm();
		assertEquals(2, selections.size());
		selections.get(1).complete.accept(true);
		assertFalse(controller.isOpen());
		assertEquals(1, closes);
	}

	@Test
	public void noResultEscapeAndMouseSelectionAreSafe()
	{
		DestinationPaletteController controller = controller();
		controller.open();
		controller.queryChanged("totally unrelated destination");
		assertTrue(controller.getVisible().isEmpty());
		controller.moveSelection(1);
		controller.confirm();
		assertTrue(selections.isEmpty());
		controller.queryChanged("magic");
		controller.selectIndex(1); // same action the dialog uses for a mouse click
		controller.confirm(); // same action the dialog uses for a double-click
		assertEquals(1, selections.size());
		assertEquals("b", selections.get(0).destination.getId());
		selections.get(0).complete.accept(false);
		controller.closeWithoutSelection();
		assertEquals(1, closes);
		assertEquals(1, selections.size());
	}

	@Test
	public void suggestionsAreCappedAndEqualGlobalCountsUseNameThenId()
	{
		List<PaletteDestination> many = new ArrayList<>();
		for (int i = 12; i >= 0; i--)
		{
			many.add(destination(String.format("id%02d", i), "Place"));
			history.recordSelection(String.format("id%02d", i), "");
		}
		destinations = many;
		DestinationPaletteController controller = controller();
		controller.open();
		assertEquals(DestinationPaletteController.VISIBLE_RESULT_LIMIT, controller.getVisible().size());
		assertEquals("id00", controller.getVisible().get(0).getId());
		controller.queryChanged("Place");
		assertEquals(DestinationPaletteController.VISIBLE_RESULT_LIMIT, controller.getVisible().size());
	}

	@Test
	public void globalSuggestionsUseCountThenNameThenStableId()
	{
		config.put("history", "{\"version\":1,\"lastDestinationId\":null,"
			+ "\"globalCounts\":{\"a\":2,\"b\":3,\"c\":3,\"custom:uuid\":3},"
			+ "\"queries\":[],\"recentIds\":[]}");
		LearnedHistoryStore reloaded = new LearnedHistoryStore(
			() -> config.get("history"), value -> config.put("history", value),
			() -> config.remove("history"), new Gson());
		DestinationPaletteController controller = controller(reloaded);
		controller.open();
		assertEquals(List.of("b", "custom:uuid", "c", "a"), ids(controller));
		assertEquals(-1, controller.getSelectedIndex());
	}

	private DestinationPaletteController controller()
	{
		return controller(history);
	}

	private DestinationPaletteController controller(LearnedHistoryStore store)
	{
		SearchService search = new SearchService(new SearchMetadata(Map.of()), store);
		return new DestinationPaletteController(() -> destinations, search, store,
			(destination, query, complete) -> selections.add(new Selection(destination, query, complete)),
			() -> closes++);
	}

	private static PaletteDestination destination(String id, String name)
	{
		return new PaletteDestination(id, name, "Test category", new WorldPoint(3200, 3200, 0));
	}

	private static List<String> ids(DestinationPaletteController controller)
	{
		List<String> ids = new ArrayList<>();
		for (PaletteDestination destination : controller.getVisible()) ids.add(destination.getId());
		return ids;
	}

	private static final class Selection
	{
		private final PaletteDestination destination;
		private final String query;
		private final Consumer<Boolean> complete;

		private Selection(PaletteDestination destination, String query, Consumer<Boolean> complete)
		{
			this.destination = destination;
			this.query = query;
			this.complete = complete;
		}
	}
}
