package com.example.ui;

import com.example.DestinationService;
import com.example.custom.CustomLocation;
import com.example.custom.CustomLocationsStore;
import com.example.custom.ui.CustomLocationsManagerDialog;
import com.example.data.LocationCatalogLoader;
import com.example.favourites.FavouritesStore;
import com.example.model.CategoryDefinition;
import com.example.model.LocationCatalog;
import com.example.model.LocationDefinition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TakeMeTherePanel extends PluginPanel
{
	private static final String FAVOURITES_CATEGORY_ID = "favourites";
	private static final String FAVOURITES_DISPLAY_NAME = "Favourites";

	private static final String CUSTOM_CATEGORY_ID = "custom";
	private static final String CUSTOM_CATEGORY_NAME = "Custom Locations";

	private static final String STAR_ON = "\u2605";
	private static final String STAR_OFF = "\u2606";
	private static final int STAR_HIT_WIDTH_PX = 26;
	private static final int STAR_RIGHT_PADDING_PX = 8;

	private final IconTextField searchField = new IconTextField();
	private final JTree tree = new JTree();

	private final JButton manageButton = new JButton("Manage Custom Locations");

	private final Map<String, Boolean> expandedByCategoryId = new LinkedHashMap<>();

	private final LocationCatalog catalog;
	private final FavouritesStore favouritesStore;
	private final DestinationService destinationService;
	private final LinkedHashSet<String> favouriteIds = new LinkedHashSet<>();

	private final CustomLocationsStore customLocationsStore;
	private CustomLocationsManagerDialog customLocationsDialog;

	private final Map<String, CustomLocation> customLocationsById = new LinkedHashMap<>();

	public TakeMeTherePanel()
	{
		this(new LocationCatalogLoader().load(), null, null, null);
	}

	public TakeMeTherePanel(LocationCatalog catalog)
	{
		this(catalog, null, null, null);
	}

	public TakeMeTherePanel(LocationCatalog catalog, FavouritesStore favouritesStore)
	{
		this(catalog, favouritesStore, null, null);
	}

	public TakeMeTherePanel(LocationCatalog catalog, FavouritesStore favouritesStore, DestinationService destinationService)
	{
		this(catalog, favouritesStore, destinationService, null);
	}

	public TakeMeTherePanel(
		LocationCatalog catalog,
		FavouritesStore favouritesStore,
		DestinationService destinationService,
		ConfigManager configManager)
	{
		this.catalog = catalog;
		this.favouritesStore = favouritesStore;
		this.destinationService = destinationService;
		this.customLocationsStore = configManager == null ? null : new CustomLocationsStore(configManager);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		expandedByCategoryId.put(FAVOURITES_CATEGORY_ID, false);
		expandedByCategoryId.put(CUSTOM_CATEGORY_ID, false);

		for (CategoryDefinition c : catalog.getCategories())
		{
			expandedByCategoryId.putIfAbsent(c.getId(), false);
		}

		if (favouritesStore != null)
		{
			favouriteIds.addAll(favouritesStore.load());
		}

		add(buildHeader(), BorderLayout.NORTH);
		add(buildTree(), BorderLayout.CENTER);

		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e) { rebuildTree(); }
			@Override public void removeUpdate(DocumentEvent e) { rebuildTree(); }
			@Override public void changedUpdate(DocumentEvent e) { rebuildTree(); }
		});
		searchField.addClearListener(this::rebuildTree);

		rebuildTree();
	}

	private JComponent buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBorder(new EmptyBorder(10, 10, 10, 10));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Take Me There");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		title.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel subtitle = new JLabel("Click to set a destination");
		subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		subtitle.setFont(FontManager.getRunescapeFont().deriveFont(12f));
		subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);

		configureManageButton();

		header.add(title);
		header.add(Box.createVerticalStrut(4));
		header.add(subtitle);
		header.add(Box.createVerticalStrut(10));
		header.add(manageButton);
		header.add(Box.createVerticalStrut(8));
		header.add(searchField);

		return header;
	}

	private void configureManageButton()
	{
		manageButton.setFocusable(false);
		manageButton.setToolTipText("Add, edit, and delete custom locations");
		manageButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		manageButton.setForeground(Color.WHITE);
		manageButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		manageButton.setEnabled(customLocationsStore != null);

		for (var listener : manageButton.getActionListeners())
		{
			manageButton.removeActionListener(listener);
		}

		manageButton.addActionListener(e -> SwingUtilities.invokeLater(this::showCustomLocationsDialog));
	}

	private void showCustomLocationsDialog()
	{
		if (customLocationsStore == null)
		{
			return;
		}

		if (customLocationsDialog == null)
		{
			Window owner = SwingUtilities.getWindowAncestor(this);
			customLocationsDialog = new CustomLocationsManagerDialog(owner, customLocationsStore);

			customLocationsDialog.addComponentListener(new ComponentAdapter()
			{
				@Override
				public void componentHidden(java.awt.event.ComponentEvent e)
				{
					SwingUtilities.invokeLater(TakeMeTherePanel.this::rebuildTree);
				}
			});
		}

		customLocationsDialog.setVisible(true);
		customLocationsDialog.toFront();
		customLocationsDialog.requestFocus();
	}

	private JComponent buildTree()
	{
		tree.setRootVisible(false);
		tree.setShowsRootHandles(false);
		tree.setRowHeight(22);
		tree.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tree.setCellRenderer(new TakeMeThereTreeCellRenderer());
		tree.setToggleClickCount(0);

		tree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener()
		{
			@Override
			public void treeExpanded(javax.swing.event.TreeExpansionEvent event)
			{
				updateExpansionStateFromEvent(event, true);
			}

			@Override
			public void treeCollapsed(javax.swing.event.TreeExpansionEvent event)
			{
				updateExpansionStateFromEvent(event, false);
			}
		});

		tree.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (path == null)
				{
					return;
				}

				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				Object userObject = node.getUserObject();

				if (!SwingUtilities.isLeftMouseButton(e))
				{
					return;
				}

				if (userObject instanceof CategoryDefinition)
				{
					toggleCategory(path);
					tree.clearSelection();
					return;
				}

				if (userObject instanceof LocationDefinition)
				{
					LocationDefinition location = (LocationDefinition) userObject;

					if (isStarClick(path, e))
					{
						toggleFavourite(location.getId());
						tree.clearSelection();
						return;
					}

					setDestination(location.getX(), location.getY(), location.getPlane());
					tree.clearSelection();
					return;
				}

				if (userObject instanceof CustomLocation)
				{
					CustomLocation location = (CustomLocation) userObject;

					if (isStarClick(path, e))
					{
						toggleFavourite(location.getId());
						tree.clearSelection();
						return;
					}

					setDestination(location.getX(), location.getY(), location.getPlane());
					tree.clearSelection();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(tree);
		scrollPane.setBorder(null);
		return scrollPane;
	}

	private void toggleCategory(TreePath path)
	{
		if (tree.isExpanded(path))
		{
			tree.collapsePath(path);
		}
		else
		{
			tree.expandPath(path);
		}
	}

	private boolean isStarClick(TreePath path, MouseEvent e)
	{
		Rectangle bounds = tree.getPathBounds(path);
		if (bounds == null)
		{
			return false;
		}

		int starHitLeft = bounds.x + bounds.width - STAR_HIT_WIDTH_PX - STAR_RIGHT_PADDING_PX;
		return e.getX() >= starHitLeft;
	}

	private void toggleFavourite(String id)
	{
		if (favouritesStore == null)
		{
			return;
		}

		if (favouriteIds.contains(id))
		{
			favouriteIds.remove(id);
			favouritesStore.remove(id);
		}
		else
		{
			favouriteIds.add(id);
			favouritesStore.add(id);
		}

		rebuildTree();
	}

	private void setDestination(int x, int y, int plane)
	{
		if (destinationService == null)
		{
			return;
		}

		destinationService.setDestination(new WorldPoint(x, y, plane));
	}

	private void reloadCustomLocations()
	{
		customLocationsById.clear();

		if (customLocationsStore == null)
		{
			return;
		}

		for (CustomLocation l : customLocationsStore.load())
		{
			if (l != null)
			{
				customLocationsById.put(l.getId(), l);
			}
		}
	}

	private void rebuildTree()
	{
		reloadCustomLocations();

		String query = normalize(searchField.getText());
		boolean searching = !query.isEmpty();

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

		DefaultMutableTreeNode favouritesNode = buildFavouritesNode(query, searching);
		if (favouritesNode != null)
		{
			root.add(favouritesNode);
		}

		DefaultMutableTreeNode customNode = buildCustomLocationsNode(query, searching);
		if (customNode != null)
		{
			root.add(customNode);
		}

		for (CategoryDefinition category : catalog.getCategories())
		{
			DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(category);

			List<LocationDefinition> locations = catalog.getLocationsByCategoryId()
				.getOrDefault(category.getId(), List.of());

			for (LocationDefinition location : locations)
			{
				if (searching && !normalize(location.getName()).contains(query))
				{
					continue;
				}
				catNode.add(new DefaultMutableTreeNode(location));
			}

			if (searching)
			{
				if (catNode.getChildCount() > 0)
				{
					root.add(catNode);
				}
			}
			else
			{
				root.add(catNode);
			}
		}

		tree.setModel(new DefaultTreeModel(root));

		if (searching)
		{
			expandAll();
		}
		else
		{
			applySavedExpansionState();
		}
	}

	private DefaultMutableTreeNode buildCustomLocationsNode(String query, boolean searching)
	{
		DefaultMutableTreeNode customNode = new DefaultMutableTreeNode(
			new CategoryDefinition(CUSTOM_CATEGORY_ID, CUSTOM_CATEGORY_NAME)
		);

		for (CustomLocation l : customLocationsById.values())
		{
			if (searching && !normalize(l.getName()).contains(query))
			{
				continue;
			}
			customNode.add(new DefaultMutableTreeNode(l));
		}

		if (searching)
		{
			return customNode.getChildCount() == 0 ? null : customNode;
		}

		return customNode;
	}

	private DefaultMutableTreeNode buildFavouritesNode(String query, boolean searching)
	{
		DefaultMutableTreeNode favNode = new DefaultMutableTreeNode(
			new CategoryDefinition(FAVOURITES_CATEGORY_ID, FAVOURITES_DISPLAY_NAME)
		);

		for (String favId : favouriteIds)
		{
			LocationDefinition builtIn = catalog.getLocationsById().get(favId);
			if (builtIn != null)
			{
				if (!searching || normalize(builtIn.getName()).contains(query))
				{
					favNode.add(new DefaultMutableTreeNode(builtIn));
				}
				continue;
			}

			CustomLocation custom = customLocationsById.get(favId);
			if (custom != null)
			{
				if (!searching || normalize(custom.getName()).contains(query))
				{
					favNode.add(new DefaultMutableTreeNode(custom));
				}
			}
		}

		if (searching)
		{
			return favNode.getChildCount() == 0 ? null : favNode;
		}

		return favNode;
	}

	private void applySavedExpansionState()
	{
		Object rootObj = tree.getModel().getRoot();
		if (!(rootObj instanceof DefaultMutableTreeNode))
		{
			return;
		}

		DefaultMutableTreeNode root = (DefaultMutableTreeNode) rootObj;

		for (int i = 0; i < root.getChildCount(); i++)
		{
			DefaultMutableTreeNode catNode = (DefaultMutableTreeNode) root.getChildAt(i);
			Object userObject = catNode.getUserObject();
			if (!(userObject instanceof CategoryDefinition))
			{
				continue;
			}

			CategoryDefinition category = (CategoryDefinition) userObject;
			boolean expanded = expandedByCategoryId.getOrDefault(category.getId(), false);

			TreePath path = new TreePath(catNode.getPath());
			if (expanded)
			{
				tree.expandPath(path);
			}
			else
			{
				tree.collapsePath(path);
			}
		}
	}

	private void expandAll()
	{
		for (int i = 0; i < tree.getRowCount(); i++)
		{
			tree.expandRow(i);
		}
	}

	private void updateExpansionStateFromEvent(javax.swing.event.TreeExpansionEvent event, boolean expanded)
	{
		String query = normalize(searchField.getText());
		if (!query.isEmpty())
		{
			return;
		}

		Object last = event.getPath().getLastPathComponent();
		if (!(last instanceof DefaultMutableTreeNode))
		{
			return;
		}

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) last;
		Object userObject = node.getUserObject();
		if (userObject instanceof CategoryDefinition)
		{
			expandedByCategoryId.put(((CategoryDefinition) userObject).getId(), expanded);
			tree.repaint();
		}
	}

	private static String normalize(String s)
	{
		return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
	}

	private final class TakeMeThereTreeCellRenderer extends JPanel implements javax.swing.tree.TreeCellRenderer
	{
		private final JLabel textLabel = new JLabel();
		private final JLabel starLabel = new JLabel();

		private TakeMeThereTreeCellRenderer()
		{
			setLayout(new BorderLayout());
			setOpaque(true);

			textLabel.setOpaque(false);

			starLabel.setOpaque(false);
			starLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			starLabel.setPreferredSize(new Dimension(STAR_HIT_WIDTH_PX, 0));

			add(textLabel, BorderLayout.CENTER);
			add(starLabel, BorderLayout.EAST);
		}

		@Override
		public Component getTreeCellRendererComponent(
			JTree tree,
			Object value,
			boolean selected,
			boolean expanded,
			boolean leaf,
			int row,
			boolean hasFocus)
		{
			Object userObject = extractUserObject(value);

			setBackground(ColorScheme.DARK_GRAY_COLOR);

			textLabel.setForeground(Color.WHITE);
			starLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

			starLabel.setVisible(false);
			starLabel.setText("");

			if (userObject instanceof CategoryDefinition)
			{
				setBorder(new EmptyBorder(2, 6, 2, 6));

				CategoryDefinition category = (CategoryDefinition) userObject;
				String prefix = expanded ? "- " : "+ ";
				textLabel.setText(prefix + category.getDisplayName());
				textLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
			}
			else if (userObject instanceof LocationDefinition)
			{
				setBorder(new EmptyBorder(2, 18, 2, 6));

				LocationDefinition location = (LocationDefinition) userObject;
				textLabel.setText(location.getName());
				textLabel.setFont(FontManager.getRunescapeFont().deriveFont(12f));

				starLabel.setVisible(true);
				boolean isFav = favouriteIds.contains(location.getId());
				starLabel.setText(isFav ? STAR_ON : STAR_OFF);
				starLabel.setForeground(isFav ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			}
			else if (userObject instanceof CustomLocation)
			{
				setBorder(new EmptyBorder(2, 18, 2, 6));

				CustomLocation location = (CustomLocation) userObject;
				textLabel.setText(location.getName());
				textLabel.setFont(FontManager.getRunescapeFont().deriveFont(12f));

				starLabel.setVisible(true);
				boolean isFav = favouriteIds.contains(location.getId());
				starLabel.setText(isFav ? STAR_ON : STAR_OFF);
				starLabel.setForeground(isFav ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			}
			else
			{
				setBorder(new EmptyBorder(2, 6, 2, 6));
				textLabel.setText(String.valueOf(userObject));
				textLabel.setFont(FontManager.getRunescapeFont().deriveFont(12f));
			}

			return this;
		}

		private Object extractUserObject(Object value)
		{
			if (value instanceof DefaultMutableTreeNode)
			{
				return ((DefaultMutableTreeNode) value).getUserObject();
			}
			return value;
		}
	}
}
