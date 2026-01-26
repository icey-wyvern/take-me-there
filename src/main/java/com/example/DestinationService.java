package com.example;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;

import java.util.HashMap;
import java.util.Map;

public final class DestinationService
{
	private final Client client;
	private final EventBus eventBus;

	@Inject
	public DestinationService(Client client, EventBus eventBus)
	{
		this.client = client;
		this.eventBus = eventBus;
	}

	public void setDestination(WorldPoint target)
	{
		if (target == null || client.getLocalPlayer() == null)
		{
			return;
		}

		Map<String, Object> data = new HashMap<>();
		data.put("start", client.getLocalPlayer().getWorldLocation());
		data.put("target", target);

		eventBus.post(new PluginMessage("shortestpath", "path", data));
	}

	public void clear()
	{
		eventBus.post(new PluginMessage("shortestpath", "clear"));
	}
}
