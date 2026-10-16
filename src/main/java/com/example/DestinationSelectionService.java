package com.example;

import com.example.history.LearnedHistoryStore;
import com.example.search.SearchService;
import net.runelite.api.coords.WorldPoint;

import javax.swing.SwingUtilities;
import java.util.Objects;
import java.util.function.Consumer;

/** Shared destination selection for sidebar and future palette. */
public final class DestinationSelectionService
{
	private final DestinationService destinationService;
	private final LearnedHistoryStore historyStore;
	private final Consumer<Runnable> historyExecutor;

	public DestinationSelectionService(DestinationService destinationService, LearnedHistoryStore historyStore)
	{
		this(destinationService, historyStore, SwingUtilities::invokeLater);
	}

	public DestinationSelectionService(DestinationService destinationService, LearnedHistoryStore historyStore,
		Consumer<Runnable> historyExecutor)
	{
		this.destinationService = Objects.requireNonNull(destinationService, "destinationService");
		this.historyStore = historyStore;
		this.historyExecutor = Objects.requireNonNull(historyExecutor, "historyExecutor");
	}

	public void select(String destinationId, WorldPoint target, String query)
	{
		if (destinationId == null || destinationId.isBlank() || target == null)
		{
			return;
		}
		String normalizedQuery = SearchService.normalize(query);
		destinationService.setDestination(target, () ->
		{
			if (historyStore != null)
			{
				historyExecutor.accept(() -> historyStore.recordSelection(destinationId, normalizedQuery));
			}
		});
	}
}
