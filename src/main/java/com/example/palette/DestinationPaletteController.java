package com.example.palette;

import com.example.history.LearnedHistoryStore;
import com.example.search.SearchCandidate;
import com.example.search.SearchResult;
import com.example.search.SearchService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Headless palette state and actions; the dialog only renders and forwards input. */
public final class DestinationPaletteController
{
	public static final int VISIBLE_RESULT_LIMIT = 10;

	@FunctionalInterface
	public interface SelectionHandler
	{
		void select(PaletteDestination destination, String normalizedQuery, Consumer<Boolean> onComplete);
	}

	private final Supplier<List<PaletteDestination>> destinationSource;
	private final SearchService searchService;
	private final LearnedHistoryStore historyStore;
	private final SelectionHandler selectionHandler;
	private final Runnable closeRequest;

	private Map<String, PaletteDestination> destinationsById = Map.of();
	private List<PaletteDestination> visible = List.of();
	private String query = "";
	private int selectedIndex = -1;
	private boolean navigationMoved;
	private boolean selectionPending;
	private boolean open;

	public DestinationPaletteController(Supplier<List<PaletteDestination>> destinationSource,
		SearchService searchService, LearnedHistoryStore historyStore,
		SelectionHandler selectionHandler, Runnable closeRequest)
	{
		this.destinationSource = Objects.requireNonNull(destinationSource, "destinationSource");
		this.searchService = Objects.requireNonNull(searchService, "searchService");
		this.historyStore = Objects.requireNonNull(historyStore, "historyStore");
		this.selectionHandler = Objects.requireNonNull(selectionHandler, "selectionHandler");
		this.closeRequest = Objects.requireNonNull(closeRequest, "closeRequest");
	}

	public void open()
	{
		Map<String, PaletteDestination> current = new LinkedHashMap<>();
		for (PaletteDestination destination : destinationSource.get())
		{
			current.put(destination.getId(), destination);
		}
		destinationsById = current;
		open = true;
		selectionPending = false;
		showEmptySuggestions();
	}

	public void queryChanged(String text)
	{
		if (!open)
		{
			return;
		}
		query = SearchService.normalize(text);
		if (query.isEmpty())
		{
			showEmptySuggestions();
			return;
		}
		List<SearchCandidate> candidates = new ArrayList<>(destinationsById.size());
		for (PaletteDestination destination : destinationsById.values())
		{
			candidates.add(destination.getSearchCandidate());
		}
		List<PaletteDestination> ranked = new ArrayList<>();
		for (SearchResult result : searchService.search(query, candidates))
		{
			ranked.add(destinationsById.get(result.getId()));
			if (ranked.size() == VISIBLE_RESULT_LIMIT)
			{
				break;
			}
		}
		visible = List.copyOf(ranked);
		selectedIndex = visible.isEmpty() ? -1 : 0;
		navigationMoved = false;
	}

	private void showEmptySuggestions()
	{
		query = "";
		Set<String> available = destinationsById.keySet();
		String last = historyStore.lastDestinationId(available);
		LinkedHashSet<String> ordered = new LinkedHashSet<>();
		if (last != null)
		{
			ordered.add(last);
		}
		ordered.addAll(historyStore.recentDestinationIds(available));
		Map<String, Integer> globalCounts = historyStore.globalDestinationCounts();
		List<String> frequent = new ArrayList<>();
		for (String id : globalCounts.keySet())
		{
			if (available.contains(id))
			{
				frequent.add(id);
			}
		}
		frequent.sort(Comparator.<String>comparingInt(id -> globalCounts.get(id)).reversed()
			.thenComparing(id -> destinationsById.get(id).getSearchCandidate().getNormalizedName())
			.thenComparing(id -> id));
		ordered.addAll(frequent);
		List<PaletteDestination> suggestions = new ArrayList<>();
		for (String id : ordered)
		{
			suggestions.add(destinationsById.get(id));
			if (suggestions.size() == VISIBLE_RESULT_LIMIT)
			{
				break;
			}
		}
		visible = List.copyOf(suggestions);
		selectedIndex = last == null ? -1 : 0;
		navigationMoved = false;
	}

	public void moveSelection(int direction)
	{
		if (!open || visible.isEmpty() || direction == 0)
		{
			return;
		}
		selectedIndex = selectedIndex < 0
			? (direction > 0 ? 0 : visible.size() - 1)
			: Math.floorMod(selectedIndex + Integer.signum(direction), visible.size());
		navigationMoved = true;
	}

	public void selectIndex(int index)
	{
		if (open && index >= 0 && index < visible.size())
		{
			selectedIndex = index;
			navigationMoved = true;
		}
	}

	public void confirm()
	{
		if (!open || selectionPending || selectedIndex < 0 || selectedIndex >= visible.size()
			|| (query.isEmpty() && !navigationMoved
				&& historyStore.lastDestinationId(destinationsById.keySet()) == null))
		{
			return;
		}
		PaletteDestination selected = visible.get(selectedIndex);
		selectionPending = true;
		try
		{
			selectionHandler.select(selected, query, posted ->
			{
				selectionPending = false;
				if (posted && open)
				{
					open = false;
					closeRequest.run();
				}
			});
		}
		catch (RuntimeException e)
		{
			selectionPending = false;
			throw e;
		}
	}

	public void closeWithoutSelection()
	{
		if (open)
		{
			open = false;
			closeRequest.run();
		}
	}

	public boolean isOpen()
	{
		return open;
	}

	public String getQuery()
	{
		return query;
	}

	public List<PaletteDestination> getVisible()
	{
		return visible;
	}

	public int getSelectedIndex()
	{
		return selectedIndex;
	}

	public boolean hasMovedSelection()
	{
		return navigationMoved;
	}
}
