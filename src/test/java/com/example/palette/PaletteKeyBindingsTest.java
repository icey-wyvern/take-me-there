package com.example.palette;

import com.example.history.LearnedHistoryStore;
import com.example.history.TestHistoryStore;
import com.example.search.SearchMetadata;
import com.example.search.SearchService;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.components.IconTextField;
import org.junit.Test;

import static org.junit.Assert.*;

/** Exercises the actual input/action maps on IconTextField's focused child. */
public class PaletteKeyBindingsTest
{
	private final Map<String, String> values = new HashMap<>();
	private final LearnedHistoryStore history = TestHistoryStore.create(values);
	private final List<Selection> selections = new ArrayList<>();
	private final List<PaletteDestination> destinations = List.of(
		new PaletteDestination("a", "Chambers of Xeric", "Raids", new WorldPoint(1, 1, 0)),
		new PaletteDestination("b", "Chambers East", "Raids", new WorldPoint(2, 2, 0)));
	private int closes;
	private DestinationPaletteController controller;
	private IconTextField searchField;
	private JTextField focusedInput;

	@Test
	public void focusedEnterSelectsHighlightedResultOnceWithNormalizedQuery() throws Exception
	{
		onEdt(() ->
		{
			open();
			searchField.setText("  CHAMBERS!!  ");
			controller.queryChanged(searchField.getText());
			assertEquals(0, controller.getSelectedIndex());
			press(KeyEvent.VK_ENTER);
			press(KeyEvent.VK_ENTER); // pending dispatch cannot be duplicated
			assertEquals(1, selections.size());
			assertEquals(controller.getVisible().get(0).getId(), selections.get(0).destination.getId());
			assertEquals("chambers", selections.get(0).query);
			assertEquals(0, closes);
			selections.get(0).complete.accept(true);
			assertEquals(1, closes);
			assertFalse(controller.isOpen());
		});
	}

	@Test
	public void focusedEscapeClosesWithoutSelectingOrChangingHistory() throws Exception
	{
		onEdt(() ->
		{
			history.recordSelection("a", "");
			String before = values.get("history");
			open();
			press(KeyEvent.VK_ESCAPE);
			assertEquals(1, closes);
			assertFalse(controller.isOpen());
			assertTrue(selections.isEmpty());
			assertEquals(before, values.get("history"));
		});
	}

	@Test
	public void focusedArrowsWrapAndEmptyEnterPreservesLastDestinationRules() throws Exception
	{
		onEdt(() ->
		{
			history.recordSelection("b", "");
			history.recordSelection("a", "");
			open();
			assertEquals(0, controller.getSelectedIndex());
			press(KeyEvent.VK_DOWN);
			assertEquals(1, controller.getSelectedIndex());
			press(KeyEvent.VK_DOWN);
			assertEquals(0, controller.getSelectedIndex());
			press(KeyEvent.VK_UP);
			assertEquals(1, controller.getSelectedIndex());
			assertEquals("", searchField.getText());
			press(KeyEvent.VK_ENTER);
			assertEquals("b", selections.get(0).destination.getId());
			assertEquals("", selections.get(0).query);
		});
	}

	@Test
	public void emptyEnterRepeatsLastButRequiresNavigationWithoutValidLast() throws Exception
	{
		onEdt(() ->
		{
			history.recordSelection("a", "");
			open();
			press(KeyEvent.VK_ENTER);
			assertEquals("a", selections.get(0).destination.getId());
			assertEquals("", selections.get(0).query);
			selections.get(0).complete.accept(false);
			controller.closeWithoutSelection();

			history.recordSelection("stale", "");
			open();
			assertEquals(-1, controller.getSelectedIndex());
			press(KeyEvent.VK_ENTER);
			assertEquals(1, selections.size());
			press(KeyEvent.VK_DOWN);
			press(KeyEvent.VK_ENTER);
			assertEquals(2, selections.size());
			assertEquals("a", selections.get(1).destination.getId());
			assertEquals("", selections.get(1).query);
		});
	}

	private void open()
	{
		controller = new DestinationPaletteController(() -> destinations,
			new SearchService(new SearchMetadata(Map.of()), history), history,
			(destination, query, complete) -> selections.add(new Selection(destination, query, complete)),
			() -> closes++);
		searchField = new IconTextField();
		PaletteKeyBindings.install(searchField, () -> controller.moveSelection(1),
			() -> controller.moveSelection(-1), controller::confirm, controller::closeWithoutSelection);
		focusedInput = PaletteKeyBindings.findTextInput(searchField);
		assertNotNull(focusedInput);
		assertNotSame(searchField, focusedInput);
		controller.open();
	}

	private void press(int keyCode)
	{
		KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, 0);
		Object key = focusedInput.getInputMap(JComponent.WHEN_FOCUSED).get(stroke);
		assertNotNull("No focused-input binding for " + keyCode, key);
		Action action = focusedInput.getActionMap().get(key);
		assertNotNull(action);
		action.actionPerformed(new ActionEvent(focusedInput, ActionEvent.ACTION_PERFORMED, key.toString()));
	}

	private static void onEdt(Runnable action) throws Exception
	{
		SwingUtilities.invokeAndWait(action);
	}

	private static final class Selection
	{
		private final PaletteDestination destination;
		private final String query;
		private final Consumer<Boolean> complete;

		private Selection(PaletteDestination destination, String query, Consumer<Boolean> complete)
		{
			this.destination = destination;
			this.query = query;
			this.complete = complete;
		}
	}
}
