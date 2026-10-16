package com.example.history;

import com.example.search.SearchService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.runelite.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Local selection requests sent to Shortest Path, not completed journeys. */
public final class LearnedHistoryStore
{
	public static final int MAX_QUERIES = 100;
	public static final int MAX_PER_QUERY = 5;
	public static final int MAX_GLOBAL = 200;
	public static final int MAX_RECENT = 20;
	public static final int MAX_COUNT = 1_000_000;
	private static final int MAX_ID_LENGTH = 160;
	private static final int MAX_QUERY_LENGTH = 120;
	private static final int VERSION = 1;
	private static final int MAX_JSON_LENGTH = 200_000;
	private static final String CONFIG_GROUP = "takemethere";
	private static final String CONFIG_KEY = "learnedSearchHistory";

	private final Supplier<String> read;
	private final Consumer<String> write;
	private final Runnable remove;
	private final Gson gson;
	private State state;

	public LearnedHistoryStore(ConfigManager configManager, Gson gson)
	{
		this(() -> configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY),
			value -> configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, value),
			() -> configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY), gson);
	}

	LearnedHistoryStore(Supplier<String> read, Consumer<String> write, Runnable remove, Gson gson)
	{
		this.read = Objects.requireNonNull(read, "read");
		this.write = Objects.requireNonNull(write, "write");
		this.remove = Objects.requireNonNull(remove, "remove");
		this.gson = Objects.requireNonNull(gson, "gson");
		this.state = load();
	}

	public synchronized void recordSelection(String destinationId, String query)
	{
		if (!validId(destinationId))
		{
			return;
		}
		String normalizedQuery = SearchService.normalize(query);
		state.lastDestinationId = destinationId;
		increment(state.globalCounts, destinationId, MAX_GLOBAL, destinationId);
		state.recentIds.remove(destinationId);
		state.recentIds.add(0, destinationId);
		if (state.recentIds.size() > MAX_RECENT)
		{
			state.recentIds.remove(state.recentIds.size() - 1);
		}
		if (!normalizedQuery.isEmpty() && normalizedQuery.length() <= MAX_QUERY_LENGTH)
		{
			QueryRecord record = null;
			for (QueryRecord existing : state.queries)
			{
				if (existing.query.equals(normalizedQuery))
				{
					record = existing;
					break;
				}
			}
			if (record == null)
			{
				record = new QueryRecord(normalizedQuery);
			}
			else
			{
				state.queries.remove(record);
			}
			increment(record.counts, destinationId, MAX_PER_QUERY, destinationId);
			state.queries.add(record); // oldest query first; this is storage LRU only
			if (state.queries.size() > MAX_QUERIES)
			{
				state.queries.remove(0);
			}
		}
		write.accept(gson.toJson(state));
	}

	public synchronized RankingCounts countsFor(String query)
	{
		String normalizedQuery = SearchService.normalize(query);
		Map<String, Integer> queryCounts = Map.of();
		for (QueryRecord record : state.queries)
		{
			if (record.query.equals(normalizedQuery))
			{
				queryCounts = Map.copyOf(record.counts);
				break;
			}
		}
		return new RankingCounts(queryCounts, Map.copyOf(state.globalCounts));
	}

	public synchronized String lastDestinationId(Set<String> availableIds)
	{
		return state.lastDestinationId != null && availableIds.contains(state.lastDestinationId)
			? state.lastDestinationId : null;
	}

	public synchronized List<String> recentDestinationIds(Set<String> availableIds)
	{
		List<String> resolved = new ArrayList<>();
		for (String id : state.recentIds)
		{
			if (availableIds.contains(id))
			{
				resolved.add(id);
			}
		}
		return List.copyOf(resolved);
	}

	public synchronized Map<String, Integer> globalDestinationCounts()
	{
		return Map.copyOf(state.globalCounts);
	}

	public synchronized void clear()
	{
		remove.run();
		state = new State();
	}

	private State load()
	{
		try
		{
			String raw = read.get();
			if (raw == null || raw.isBlank() || raw.length() > MAX_JSON_LENGTH)
			{
				return new State();
			}
			JsonObject object = gson.fromJson(raw, JsonObject.class);
			if (object == null || !object.has("version") || !object.has("lastDestinationId")
				|| !object.has("globalCounts") || !object.has("queries") || !object.has("recentIds"))
			{
				return new State();
			}
			State parsed = gson.fromJson(object, State.class);
			return valid(parsed) ? parsed : new State();
		}
		catch (RuntimeException e)
		{
			return new State();
		}
	}

	private static boolean valid(State value)
	{
		if (value == null || value.version != VERSION || value.globalCounts == null
			|| value.queries == null || value.recentIds == null
			|| value.globalCounts.size() > MAX_GLOBAL || value.queries.size() > MAX_QUERIES
			|| value.recentIds.size() > MAX_RECENT || !validIdOrNull(value.lastDestinationId)
			|| !validCounts(value.globalCounts, MAX_GLOBAL))
		{
			return false;
		}
		Set<String> queries = new HashSet<>();
		for (QueryRecord record : value.queries)
		{
			if (record == null || record.query == null || record.query.isEmpty()
				|| record.query.length() > MAX_QUERY_LENGTH
				|| !record.query.equals(SearchService.normalize(record.query))
				|| !queries.add(record.query) || !validCounts(record.counts, MAX_PER_QUERY))
			{
				return false;
			}
		}
		Set<String> recent = new HashSet<>();
		for (String id : value.recentIds)
		{
			if (!validId(id) || !recent.add(id))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean validCounts(Map<String, Integer> counts, int maximum)
	{
		if (counts == null || counts.size() > maximum)
		{
			return false;
		}
		for (Map.Entry<String, Integer> entry : counts.entrySet())
		{
			if (!validId(entry.getKey()) || entry.getValue() == null
				|| entry.getValue() < 1 || entry.getValue() > MAX_COUNT)
			{
				return false;
			}
		}
		return true;
	}

	private static boolean validId(String id)
	{
		return id != null && !id.isBlank() && id.length() <= MAX_ID_LENGTH;
	}

	private static boolean validIdOrNull(String id)
	{
		return id == null || validId(id);
	}

	private static void increment(Map<String, Integer> counts, String id, int maximum, String protectedId)
	{
		counts.put(id, Math.min(MAX_COUNT, counts.getOrDefault(id, 0) + 1));
		if (counts.size() > maximum)
		{
			String victim = counts.entrySet().stream()
				.filter(entry -> !entry.getKey().equals(protectedId))
				.min((left, right) ->
				{
					int byCount = Integer.compare(left.getValue(), right.getValue());
					return byCount != 0 ? byCount : right.getKey().compareTo(left.getKey());
				})
				.orElseThrow().getKey();
			counts.remove(victim);
		}
	}

	public static final class RankingCounts
	{
		private final Map<String, Integer> queryCounts;
		private final Map<String, Integer> globalCounts;

		private RankingCounts(Map<String, Integer> queryCounts, Map<String, Integer> globalCounts)
		{
			this.queryCounts = queryCounts;
			this.globalCounts = globalCounts;
		}

		public int queryCount(String id)
		{
			return queryCounts.getOrDefault(id, 0);
		}

		public int globalCount(String id)
		{
			return globalCounts.getOrDefault(id, 0);
		}
	}

	private static final class State
	{
		private int version = VERSION;
		private String lastDestinationId;
		private Map<String, Integer> globalCounts = new HashMap<>();
		private List<QueryRecord> queries = new ArrayList<>();
		private List<String> recentIds = new ArrayList<>();
	}

	private static final class QueryRecord
	{
		private String query;
		private Map<String, Integer> counts = new LinkedHashMap<>();

		private QueryRecord(String query)
		{
			this.query = query;
		}
	}
}
