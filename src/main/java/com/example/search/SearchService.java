package com.example.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SearchService
{
	private static final Comparator<SearchResult> RESULT_ORDER = Comparator
		.comparing(SearchResult::getTier)
		.thenComparing(result -> result.getCandidate().getNormalizedName())
		.thenComparing(SearchResult::getId);

	/** Lowercase, replace punctuation with spaces, and collapse whitespace. */
	public static String normalize(String text)
	{
		return text == null ? "" : text.toLowerCase(Locale.ROOT)
			.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
	}

	public List<SearchResult> search(String query, Collection<SearchCandidate> candidates)
	{
		Objects.requireNonNull(candidates, "candidates");
		String normalizedQuery = normalize(query);
		if (normalizedQuery.isEmpty())
		{
			return List.of();
		}

		List<SearchResult> results = new ArrayList<>();
		for (SearchCandidate candidate : candidates)
		{
			MatchTier tier = match(normalizedQuery, candidate.getNormalizedName());
			if (tier != null)
			{
				results.add(new SearchResult(candidate, tier));
			}
		}
		results.sort(RESULT_ORDER);
		return results;
	}

	private static MatchTier match(String query, String name)
	{
		if (name.equals(query))
		{
			return MatchTier.EXACT;
		}
		if (name.startsWith(query))
		{
			return MatchTier.PREFIX;
		}
		if (tokensMatchInOrder(query, name))
		{
			return MatchTier.TOKEN;
		}
		return name.contains(query) ? MatchTier.SUBSTRING : null;
	}

	private static boolean tokensMatchInOrder(String query, String name)
	{
		String[] queryTokens = query.split(" ");
		if (queryTokens.length < 2)
		{
			return false;
		}
		for (String token : queryTokens)
		{
			if (token.length() < 2)
			{
				return false;
			}
		}

		String[] nameTokens = name.split(" ");
		int queryIndex = 0;
		for (String token : nameTokens)
		{
			if (token.equals(queryTokens[queryIndex]))
			{
				queryIndex++;
				if (queryIndex == queryTokens.length)
				{
					return true;
				}
			}
		}
		return false;
	}
}
