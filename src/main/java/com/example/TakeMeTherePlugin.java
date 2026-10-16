package com.example;

import com.example.data.LocationCatalogLoader;
import com.example.custom.CustomLocationsStore;
import com.example.favourites.FavouritesStore;
import com.example.history.LearnedHistoryStore;
import com.example.model.LocationCatalog;
import com.example.onboarding.PaletteTipState;
import com.example.palette.DestinationPaletteDialog;
import com.example.palette.PaletteHotkeyController;
import com.example.search.SearchMetadata;
import com.example.search.SearchService;
import com.example.ui.TakeMeTherePanel;
import com.google.gson.Gson;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

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

	@Inject
	private Client client;

	@Inject
	private KeyManager keyManager;

	@Inject
	private TakeMeThereConfig config;

	private NavigationButton navigationButton;
	private DestinationPaletteDialog palette;
	private PaletteHotkeyController paletteHotkeys;
	private volatile PaletteTipState paletteTipState;
	private TakeMeTherePanel panel;
	private final AtomicInteger lifecycleGeneration = new AtomicInteger();

	@Provides
	TakeMeThereConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(TakeMeThereConfig.class);
	}

	@Override
	protected void startUp()
	{
		int generation = lifecycleGeneration.incrementAndGet();
		SwingUtilities.invokeLater(() ->
		{
			if (generation != lifecycleGeneration.get())
			{
				return;
			}
			disposeUi();
			FavouritesStore favouritesStore = new FavouritesStore(configManager, gson);
			CustomLocationsStore customLocationsStore = new CustomLocationsStore(configManager, gson);

			LocationCatalog catalog = new LocationCatalogLoader(gson).load();
			LearnedHistoryStore historyStore = new LearnedHistoryStore(configManager, gson);
			SearchService searchService = new SearchService(SearchMetadata.load(gson, catalog), historyStore);
			DestinationSelectionService selectionService = new DestinationSelectionService(destinationService, historyStore);
			PaletteTipState tipState = new PaletteTipState(configManager);
			TakeMeTherePanel newPanel = new TakeMeTherePanel(
				catalog,
				favouritesStore,
				customLocationsStore,
				searchService,
				selectionService
			);
			newPanel.setPaletteTipVisible(tipState.reconcile(config.paletteHotkey()));
			paletteTipState = tipState;
			panel = newPanel;
			Window owner = SwingUtilities.getWindowAncestor((Component) client);
			if (owner == null)
			{
				throw new IllegalStateException("RuneLite client window is unavailable");
			}
			palette = new DestinationPaletteDialog(owner, catalog, customLocationsStore,
				searchService, historyStore, selectionService);
			KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			paletteHotkeys = new PaletteHotkeyController(config::paletteHotkey, palette,
				usedHotkey ->
				{
					tipState.openedWith(usedHotkey);
					newPanel.setPaletteTipVisible(tipState.shouldShow(config.paletteHotkey()));
				},
				keyManager::registerKeyListener, keyManager::unregisterKeyListener,
				focusManager::addKeyEventDispatcher, focusManager::removeKeyEventDispatcher);

			BufferedImage icon = ImageUtil.loadImageResource(TakeMeTherePlugin.class, "/panel_icon.png");

			navigationButton = NavigationButton.builder()
				.tooltip("Take Me There")
				.icon(icon)
				.panel(newPanel)
				.build();

			clientToolbar.addNavigation(navigationButton);
			paletteHotkeys.start();
		});
	}

	@Override
	protected void shutDown()
	{
		lifecycleGeneration.incrementAndGet();
		SwingUtilities.invokeLater(this::disposeUi);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("takemethere".equals(event.getGroup()) && "paletteHotkey".equals(event.getKey()))
		{
			PaletteTipState tipState = paletteTipState;
			if (tipState != null)
			{
				// ConfigManager posts this event after updating the config proxy.
				tipState.reconcile(config.paletteHotkey());
			}
			SwingUtilities.invokeLater(() ->
			{
				if (paletteHotkeys != null)
				{
					paletteHotkeys.configurationChanged();
				}
				if (tipState != null && tipState == paletteTipState && panel != null)
				{
					panel.setPaletteTipVisible(tipState.shouldShow(config.paletteHotkey()));
				}
			});
		}
	}

	private void disposeUi()
	{
		if (paletteHotkeys != null)
		{
			paletteHotkeys.stop();
			paletteHotkeys = null;
			palette = null;
		}
		else if (palette != null)
		{
			palette.disposePalette();
			palette = null;
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		panel = null;
		paletteTipState = null;
	}
}
