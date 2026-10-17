package com.example.palette;

import com.example.DestinationSelectionService;
import com.example.custom.CustomLocationsStore;
import com.example.history.LearnedHistoryStore;
import com.example.model.LocationCatalog;
import com.example.search.SearchService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import net.runelite.client.config.Keybind;

/** Modeless Swing shell controlled by the plugin hotkey lifecycle. */
public final class DestinationPaletteDialog extends JDialog implements PaletteHotkeyController.PaletteWindow
{
	private static final Dimension PREFERRED_SIZE = new Dimension(560, 470);
	private static final String RESULTS_CARD = "results";
	private static final String MESSAGE_CARD = "message";

	private final DestinationPaletteController controller;
	private final IconTextField searchField = new IconTextField();
	private final DefaultListModel<PaletteDestination> listModel = new DefaultListModel<>();
	private final JList<PaletteDestination> results = new JList<PaletteDestination>(listModel)
	{
		@Override
		public String getToolTipText(MouseEvent event)
		{
			int index = locationToIndex(event.getPoint());
			Rectangle cell = index < 0 ? null : getCellBounds(index, index);
			if (cell == null || !cell.contains(event.getPoint()))
			{
				return null;
			}
			PaletteDestination destination = listModel.get(index);
			return destination.getName() + " — " + destination.getCategory();
		}
	};
	private final java.awt.CardLayout cards = new java.awt.CardLayout();
	private final JPanel resultContainer = new JPanel(cards);
	private final JLabel message = new JLabel("", JLabel.CENTER);
	private PaletteKeyBindings keyBindings;

