package com.example.palette;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.components.IconTextField;

/** Binds palette actions on the text field that actually receives keyboard focus. */
final class PaletteKeyBindings
{
	private static final String HOTKEY_CLOSE_ACTION = "paletteHotkeyClose";
	private final JTextField focusedInput;
	private final Map<KeyStroke, Object> replacedBindings = new LinkedHashMap<>();

	private PaletteKeyBindings(JTextField focusedInput, Runnable close)
	{
		this.focusedInput = focusedInput;
		focusedInput.getActionMap().put(HOTKEY_CLOSE_ACTION, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				close.run();
			}
		});
	}

	static PaletteKeyBindings install(IconTextField searchField, Runnable down, Runnable up,
		Runnable confirm, Runnable close)
	{
		JTextField focusedInput = findTextInput(Objects.requireNonNull(searchField, "searchField"));
		if (focusedInput == null)
		{
			throw new IllegalStateException("IconTextField has no focused text input");
		}
		bind(focusedInput, KeyEvent.VK_DOWN, "paletteDown", down);
		bind(focusedInput, KeyEvent.VK_UP, "paletteUp", up);
		bind(focusedInput, KeyEvent.VK_ENTER, "paletteEnter", confirm);
		bind(focusedInput, KeyEvent.VK_ESCAPE, "paletteEscape", close);
		return new PaletteKeyBindings(focusedInput, close);
	}

	void updateHotkey(Keybind keybind)
	{
		for (Map.Entry<KeyStroke, Object> binding : replacedBindings.entrySet())
		{
			KeyStroke stroke = binding.getKey();
			if (HOTKEY_CLOSE_ACTION.equals(focusedInput.getInputMap(JComponent.WHEN_FOCUSED).get(stroke)))
			{
				if (binding.getValue() == null)
				{
					focusedInput.getInputMap(JComponent.WHEN_FOCUSED).remove(stroke);
				}
				else
				{
					focusedInput.getInputMap(JComponent.WHEN_FOCUSED).put(stroke, binding.getValue());
				}
			}
		}
		replacedBindings.clear();
		if (keybind == null || Keybind.NOT_SET.equals(keybind))
		{
			return;
		}
		int code = keybind.getKeyCode();
		int modifiers = keybind.getModifiers();
		if (code != KeyEvent.VK_UNDEFINED)
		{
			addCloseStroke(code, modifiers);
			return;
		}
		// RuneLite also permits modifier-only keybinds. Any final modifier can complete one.
		if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0)
		{
			addCloseStroke(KeyEvent.VK_CONTROL, modifiers);
		}
		if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0)
		{
			addCloseStroke(KeyEvent.VK_ALT, modifiers);
		}
		if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)
		{
			addCloseStroke(KeyEvent.VK_SHIFT, modifiers);
		}
		if ((modifiers & InputEvent.META_DOWN_MASK) != 0)
		{
			addCloseStroke(KeyEvent.VK_META, modifiers);
		}
	}

	private void addCloseStroke(int code, int modifiers)
	{
		KeyStroke stroke = KeyStroke.getKeyStroke(code, modifiers);
		replacedBindings.put(stroke, focusedInput.getInputMap(JComponent.WHEN_FOCUSED).get(stroke));
		focusedInput.getInputMap(JComponent.WHEN_FOCUSED).put(stroke, HOTKEY_CLOSE_ACTION);
	}

	static JTextField findTextInput(Component component)
	{
		if (component instanceof JTextField)
		{
			return (JTextField) component;
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				JTextField found = findTextInput(child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static void bind(JTextField input, int keyCode, String actionKey, Runnable action)
	{
		Objects.requireNonNull(action, actionKey);
		input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, 0), actionKey);
		input.getActionMap().put(actionKey, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				action.run();
			}
		});
	}
}
