package com.example.search;

/** Earlier tiers have stronger textual relevance; fuzzy matches are always last. */
public enum MatchTier
{
	EXACT,
	PREFIX,
	TOKEN,
	ALIAS,
	SUBSTRING,
	ALIAS_PARTIAL,
	CATEGORY,
	FUZZY
}
