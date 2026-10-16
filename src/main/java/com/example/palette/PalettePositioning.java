package com.example.palette;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

public final class PalettePositioning
{
	private PalettePositioning()
	{
	}

	/** Centre horizontally; place the dialog centre one third down the usable monitor. */
	public static Rectangle boundsFor(Rectangle usable, Dimension preferred)
	{
		int width = Math.min(Math.max(1, preferred.width), usable.width);
		int height = Math.min(Math.max(1, preferred.height), usable.height);
		int x = usable.x + (usable.width - width) / 2;
		int y = usable.y + usable.height / 3 - height / 2;
		y = Math.max(usable.y, Math.min(y, usable.y + usable.height - height));
		return new Rectangle(x, y, width, height);
	}

	/** Uses the RuneLite owner's monitor and excludes taskbar or dock insets. */
	public static Rectangle usableBounds(Window owner)
	{
		GraphicsConfiguration screen = owner.getGraphicsConfiguration();
		if (screen == null)
		{
			screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice().getDefaultConfiguration();
		}
		Rectangle bounds = screen.getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
		return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
			bounds.width - insets.left - insets.right,
			bounds.height - insets.top - insets.bottom);
	}
}
