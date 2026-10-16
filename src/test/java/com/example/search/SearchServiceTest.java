package com.example.search;

import com.example.custom.CustomLocation;
import com.example.model.LocationDefinition;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SearchServiceTest
{
	private final SearchService search = new SearchService();

	@Test
	public void normalizesCaseWhitespaceAndPunctuation()
	{
		assertEquals("chambers of xeric", SearchService.normalize("  CHAMBERS   of\tXeric! "));
		assertEquals("seers village", SearchService.normalize("Seers' Village"));
		assertEquals("", SearchService.normalize(" -- "));
		assertEquals("", SearchService.normalize(null));
	}

	@Test
	public void ranksCanonicalTiersBeforeNameAndId()
	{
		List<SearchCandidate> candidates = List.of(
			new SearchCandidate("substring", "Old Prechambers Xeric Room"),
			new SearchCandidate("token", "Chambers of Xeric"),
			new SearchCandidate("prefix", "Chambers Xeric Lobby"),
			new SearchCandidate("exact", "Chambers Xeric")
		);

		List<SearchResult> results = search.search("chambers xeric", candidates);
		assertEquals(List.of("exact", "prefix", "token", "substring"), ids(results));
		assertEquals(List.of(MatchTier.EXACT, MatchTier.PREFIX, MatchTier.TOKEN, MatchTier.SUBSTRING),
			results.stream().map(SearchResult::getTier).collect(Collectors.toList()));
	}

	@Test
	public void substringStillMatchesInsideAName()
	{
		SearchResult result = search.search("ambers", List.of(new SearchCandidate("cox", "Chambers of Xeric"))).get(0);
		assertEquals(MatchTier.SUBSTRING, result.getTier());
	}

	@Test
	public void tokenMatchRequiresCompleteOrderedTokens()
	{
		SearchCandidate candidate = new SearchCandidate("cox", "Chambers of Xeric");
		assertEquals(MatchTier.TOKEN, search.search("chambers xeric", List.of(candidate)).get(0).getTier());
		assertTrue(search.search("ch x", List.of(candidate)).isEmpty());
		assertTrue(search.search("xeric chambers", List.of(candidate)).isEmpty());
	}

	@Test
	public void tiesUseNormalizedNameThenStableIdRegardlessOfInputOrder()
	{
		List<SearchCandidate> candidates = List.of(
			new SearchCandidate("z", "Farming Guild"),
			new SearchCandidate("b", "Farming Guild"),
			new SearchCandidate("a", "Farming Gate")
		);
		assertEquals(List.of("a", "b", "z"), ids(search.search("farming", candidates)));
		assertEquals(List.of("a", "b", "z"), ids(search.search("farming", List.of(candidates.get(2), candidates.get(0), candidates.get(1)))));
	}

	@Test
	public void emptyAndUnrelatedQueriesHaveNoRankedResults()
	{
		List<SearchCandidate> candidates = List.of(new SearchCandidate("id", "Chambers of Xeric"));
		assertTrue(search.search("  ", candidates).isEmpty());
		assertTrue(search.search("unrelated", candidates).isEmpty());
	}

	@Test
	public void builtInAndCustomDestinationsUseTheSameNameMatcher()
	{
		LocationDefinition builtIn = new LocationDefinition("dungeons:chambers-of-xeric", "Chambers of Xeric", 1, 2, 0);
		CustomLocation custom = new CustomLocation("custom:one", "Chambers of Xeric", "custom", 3, 4, 0);
		List<SearchResult> results = search.search("Chambers of Xeric", List.of(
			new SearchCandidate(custom.getId(), custom.getName()),
			new SearchCandidate(builtIn.getId(), builtIn.getName())
		));
		assertEquals(List.of("custom:one", "dungeons:chambers-of-xeric"), ids(results));
		assertEquals(MatchTier.EXACT, results.get(0).getTier());
		assertEquals(MatchTier.EXACT, results.get(1).getTier());
	}

	private static List<String> ids(List<SearchResult> results)
	{
		return results.stream().map(SearchResult::getId).collect(Collectors.toList());
	}
}
