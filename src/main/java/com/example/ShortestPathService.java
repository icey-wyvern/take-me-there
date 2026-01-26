package com.example;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

@Slf4j
public class ShortestPathService
{
	private static final String TARGET_PLUGIN_SIMPLE_NAME = "ShortestPathPlugin";

	private final PluginManager pluginManager;
	private final ClientThread clientThread;

	@Inject
	public ShortestPathService(PluginManager pluginManager, ClientThread clientThread)
	{
		this.pluginManager = pluginManager;
		this.clientThread = clientThread;
	}

	public void setDestination(WorldPoint destination)
	{
		if (destination == null)
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			Plugin shortestPathPlugin = findShortestPathPlugin(pluginManager.getPlugins());
			if (shortestPathPlugin == null)
			{
				log.warn("Shortest Path plugin not found (is it installed/enabled?)");
				return;
			}

			if (!tryInvokeTargetSetter(shortestPathPlugin, destination))
			{
				log.warn("Found Shortest Path plugin, but could not set destination via reflection");
			}
		});
	}

	private static Plugin findShortestPathPlugin(Collection<Plugin> plugins)
	{
		for (Plugin plugin : plugins)
		{
			Class<?> clazz = plugin.getClass();
			if (TARGET_PLUGIN_SIMPLE_NAME.equals(clazz.getSimpleName()))
			{
				return plugin;
			}

			String fqcn = clazz.getName().toLowerCase();
			if (fqcn.contains("shortest") && fqcn.contains("path"))
			{
				return plugin;
			}
		}
		return null;
	}

	private static boolean tryInvokeTargetSetter(Object plugin, WorldPoint destination)
	{
		Class<?> clazz = plugin.getClass();

		// Common method names seen across plugins
		String[] candidateNames = {"setTarget", "setDestination", "setDest", "setGoal"};

		for (String name : candidateNames)
		{
			if (tryInvokeByName(clazz, plugin, name, destination))
			{
				return true;
			}
		}

		// Fallback: scan any public method that can accept a WorldPoint as first param
		for (Method m : clazz.getMethods())
		{
			if (tryInvokeMethod(plugin, m, destination))
			{
				return true;
			}
		}

		return false;
	}

	private static boolean tryInvokeByName(Class<?> clazz, Object plugin, String methodName, WorldPoint destination)
	{
		for (Method m : clazz.getMethods())
		{
			if (!m.getName().equals(methodName))
			{
				continue;
			}

			if (tryInvokeMethod(plugin, m, destination))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean tryInvokeMethod(Object plugin, Method method, WorldPoint destination)
	{
		Class<?>[] params = method.getParameterTypes();
		if (params.length == 1 && params[0].isAssignableFrom(WorldPoint.class))
		{
			return invoke(plugin, method, destination);
		}

		if (params.length == 2
			&& params[0].isAssignableFrom(WorldPoint.class)
			&& (params[1] == boolean.class || params[1] == Boolean.class))
		{
			// second param often means "snap to tile"/"persist"/"recalculate" - true is typically safe
			return invoke(plugin, method, destination, true);
		}

		return false;
	}

	private static boolean invoke(Object target, Method method, Object... args)
	{
		try
		{
			method.setAccessible(true);
			method.invoke(target, args);
			log.debug("Invoked {}.{}({})", target.getClass().getName(), method.getName(), args.length);
			return true;
		}
		catch (IllegalAccessException | InvocationTargetException e)
		{
			log.debug("Failed invoking {}.{}: {}", target.getClass().getName(), method.getName(), e.toString());
			return false;
		}
	}
}
