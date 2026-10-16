package com.example.palette;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import net.runelite.client.ui.components.IconTextField;

/** Binds palette actions on the text field that actually receives keyboard focus. */
final class PaletteKeyBindings
{
	private PaletteKeyBindings()
	{
	}

	static void install(IconTextField searchField, Runnable down, Runnable up,
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
