package com.example.search;

/** Earlier tiers have stronger canonical-name relevance. */
public enum MatchTier
{
	EXACT,
	PREFIX,
	TOKEN,
	SUBSTRING
}
