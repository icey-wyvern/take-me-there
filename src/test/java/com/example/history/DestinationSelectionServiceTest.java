package com.example.history;

import com.example.DestinationSelectionService;
import com.example.DestinationService;
import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class DestinationSelectionServiceTest
{
	private final Map<String, String> config = new HashMap<>();
	private final LearnedHistoryStore history = new LearnedHistoryStore(
		() -> config.get("history"), value -> config.put("history", value), () -> config.remove("history"), new Gson());
	private final RecordingClientThread clientThread = new RecordingClientThread();
	private final RecordingEventBus eventBus = new RecordingEventBus();
	private final List<Runnable> pendingHistory = new ArrayList<>();
	private final WorldPoint target = new WorldPoint(3165, 3490, 0);

	@Test
	public void successfulPathPostRecordsExactlyOnceAndNormalizesQuery()
	{
		DestinationSelectionService selection = selection(playerClient());
		selection.select("custom:uuid", target, "  MAGIC,  SHOP! ");
		assertEquals(0, history.countsFor("magic shop").globalCount("custom:uuid"));
		clientThread.runPending();
		assertEquals(1, eventBus.events.size());
		PluginMessage message = (PluginMessage) eventBus.events.get(0);
		assertEquals("shortestpath", message.getNamespace());
		assertEquals("path", message.getName());
		assertEquals(target, message.getData().get("target"));
		assertEquals(1, pendingHistory.size());
		pendingHistory.remove(0).run();
		assertEquals(1, history.countsFor("magic shop").queryCount("custom:uuid"));
		assertEquals(1, history.countsFor("magic shop").globalCount("custom:uuid"));
		assertEquals("custom:uuid", history.lastDestinationId(Set.of("custom:uuid")));
		assertTrue(pendingHistory.isEmpty());
	}

	@Test
	public void emptyQueryRecordsGlobalLastAndRecentButNoQueryEntry()
	{
		selection(playerClient()).select("built-in:id", target, "  ");
		clientThread.runPending();
		pendingHistory.remove(0).run();
		assertEquals(1, history.countsFor("").globalCount("built-in:id"));
		assertEquals(0, history.countsFor("").queryCount("built-in:id"));
		assertEquals(List.of("built-in:id"), history.recentDestinationIds(Set.of("built-in:id")));
	}

	@Test
	public void invalidSelectionAndAbsentPlayerNeverRecord()
	{
		DestinationSelectionService selection = selection(playerClient());
		List<Boolean> completions = new ArrayList<>();
		selection.select("", target, "query", completions::add);
		selection.select("id", null, "query", completions::add);
		assertNull(clientThread.pending);
		selection(nullPlayerClient()).select("id", target, "query", completions::add);
		clientThread.runPending();
		assertTrue(eventBus.events.isEmpty());
		assertEquals(3, pendingHistory.size());
		while (!pendingHistory.isEmpty()) pendingHistory.remove(0).run();
		assertEquals(List.of(false, false, false), completions);
		assertEquals(0, history.countsFor("query").globalCount("id"));
	}

	@Test
	public void failedMessagePostNeverRecords()
	{
		eventBus.failPost = true;
		List<Boolean> completions = new ArrayList<>();
		selection(playerClient()).select("id", target, "query", completions::add);
		try
		{
			clientThread.runPending();
			fail("Expected event bus failure");
		}
		catch (IllegalStateException expected)
		{
			assertEquals("post failed", expected.getMessage());
		}
		assertEquals(1, pendingHistory.size());
		pendingHistory.remove(0).run();
		assertEquals(List.of(false), completions);
		assertEquals(0, history.countsFor("query").globalCount("id"));
	}

	@Test
	public void successfulSelectionCompletesOnceAfterHistoryIsRecorded()
	{
		List<Boolean> completions = new ArrayList<>();
		selection(playerClient()).select("id", target, "search", posted ->
		{
			assertEquals(1, history.countsFor("search").globalCount("id"));
			completions.add(posted);
		});
		clientThread.runPending();
		assertTrue(completions.isEmpty());
		assertEquals(1, pendingHistory.size());
		pendingHistory.remove(0).run();
		assertEquals(List.of(true), completions);
		assertTrue(pendingHistory.isEmpty());
	}

	private DestinationSelectionService selection(Client client)
	{
		return new DestinationSelectionService(new DestinationService(client, clientThread, eventBus),
			history, pendingHistory::add);
	}

	private static Client playerClient()
	{
		WorldPoint start = new WorldPoint(3221, 3218, 0);
		Player player = proxy(Player.class, (instance, method, args) ->
		{
			if (method.getName().equals("getWorldLocation"))
			{
				return start;
			}
			throw new AssertionError(method.getName());
		});
		return proxy(Client.class, (instance, method, args) ->
		{
			if (method.getName().equals("getLocalPlayer"))
			{
				return player;
			}
			throw new AssertionError(method.getName());
		});
	}

	private static Client nullPlayerClient()
	{
		return proxy(Client.class, (instance, method, args) ->
		{
			if (method.getName().equals("getLocalPlayer"))
			{
				return null;
			}
			throw new AssertionError(method.getName());
		});
	}

	private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler)
	{
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
	}

	private static final class RecordingClientThread extends ClientThread
	{
		private Runnable pending;

		@Override
		public void invokeLater(Runnable runnable)
		{
			assertNull(pending);
			pending = runnable;
		}

		private void runPending()
		{
			assertNotNull(pending);
			Runnable task = pending;
			pending = null;
			task.run();
		}
	}

	private static final class RecordingEventBus extends EventBus
	{
		private final List<Object> events = new ArrayList<>();
		private boolean failPost;

		@Override
		public void post(Object event)
		{
			if (failPost)
			{
				throw new IllegalStateException("post failed");
			}
			events.add(event);
		}
	}
}