	public DestinationPaletteDialog(Window owner, LocationCatalog catalog, CustomLocationsStore customStore,
		SearchService searchService, LearnedHistoryStore historyStore,
		DestinationSelectionService selectionService)
	{
		super(Objects.requireNonNull(owner, "owner"), "Search destinations", ModalityType.MODELESS);
		Objects.requireNonNull(catalog, "catalog");
		Objects.requireNonNull(selectionService, "selectionService");
		controller = new DestinationPaletteController(
			() -> PaletteDestinationSource.load(catalog, customStore), searchService, historyStore,
			(destination, query, completion) -> selectionService.select(destination.getId(),
				destination.getTarget(), query, completion), this::hideAndRestoreFocus);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setSize(PREFERRED_SIZE);
		buildUi();
		bindKeys();
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				closeWithoutSelection();
			}
		});
	}

	private void buildUi()
	{
		JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		setContentPane(content);

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel title = new JLabel("Search destinations");
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		header.add(title);
		header.add(Box.createVerticalStrut(8));
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		searchField.getAccessibleContext().setAccessibleName("Search destinations");
		searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
		header.add(searchField);
		content.add(header, BorderLayout.NORTH);

		results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		results.setFixedCellHeight(38);
		results.setVisibleRowCount(DestinationPaletteController.VISIBLE_RESULT_LIMIT);
		results.setFocusable(false);
		results.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		results.setCellRenderer(new DestinationRenderer());
		results.getAccessibleContext().setAccessibleName("Destination results");
		results.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (SwingUtilities.isLeftMouseButton(event))
				{
					selectMouseRow(event);
				}
			}

			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2
					&& selectMouseRow(event))
				{
					controller.confirm();
				}
			}
		});
		JScrollPane scroll = new JScrollPane(results);
		scroll.setBorder(null);
		resultContainer.add(scroll, RESULTS_CARD);
		message.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		message.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		message.setOpaque(true);
		resultContainer.add(message, MESSAGE_CARD);
		content.add(resultContainer, BorderLayout.CENTER);

		JLabel hint = new JLabel("↑↓ Select · Enter Route · Esc Close");
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		hint.setFont(FontManager.getRunescapeFont().deriveFont(12f));
		content.add(hint, BorderLayout.SOUTH);

		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e) { updateSearch(); }
			@Override public void removeUpdate(DocumentEvent e) { updateSearch(); }
			@Override public void changedUpdate(DocumentEvent e) { updateSearch(); }
		});
		searchField.addClearListener(this::updateSearch);
	}

	private void bindKeys()
	{
		// IconTextField forwards focus to its nested JTextField, not to the outer JPanel.
		keyBindings = PaletteKeyBindings.install(searchField,
			() -> { controller.moveSelection(1); render(); },
			() -> { controller.moveSelection(-1); render(); },
			controller::confirm, this::closeWithoutSelection);
	}

	private boolean selectMouseRow(MouseEvent event)
	{
		int index = results.locationToIndex(event.getPoint());
		Rectangle cell = index < 0 ? null : results.getCellBounds(index, index);
		if (cell == null || !cell.contains(event.getPoint()))
		{
			return false;
		}
		controller.selectIndex(index);
		render();
		return true;
	}

	private void updateSearch()
	{
		controller.queryChanged(searchField.getText());
		render();
	}

	private void render()
	{
		listModel.clear();
		for (PaletteDestination destination : controller.getVisible())
		{
			listModel.addElement(destination);
		}
		if (controller.getVisible().isEmpty())
		{
			message.setText(controller.getQuery().isEmpty() ? "Search for a destination" : "No destinations found");
			cards.show(resultContainer, MESSAGE_CARD);
		}
		else
		{
			cards.show(resultContainer, RESULTS_CARD);
			results.setSelectedIndex(controller.getSelectedIndex());
			if (controller.getSelectedIndex() >= 0)
			{
				results.ensureIndexIsVisible(controller.getSelectedIndex());
			}
		}
	}

	public void open()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::open);
			return;
		}
		if (controller.isOpen())
		{
			searchField.requestFocusInWindow();
			return;
		}
		controller.open();
		searchField.setText("");
		render();
		setBounds(PalettePositioning.boundsFor(PalettePositioning.usableBounds(getOwner()), PREFERRED_SIZE));
		setVisible(true);
		SwingUtilities.invokeLater(() ->
		{
			if (isVisible())
			{
				searchField.requestFocusInWindow();
			}
		});
	}

	public void closeWithoutSelection()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::closeWithoutSelection);
			return;
		}
		controller.closeWithoutSelection();
	}

	public boolean isOpen()
	{
		return controller.isOpen();
	}

	@Override
	public boolean ownsKeyEvent(KeyEvent event)
	{
		return event.getSource() instanceof Component
			&& SwingUtilities.getWindowAncestor((Component) event.getSource()) == this;
	}

	@Override
	public void configureHotkey(Keybind keybind)
	{
		keyBindings.updateHotkey(keybind);
	}

	public void disposePalette()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::disposePalette);
			return;
		}
		controller.closeWithoutSelection();
		dispose();
	}

	private void hideAndRestoreFocus()
	{
		setVisible(false);
		Window owner = getOwner();
		if (owner != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				if (owner.isDisplayable())
				{
					owner.requestFocus();
				}
			});
		}
	}

	private static final class DestinationRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
			boolean selected, boolean hasFocus)
		{
			PaletteDestination destination = (PaletteDestination) value;
			JPanel row = new JPanel(new BorderLayout());
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, selected ? 3 : 0, 0, 0, Color.WHITE),
				BorderFactory.createEmptyBorder(2, selected ? 7 : 10, 2, 8)));
			row.setBackground(selected ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			JLabel name = new JLabel(destination.getName());
			name.setForeground(Color.WHITE);
			name.setFont(FontManager.getRunescapeFont().deriveFont(13f));
			JLabel category = new JLabel(destination.getCategory());
			category.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			category.setFont(FontManager.getRunescapeFont().deriveFont(11f));
			row.add(name, BorderLayout.CENTER);
			row.add(category, BorderLayout.SOUTH);
			return row;
		}
	}
}
