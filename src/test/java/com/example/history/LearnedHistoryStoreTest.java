package com.example.history;

import com.google.gson.Gson;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class LearnedHistoryStoreTest
{
	private final Gson gson = new Gson();
	private final Map<String, String> config = new HashMap<>();

	@Test
	public void emptyStoreRoundTripLastCountsAndRecents()
	{
		LearnedHistoryStore store = store();
		assertNull(store.lastDestinationId(Set.of("a")));
		assertTrue(store.recentDestinationIds(Set.of("a")).isEmpty());
		assertEquals(0, store.countsFor("magic shop").globalCount("a"));

		store.recordSelection("a", "  MAGIC,   SHOP! ");
		store.recordSelection("b", "magic shop");
		store.recordSelection("a", "");
		LearnedHistoryStore reloaded = store();
		assertEquals("a", reloaded.lastDestinationId(Set.of("a", "b")));
		assertEquals(List.of("a", "b"), reloaded.recentDestinationIds(Set.of("a", "b")));
		assertEquals(2, reloaded.countsFor("magic shop").globalCount("a"));
		assertEquals(1, reloaded.countsFor("MAGIC--SHOP").queryCount("a"));
		assertEquals(1, reloaded.countsFor("magic shop").queryCount("b"));
		assertEquals(0, reloaded.countsFor("").queryCount("a"));
		assertTrue(config.get("learnedSearchHistory").contains("\"version\":1"));
	}

	@Test
	public void queryEvictionUsesPersistedLeastRecentlyUsedOrder()
	{
		LearnedHistoryStore store = store();
		for (int i = 0; i < LearnedHistoryStore.MAX_QUERIES; i++)
		{
			store.recordSelection("id", "query " + i);
		}
		store.recordSelection("id", "query 0");
		store.recordSelection("id", "query 100");
		LearnedHistoryStore reloaded = store();
		assertEquals(2, reloaded.countsFor("query 0").queryCount("id"));
		assertEquals(0, reloaded.countsFor("query 1").queryCount("id"));
		assertEquals(1, reloaded.countsFor("query 100").queryCount("id"));
	}

	@Test
	public void perQueryAndGlobalCapsKeepNewSelectionDeterministically()
	{
		LearnedHistoryStore store = store();
		for (String id : List.of("a", "b", "c", "d", "e", "f"))
		{
			store.recordSelection(id, "same query");
		}
		assertEquals(0, store.countsFor("same query").queryCount("e"));
		assertEquals(1, store.countsFor("same query").queryCount("f"));
		for (int i = 0; i <= LearnedHistoryStore.MAX_GLOBAL; i++)
		{
			store.recordSelection(String.format("global:%03d", i), "");
		}
		assertEquals(0, store.countsFor("").globalCount("global:199"));
		assertEquals(1, store.countsFor("").globalCount("global:200"));
		assertEquals(0, store.countsFor("").queryCount("global:200"));
	}

	@Test
	public void countersSaturateAndRecentIdsStayUniqueAndBounded()
	{
		config.put("learnedSearchHistory", "{\"version\":1,\"lastDestinationId\":\"a\","
			+ "\"globalCounts\":{\"a\":1000000},"
			+ "\"queries\":[{\"query\":\"q\",\"counts\":{\"a\":1000000}}],"
			+ "\"recentIds\":[\"a\"]}");
		LearnedHistoryStore store = store();
		store.recordSelection("a", "q");
		assertEquals(LearnedHistoryStore.MAX_COUNT, store.countsFor("q").queryCount("a"));
		assertEquals(LearnedHistoryStore.MAX_COUNT, store.countsFor("q").globalCount("a"));
		for (int i = 0; i < 25; i++)
		{
			store.recordSelection("recent:" + i, "");
		}
		store.recordSelection("recent:10", "");
		assertEquals(20, store.recentDestinationIds(allRecentIds()).size());
		assertEquals("recent:10", store.recentDestinationIds(allRecentIds()).get(0));
		assertEquals(1, store.recentDestinationIds(allRecentIds()).stream()
			.filter("recent:10"::equals).count());
	}

	@Test
	public void corruptUnknownVersionAndNegativeCountsLoadAsEmptyWithoutOverwriting()
	{
		for (String raw : List.of("{broken", "{\"version\":2,\"lastDestinationId\":null,\"globalCounts\":{},\"queries\":[],\"recentIds\":[]}",
			"{\"version\":1,\"lastDestinationId\":null,\"globalCounts\":{\"a\":-1},\"queries\":[],\"recentIds\":[]}",
			"{\"version\":1}"))
		{
			config.put("learnedSearchHistory", raw);
			LearnedHistoryStore store = store();
			assertNull(store.lastDestinationId(Set.of("a")));
			assertEquals(0, store.countsFor("q").globalCount("a"));
			assertEquals(raw, config.get("learnedSearchHistory"));
		}
	}

	@Test
	public void staleIdsAreHarmlessAndClearOnlyRemovesHistory()
	{
		LearnedHistoryStore store = store();
		store.recordSelection("custom:deleted", "search");
		store.recordSelection("live", "search");
		assertEquals("live", store.lastDestinationId(Set.of("live")));
		assertEquals(List.of("live"), store.recentDestinationIds(Set.of("live")));
		assertEquals(1, store.countsFor("search").queryCount("custom:deleted"));
		config.put("favourites", "keep");
		config.put("customLocations", "keep");
		store.clear();
		assertFalse(config.containsKey("learnedSearchHistory"));
		assertEquals("keep", config.get("favourites"));
		assertEquals("keep", config.get("customLocations"));
		assertNull(store.lastDestinationId(Set.of("live")));
		assertEquals(0, store.countsFor("search").queryCount("live"));
	}

	@Test
	public void oversizedQueryStillUpdatesSelectionWithoutGrowingQueryHistory()
	{
		LearnedHistoryStore store = store();
		String query = "verylong".repeat(30);
		store.recordSelection("id", query);
		assertEquals(1, store.countsFor(query).globalCount("id"));
		assertEquals(0, store.countsFor(query).queryCount("id"));
		assertEquals("id", store.lastDestinationId(Set.of("id")));
	}

	private LearnedHistoryStore store()
	{
		return new LearnedHistoryStore(() -> config.get("learnedSearchHistory"),
			value -> config.put("learnedSearchHistory", value),
			() -> config.remove("learnedSearchHistory"), gson);
	}

	private static Set<String> allRecentIds()
	{
		java.util.Set<String> ids = new java.util.HashSet<>();
		for (int i = 0; i < 25; i++)
		{
			ids.add("recent:" + i);
		}
		return ids;
	}
}
