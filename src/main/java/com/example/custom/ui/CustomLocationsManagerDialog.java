package com.example.custom.ui;

import com.example.custom.CustomLocation;
import com.example.custom.CustomLocationsStore;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CustomLocationsManagerDialog extends JDialog
{
	private static final String CUSTOM_CATEGORY_ID = "custom";
	private static final String CUSTOM_CATEGORY_NAME = "Custom Locations";

	private final CustomLocationsStore store;

	private final CustomLocationsTableModel tableModel = new CustomLocationsTableModel();
	private final JTable table = new JTable(tableModel);

	private final JButton addButton = new JButton("Add");
	private final JButton editButton = new JButton("Edit");
	private final JButton deleteButton = new JButton("Delete");
	private final JButton closeButton = new JButton("Close");

	public CustomLocationsManagerDialog(Window owner, CustomLocationsStore store)
	{
		super(owner, "Take Me There - Custom Locations");
		this.store = Objects.requireNonNull(store, "store");

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setMinimumSize(new Dimension(700, 420));
		setSize(700, 420);
		setLocationRelativeTo(owner);

		initUi();
		loadFromStore();
	}

	private void initUi()
	{
		setLayout(new BorderLayout(8, 8));

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);
		table.getSelectionModel().addListSelectionListener(e -> updateButtons());

		add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
		actions.add(addButton);
		actions.add(editButton);
		actions.add(deleteButton);

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(actions, BorderLayout.WEST);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		right.add(closeButton);
		bottom.add(right, BorderLayout.EAST);

		add(bottom, BorderLayout.SOUTH);

		addButton.addActionListener(e -> onAdd());
		editButton.addActionListener(e -> onEdit());
		deleteButton.addActionListener(e -> onDelete());
		closeButton.addActionListener(e -> setVisible(false));

		updateButtons();
	}

	private void loadFromStore()
	{
		tableModel.setRows(store.load());
		updateButtons();
	}

	private void persist()
	{
		store.save(tableModel.getRows());
	}

	private void updateButtons()
	{
		boolean hasSelection = table.getSelectedRow() >= 0;
		editButton.setEnabled(hasSelection);
		deleteButton.setEnabled(hasSelection);
	}

	private void onAdd()
	{
		CustomLocationEditor editor = new CustomLocationEditor(this, null);
		editor.setVisible(true);

		CustomLocation created = editor.getResult();
		if (created == null)
		{
			return;
		}

		tableModel.add(created);
		persist();
	}

	private void onEdit()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return;
		}

		CustomLocation existing = tableModel.getAt(row);
		CustomLocationEditor editor = new CustomLocationEditor(this, existing);
		editor.setVisible(true);

		CustomLocation updated = editor.getResult();
		if (updated == null)
		{
			return;
		}

		tableModel.update(row, updated);
		persist();
	}

	private void onDelete()
	{
		int row = table.getSelectedRow();
		if (row < 0)
		{
			return;
		}

		CustomLocation existing = tableModel.getAt(row);

		int confirm = JOptionPane.showConfirmDialog(
			this,
			"Delete \"" + existing.getName() + "\"?",
			"Delete Custom Location",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		);

		if (confirm != JOptionPane.OK_OPTION)
		{
			return;
		}

		tableModel.remove(row);
		persist();
	}

	private static final class CustomLocationsTableModel extends AbstractTableModel
	{
		private static final String[] COLS = {"Name", "Category", "X", "Y", "Plane"};
		private final List<CustomLocation> rows = new ArrayList<>();

		void setRows(List<CustomLocation> newRows)
		{
			rows.clear();
			if (newRows != null)
			{
				rows.addAll(newRows);
			}
			fireTableDataChanged();
		}

		List<CustomLocation> getRows()
		{
			return new ArrayList<>(rows);
		}

		CustomLocation getAt(int row)
		{
			return rows.get(row);
		}

		void add(CustomLocation location)
		{
			rows.add(location);
			int idx = rows.size() - 1;
			fireTableRowsInserted(idx, idx);
		}

		void update(int row, CustomLocation location)
		{
			rows.set(row, location);
			fireTableRowsUpdated(row, row);
		}

		void remove(int row)
		{
			rows.remove(row);
			fireTableRowsDeleted(row, row);
		}

		@Override
		public int getRowCount()
		{
			return rows.size();
		}

		@Override
		public int getColumnCount()
		{
			return COLS.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return COLS[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			CustomLocation l = rows.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return l.getName();
				case 1:
					return CUSTOM_CATEGORY_NAME;
				case 2:
					return l.getX();
				case 3:
					return l.getY();
				case 4:
					return l.getPlane();
				default:
					return "";
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			if (columnIndex >= 2)
			{
				return Integer.class;
			}
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}
	}

	private static final class CustomLocationEditor extends JDialog
	{
		private final JTextField nameField = new JTextField();
		private final JTextField categoryField = new JTextField(CUSTOM_CATEGORY_NAME);
		private final JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
		private final JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
		private final JSpinner planeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));

		private CustomLocation result;

		private final String existingId;

		CustomLocationEditor(Window owner, CustomLocation existing)
		{
			super(owner, existing == null ? "Add Custom Location" : "Edit Custom Location", ModalityType.APPLICATION_MODAL);

			this.existingId = existing == null ? null : existing.getId();

			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			setSize(420, 260);
			setLocationRelativeTo(owner);

			categoryField.setEditable(false);

			initUi();

			if (existing != null)
			{
				nameField.setText(existing.getName());
				xSpinner.setValue(existing.getX());
				ySpinner.setValue(existing.getY());
				planeSpinner.setValue(existing.getPlane());
			}
		}

		CustomLocation getResult()
		{
			return result;
		}

		private void initUi()
		{
			JPanel form = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(6, 6, 6, 6);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;

			int row = 0;

			addRow(form, c, row++, "Name", nameField);
			addRow(form, c, row++, "Category", categoryField);
			addRow(form, c, row++, "X", xSpinner);
			addRow(form, c, row++, "Y", ySpinner);
			addRow(form, c, row++, "Plane", planeSpinner);

			JButton ok = new JButton("Save");
			JButton cancel = new JButton("Cancel");

			ok.addActionListener(e -> onSave());
			cancel.addActionListener(e -> dispose());

			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
			buttons.add(cancel);
			buttons.add(ok);

			setLayout(new BorderLayout());
			add(form, BorderLayout.CENTER);
			add(buttons, BorderLayout.SOUTH);
		}

		private void addRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent field)
		{
			c.gridx = 0;
			c.gridy = row;
			c.weightx = 0;
			panel.add(new JLabel(label), c);

			c.gridx = 1;
			c.gridy = row;
			c.weightx = 1;
			panel.add(field, c);
		}

		private void onSave()
		{
			String name = nameField.getText() == null ? "" : nameField.getText().trim();
			if (name.isEmpty())
			{
				JOptionPane.showMessageDialog(this, "Name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
				return;
			}

			int x = (Integer) xSpinner.getValue();
			int y = (Integer) ySpinner.getValue();
			int plane = (Integer) planeSpinner.getValue();

			String id = existingId != null ? existingId : "custom:" + UUID.randomUUID();

			result = new CustomLocation(id, name, CUSTOM_CATEGORY_ID, x, y, plane);
			dispose();
		}
	}
}
