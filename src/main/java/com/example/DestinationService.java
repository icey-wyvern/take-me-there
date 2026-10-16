package com.example;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DestinationService
{
	private static final Logger log = LoggerFactory.getLogger(DestinationService.class);

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;

	@Inject
	public DestinationService(Client client, ClientThread clientThread, EventBus eventBus)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
	}

	public void setDestination(WorldPoint target)
	{
		setDestination(target, () -> {});
	}

	/** Calls onDispatched only after the Shortest Path path message was posted. */
	public void setDestination(WorldPoint target, Runnable onDispatched)
	{
		Objects.requireNonNull(onDispatched, "onDispatched");
		if (target == null)
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			Player localPlayer = client.getLocalPlayer();
			if (localPlayer == null)
			{
				log.debug("Ignoring destination set because local player is null. target={}", target);
				return;
			}

			Map<String, Object> data = new HashMap<>();
			data.put("start", localPlayer.getWorldLocation());
			data.put("target", target);

			log.debug("Posting Shortest Path destination. start={} target={}", localPlayer.getWorldLocation(), target);
			eventBus.post(new PluginMessage("shortestpath", "path", data));
			onDispatched.run();
		});
	}

	public void clear()
	{
		eventBus.post(new PluginMessage("shortestpath", "clear"));
	}
}
