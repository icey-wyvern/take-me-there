package com.example.history;

import com.google.gson.Gson;
import java.util.Map;

/** Test-only access to the store's in-memory persistence constructor. */
public final class TestHistoryStore
{
	private TestHistoryStore()
	{
	}

	public static LearnedHistoryStore create(Map<String, String> values)
	{
		return new LearnedHistoryStore(
			() -> values.get("history"), value -> values.put("history", value),
			() -> values.remove("history"), new Gson());
	}
}
