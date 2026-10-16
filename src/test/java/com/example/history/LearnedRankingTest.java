package com.example.history;

import com.example.data.LocationCatalogLoader;
import com.example.model.LocationCatalog;
import com.example.model.LocationDefinition;
import com.example.search.MatchTier;
import com.example.search.SearchCandidate;
import com.example.search.SearchMetadata;
import com.example.search.SearchResult;
import com.example.search.SearchService;
import com.google.gson.Gson;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class LearnedRankingTest
{
	private final Map<String, String> config = new HashMap<>();
	private final LearnedHistoryStore history = new LearnedHistoryStore(
		() -> config.get("history"), value -> config.put("history", value), () -> config.remove("history"), new Gson());

	@Test
	public void querySpecificSelectionReordersOnlyItsSameTierPeers()
	{
		SearchService search = new SearchService(new SearchMetadata(Map.of()), history);
		List<SearchCandidate> candidates = List.of(
			new SearchCandidate("a", "Magic Shop Alpha"),
			new SearchCandidate("custom:b", "Magic Shop Beta")
		);
		assertEquals(List.of("a", "custom:b"), ids(search.search("magic shop", candidates)));
		history.recordSelection("custom:b", "  MAGIC,  SHOP! ");
		assertEquals(List.of("custom:b", "a"), ids(search.search("magic shop", candidates)));
		assertEquals(List.of("custom:b", "a"), ids(search.search("magic", candidates))); // weaker global tie-break
		history.recordSelection("a", "magic");
		assertEquals(List.of("a", "custom:b"), ids(search.search("magic", candidates)));
		assertEquals(1, history.countsFor("magic shop").queryCount("custom:b"));
	}

	@Test
	public void selectingRunicEmporiumChangesMagicShopTopResultWithinTokenTier()
	{
		Gson gson = new Gson();
		LocationCatalog catalog = new LocationCatalogLoader(gson).load();
		String talId = "varlamore-shops:tal-teklan-rune-shop-magic-shop";
		String runicId = "varlamore-shops:the-runic-emporium-cam-torum-magic-shop";
		LocationDefinition tal = catalog.getLocationsById().get(talId);
		LocationDefinition runic = catalog.getLocationsById().get(runicId);
		SearchService search = new SearchService(SearchMetadata.load(gson, catalog), history);
		List<SearchCandidate> candidates = List.of(
			new SearchCandidate(tal.getId(), tal.getName()),
			new SearchCandidate(runic.getId(), runic.getName())
		);
		assertEquals(List.of(talId, runicId), ids(search.search("magic shop", candidates)));
		history.recordSelection(runicId, " MAGIC   SHOP ");
		List<SearchResult> learned = search.search("magic shop", candidates);
		assertEquals(List.of(runicId, talId), ids(learned));
		assertEquals(MatchTier.TOKEN, learned.get(0).getTier());
	}

	@Test
	public void queryCountBeatsMuchLargerGlobalCountWithinTier()
	{
		SearchService search = new SearchService(new SearchMetadata(Map.of()), history);
		List<SearchCandidate> candidates = List.of(
			new SearchCandidate("a", "Magic Shop Alpha"),
			new SearchCandidate("b", "Magic Shop Beta")
		);
		for (int i = 0; i < 50; i++)
		{
			history.recordSelection("b", "unrelated");
		}
		history.recordSelection("a", "magic shop");
		assertEquals(List.of("a", "b"), ids(search.search("magic shop", candidates)));
		assertEquals(List.of("b", "a"), ids(search.search("magic", candidates)));
	}

	@Test
	public void historyNeverPromotesAliasFuzzyOrCategoryAcrossTiers()
	{
		SearchService search = new SearchService(new SearchMetadata(Map.of("alias", List.of("cox"))), history);
		for (int i = 0; i < 30; i++)
		{
			history.recordSelection("alias", "cox");
			history.recordSelection("fuzzy", "chambers");
			history.recordSelection("category", "magic shop");
		}
		List<SearchResult> cox = search.search("cox", List.of(
			new SearchCandidate("alias", "Chambers of Xeric"), new SearchCandidate("exact", "Cox")));
		assertEquals(List.of("exact", "alias"), ids(cox));
		assertEquals(MatchTier.EXACT, cox.get(0).getTier());

		List<SearchResult> chambers = search.search("chambers", List.of(
			new SearchCandidate("fuzzy", "Chambars"), new SearchCandidate("prefix", "Chambers of Xeric")));
		assertEquals(List.of("prefix", "fuzzy"), ids(chambers));
		assertEquals(MatchTier.FUZZY, chambers.get(1).getTier());

		List<SearchResult> shop = search.search("magic shop", List.of(
			new SearchCandidate("category", "Runic Emporium", "Magic Shop"),
			new SearchCandidate("token", "Magic Guild Shop")));
		assertEquals(List.of("token", "category"), ids(shop));
		assertEquals(MatchTier.CATEGORY, shop.get(1).getTier());
	}

	@Test
	public void emptyHistoryKeepsBaselineOrderAndStaleIdsDoNotAffectResults()
	{
		List<SearchCandidate> candidates = List.of(new SearchCandidate("b", "Chambers of Xeric"),
			new SearchCandidate("a", "Chambers of Xeric"));
		SearchMetadata metadata = new SearchMetadata(Map.of());
		assertEquals(ids(new SearchService(metadata).search("chambers", candidates)),
			ids(new SearchService(metadata, history).search("chambers", candidates)));
		history.recordSelection("removed:id", "chambers");
		assertEquals(List.of("a", "b"), ids(new SearchService(metadata, history).search("chambers", candidates)));
	}

	private static List<String> ids(List<SearchResult> results)
	{
		return results.stream().map(SearchResult::getId).collect(Collectors.toList());
	}
}
