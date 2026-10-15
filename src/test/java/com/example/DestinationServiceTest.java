package com.example;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DestinationServiceTest
{
	@Test
	public void postsShortestPathMessageAfterReadingPlayerOnClientThread()
	{
		WorldPoint start = new WorldPoint(3221, 3218, 0);
		WorldPoint target = new WorldPoint(3165, 3490, 0);
		int[] playerReads = {0};
		int[] locationReads = {0};
		Player player = proxy(Player.class, (proxy, method, args) ->
		{
			if (method.getName().equals("getWorldLocation"))
			{
				locationReads[0]++;
				return start;
			}
			throw new AssertionError("Unexpected Player call: " + method.getName());
		});
		Client client = proxy(Client.class, (proxy, method, args) ->
		{
			if (method.getName().equals("getLocalPlayer"))
			{
				playerReads[0]++;
				return player;
			}
			throw new AssertionError("Unexpected Client call: " + method.getName());
		});
		RecordingClientThread clientThread = new RecordingClientThread();
		RecordingEventBus eventBus = new RecordingEventBus();
		new DestinationService(client, clientThread, eventBus).setDestination(target);

		assertEquals(0, playerReads[0]);
		assertEquals(0, locationReads[0]);
		assertTrue(eventBus.events.isEmpty());
		assertNotNull("Client thread was not scheduled", clientThread.pending);

		clientThread.runPending();
		assertEquals(1, playerReads[0]);
		assertTrue(locationReads[0] > 0);
		assertEquals(1, eventBus.events.size());
		PluginMessage message = (PluginMessage) eventBus.events.get(0);
		assertEquals("shortestpath", message.getNamespace());
		assertEquals("path", message.getName());
		assertEquals(Map.of("start", start, "target", target), message.getData());
	}

	@Test
	public void nullPlayerDoesNotPostDestination()
	{
		Client client = proxy(Client.class, (proxy, method, args) ->
		{
			if (method.getName().equals("getLocalPlayer"))
			{
				return null;
			}
			throw new AssertionError("Unexpected Client call: " + method.getName());
		});
		RecordingClientThread clientThread = new RecordingClientThread();
		RecordingEventBus eventBus = new RecordingEventBus();
		new DestinationService(client, clientThread, eventBus).setDestination(new WorldPoint(3165, 3490, 0));
		clientThread.runPending();
		assertTrue(eventBus.events.isEmpty());
	}

	@Test
	public void nullTargetDoesNotScheduleClientThread()
	{
		RecordingClientThread clientThread = new RecordingClientThread();
		RecordingEventBus eventBus = new RecordingEventBus();
		new DestinationService(null, clientThread, eventBus).setDestination(null);
		assertNull(clientThread.pending);
		assertTrue(eventBus.events.isEmpty());
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
			assertNull("Unexpected second client-thread task", pending);
			pending = runnable;
		}

		private void runPending()
		{
			assertNotNull("No client-thread task", pending);
			Runnable runnable = pending;
			pending = null;
			runnable.run();
		}
	}

	private static final class RecordingEventBus extends EventBus
	{
		private final List<Object> events = new ArrayList<>();

		@Override
		public void post(Object event)
		{
			events.add(event);
		}
	}
}
