package com.example.search;

import com.example.history.LearnedHistoryStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SearchService
{
	private final SearchMetadata metadata;
	private final LearnedHistoryStore historyStore;

	public SearchService()
	{
		this(new SearchMetadata(java.util.Map.of()));
	}

	public SearchService(SearchMetadata metadata)
	{
		this(metadata, null);
	}

	public SearchService(SearchMetadata metadata, LearnedHistoryStore historyStore)
	{
		this.metadata = Objects.requireNonNull(metadata, "metadata");
		this.historyStore = historyStore;
	}

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
			MatchTier tier = match(normalizedQuery, candidate);
			if (tier != null)
			{
				results.add(new SearchResult(candidate, tier));
			}
		}
		if (historyStore == null)
		{
			results.sort(RESULT_ORDER);
		}
		else
		{
			LearnedHistoryStore.RankingCounts counts = historyStore.countsFor(normalizedQuery);
			results.sort((left, right) ->
			{
				int tier = left.getTier().compareTo(right.getTier());
				if (tier != 0)
				{
					return tier;
				}
				int queryCount = Integer.compare(counts.queryCount(right.getId()), counts.queryCount(left.getId()));
				if (queryCount != 0)
				{
					return queryCount;
				}
				int globalCount = Integer.compare(counts.globalCount(right.getId()), counts.globalCount(left.getId()));
				if (globalCount != 0)
				{
					return globalCount;
				}
				return RESULT_ORDER.compare(left, right);
			});
		}
		return results;
	}

	private MatchTier match(String query, SearchCandidate candidate)
	{
		String name = candidate.getNormalizedName();
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
		List<String> aliases = metadata.aliasesFor(candidate.getId());
		for (String alias : aliases)
		{
			if (alias.equals(query))
			{
				return MatchTier.ALIAS;
			}
		}
		if (name.contains(query))
		{
			return MatchTier.SUBSTRING;
		}
		if (query.length() >= 3)
		{
			for (String alias : aliases)
			{
				if (alias.startsWith(query) || tokensMatchInOrder(query, alias) || alias.contains(query))
				{
					return MatchTier.ALIAS_PARTIAL;
				}
			}
		}
		if (categoryMatches(query, candidate.getNormalizedCategory()))
		{
			return MatchTier.CATEGORY;
		}
		return fuzzyMatches(query, name) ? MatchTier.FUZZY : null;
	}

	private static boolean categoryMatches(String query, String category)
	{
		if (query.length() < 4 || category.isEmpty())
		{
			return false;
		}
		if (category.equals(query))
		{
			return true;
		}
		if (query.indexOf(' ') >= 0)
		{
			return false;
		}
		for (String token : category.split(" "))
		{
			if (token.equals(query))
			{
				return true;
			}
		}
		return false;
	}

	/** Match name tokens in order, allowing one misspelled token per query. */
	private static boolean fuzzyMatches(String query, String name)
	{
		String[] queryTokens = query.split(" ");
		String[] nameTokens = name.split(" ");
		for (String queryToken : queryTokens)
		{
			if (queryToken.length() < 3)
			{
				return false;
			}
		}

		int queryIndex = 0;
		boolean usedTypo = false;
		for (String nameToken : nameTokens)
		{
			if (queryIndex == queryTokens.length)
			{
				break;
			}
			String queryToken = queryTokens[queryIndex];
			if (nameToken.equals(queryToken))
			{
				queryIndex++;
			}
			else if (!usedTypo && nameToken.charAt(0) == queryToken.charAt(0)
				&& editDistanceAtMost(queryToken, nameToken, queryToken.length() <= 5 ? 1 : 2))
			{
				usedTypo = true;
				queryIndex++;
			}
		}
		return usedTypo && queryIndex == queryTokens.length;
	}

	/** Bounded Levenshtein distance: short tokens allow one edit, long tokens two. */
	private static boolean editDistanceAtMost(String left, String right, int limit)
	{
		if (Math.abs(left.length() - right.length()) > limit)
		{
			return false;
		}
		int[] previous = new int[right.length() + 1];
		int[] current = new int[right.length() + 1];
		for (int j = 0; j <= right.length(); j++)
		{
			previous[j] = j;
		}
		for (int i = 1; i <= left.length(); i++)
		{
			current[0] = i;
			int rowMinimum = current[0];
			for (int j = 1; j <= right.length(); j++)
			{
				int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), previous[j - 1] + cost);
				rowMinimum = Math.min(rowMinimum, current[j]);
			}
			if (rowMinimum > limit)
			{
				return false;
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[right.length()] <= limit;
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
