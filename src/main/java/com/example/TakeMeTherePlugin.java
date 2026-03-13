package com.example;

import com.example.data.LocationCatalogLoader;
import com.example.custom.CustomLocationsStore;
import com.example.favourites.FavouritesStore;
import com.example.ui.TakeMeTherePanel;
import com.google.gson.Gson;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;

@PluginDescriptor(
	name = "Take Me There"
)
public class TakeMeTherePlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private DestinationService destinationService;

	@Inject
	private Gson gson;

	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		SwingUtilities.invokeLater(() ->
		{
			FavouritesStore favouritesStore = new FavouritesStore(configManager, gson);
			CustomLocationsStore customLocationsStore = new CustomLocationsStore(configManager, gson);

			TakeMeTherePanel panel = new TakeMeTherePanel(
				new LocationCatalogLoader(gson).load(),
				favouritesStore,
				destinationService,
				customLocationsStore
			);

			BufferedImage icon = ImageUtil.loadImageResource(TakeMeTherePlugin.class, "/panel_icon.png");

			navigationButton = NavigationButton.builder()
				.tooltip("Take Me There")
				.icon(icon)
				.panel(panel)
				.build();

			clientToolbar.addNavigation(navigationButton);
		});
	}

	@Override
	protected void shutDown()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (navigationButton != null)
			{
				clientToolbar.removeNavigation(navigationButton);
				navigationButton = null;
			}
		});
	}
}
