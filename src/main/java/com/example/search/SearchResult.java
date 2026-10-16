package com.example.search;

import java.util.Objects;

public final class SearchResult
{
	private final SearchCandidate candidate;
	private final MatchTier tier;

	public SearchResult(SearchCandidate candidate, MatchTier tier)
	{
		this.candidate = Objects.requireNonNull(candidate, "candidate");
		this.tier = Objects.requireNonNull(tier, "tier");
	}

	public SearchCandidate getCandidate()
	{
		return candidate;
	}

	public String getId()
	{
		return candidate.getId();
	}

	public MatchTier getTier()
	{
		return tier;
	}
}
