package com.example.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.components.IconTextField;

/** Visual-only hint for RuneLite's IconTextField, which has no placeholder API. */
final class PlaceholderIconTextField extends IconTextField
{
	private final String placeholder;
	private final JTextField input;

	PlaceholderIconTextField(String placeholder)
	{
		this.placeholder = placeholder;
		this.input = findTextInput(this);
		if (input == null)
		{
			throw new IllegalStateException("IconTextField has no text input");
		}
		getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent event) { repaint(); }
			@Override public void removeUpdate(DocumentEvent event) { repaint(); }
			@Override public void changedUpdate(DocumentEvent event) { repaint(); }
		});
	}

	String getPlaceholder()
	{
		return placeholder;
	}

	boolean isPlaceholderVisible()
	{
		return getText().isEmpty();
	}

	@Override
	protected void paintChildren(Graphics graphics)
	{
		super.paintChildren(graphics);
		if (!isPlaceholderVisible())
		{
			return;
		}
		Point origin = SwingUtilities.convertPoint(input, 0, 0, this);
		FontMetrics metrics = input.getFontMetrics(input.getFont());
		Color hintColor = UIManager.getColor("TextField.inactiveForeground");
		Graphics copy = graphics.create();
		try
		{
			copy.setColor(hintColor != null ? hintColor : input.getDisabledTextColor());
			copy.setFont(input.getFont());
			copy.drawString(placeholder, origin.x + input.getInsets().left,
				origin.y + (input.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
		}
		finally
		{
			copy.dispose();
		}
	}

	private static JTextField findTextInput(Component component)
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
}
