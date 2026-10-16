package com.example.search;

import com.example.data.LocationCatalogLoader;
import com.example.model.CategoryDefinition;
import com.example.model.LocationCatalog;
import com.example.model.LocationDefinition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SearchVocabularyTest
{
	private static final Gson GSON = new Gson();
	private final LocationCatalog catalog = new LocationCatalogLoader(GSON).load();
	private final SearchMetadata metadata = SearchMetadata.load(GSON, catalog);
	private final SearchService search = new SearchService(metadata);
	private final List<SearchCandidate> candidates = candidatesFor(catalog);

	@Test
	public void curatedAliasesReferenceExistingIdsAndSharedAliasesAreIntentional()
	{
		assertTrue(catalog.getLocationsById().keySet().containsAll(metadata.destinationIds()));
		assertEquals(Set.of("dungeons:chambers-of-xeric", "zeah-general-pois:chambers-of-xeric"),
			idsAtTier("cox", MatchTier.ALIAS));
		assertEquals(4, idsAtTier("red chins", MatchTier.ALIAS).size());
		assertEquals(2, idsAtTier("sawmill", MatchTier.ALIAS).size());
		assertEquals(2, metadata.destinationIds().stream()
			.filter(id -> metadata.aliasesFor(id).contains("rune shop")).count());
		assertEquals(2, metadata.destinationIds().stream()
			.filter(id -> metadata.aliasesFor(id).contains("magic shop")).count());
	}

	@Test
	public void rejectsDuplicateAliasForOneDestination()
	{
		try
		{
			new SearchMetadata(Map.of("id", List.of("CoX", "cox!")));
			fail("Expected duplicate normalized alias to be rejected");
		}
		catch (IllegalArgumentException expected)
		{
			assertTrue(expected.getMessage().contains("duplicate alias"));
		}
	}

	@Test
	public void canonicalExactBeatsAnotherDestinationsExactAlias()
	{
		SearchService service = new SearchService(new SearchMetadata(Map.of("alias", List.of("cox"))));
		List<SearchResult> results = service.search("cox", List.of(
			new SearchCandidate("alias", "Chambers of Xeric"),
			new SearchCandidate("canonical", "Cox")
		));
		assertEquals("canonical", results.get(0).getId());
		assertEquals(MatchTier.EXACT, results.get(0).getTier());
		assertEquals(MatchTier.ALIAS, results.get(1).getTier());
	}

	@Test
	public void fuzzyNeverBeatsAliasOrCanonicalText()
	{
		SearchService service = new SearchService(new SearchMetadata(Map.of("alias", List.of("chambars"))));
		List<SearchResult> results = service.search("chambars", List.of(
			new SearchCandidate("fuzzy", "Chambers"),
			new SearchCandidate("alias", "Xeric"),
			new SearchCandidate("canonical", "Chambars")
		));
		assertEquals(List.of("canonical", "alias", "fuzzy"), ids(results));
		assertEquals(MatchTier.FUZZY, results.get(2).getTier());
	}

	@Test
	public void shortQueriesDoNotFuzzilyMatchAliasesOrNames()
	{
		SearchCandidate candidate = new SearchCandidate("cox", "Cox");
		assertTrue(search.search("c", List.of(candidate)).stream().noneMatch(r -> r.getTier() == MatchTier.FUZZY));
		assertTrue(search.search("co", List.of(candidate)).stream().noneMatch(r -> r.getTier() == MatchTier.FUZZY));
		assertTrue(search.search("box", List.of(candidate)).isEmpty()); // different first character
	}

	@Test
	public void categoryVocabularyIsWeakerThanCanonicalAndAliases()
	{
		SearchService service = new SearchService(new SearchMetadata(Map.of("alias", List.of("shops"))));
		List<SearchResult> results = service.search("shops", List.of(
			new SearchCandidate("category", "Aleck's Hunter Emporium", "Kandarin Shops"),
			new SearchCandidate("alias", "Runic Emporium"),
			new SearchCandidate("canonical", "Shops")
		));
		assertEquals(List.of("canonical", "alias", "category"), ids(results));
		assertEquals(MatchTier.CATEGORY, results.get(2).getTier());
	}

	@Test
	public void representativeSearchCorpusIsStable() throws Exception
	{
		for (RegressionCase regression : loadRegressionCases())
		{
			List<SearchResult> first = search.search(regression.query, candidates);
			List<SearchResult> second = search.search(regression.query, candidates);
			assertEquals(regression.query, ids(first), ids(second));
			assertTrue(regression.query + " has too few results", first.size() >= regression.topIds.size());
			assertEquals(regression.query, regression.topIds, ids(first).subList(0, regression.topIds.size()));
			if (regression.topIds.isEmpty())
			{
				assertTrue(regression.query, first.isEmpty());
			}
			else
			{
				assertEquals(regression.query, MatchTier.valueOf(regression.tier), first.get(0).getTier());
			}
		}
	}

	private Set<String> idsAtTier(String query, MatchTier tier)
	{
		return search.search(query, candidates).stream()
			.filter(result -> result.getTier() == tier)
			.map(SearchResult::getId)
			.collect(Collectors.toSet());
	}

	private static List<SearchCandidate> candidatesFor(LocationCatalog catalog)
	{
		List<SearchCandidate> result = new ArrayList<>();
		for (CategoryDefinition category : catalog.getCategories())
		{
			for (LocationDefinition location : catalog.getLocationsByCategoryId().get(category.getId()))
			{
				result.add(new SearchCandidate(location.getId(), location.getName(), category.getDisplayName()));
			}
		}
		return result;
	}

	private static List<RegressionCase> loadRegressionCases() throws Exception
	{
		InputStream stream = SearchVocabularyTest.class.getResourceAsStream("/search-regressions.json");
		assertNotNull(stream);
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			Type type = new TypeToken<List<RegressionCase>>() {}.getType();
			return GSON.fromJson(reader, type);
		}
	}

	private static List<String> ids(List<SearchResult> results)
	{
		return results.stream().map(SearchResult::getId).collect(Collectors.toList());
	}

	private static final class RegressionCase
	{
		private String query;
		private List<String> topIds;
		private String tier;
	}
}
