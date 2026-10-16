package com.example.palette;

import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PalettePositioningTest
{
	private static final Dimension DIALOG = new Dimension(560, 470);

	@Test
	public void centresOnStandardMonitorOneThirdDown()
	{
		Rectangle result = PalettePositioning.boundsFor(new Rectangle(0, 0, 1920, 1080), DIALOG);
		assertEquals(960, result.getCenterX(), 1);
		assertEquals(360, result.getCenterY(), 1);
	}

	@Test
	public void honoursNonzeroOriginsAndLeftOrRightMonitors()
	{
		for (Rectangle monitor : new Rectangle[]{
			new Rectangle(1920, 120, 1600, 900),
			new Rectangle(-1600, -100, 1600, 900)})
		{
			Rectangle result = PalettePositioning.boundsFor(monitor, DIALOG);
			assertEquals(monitor.getCenterX(), result.getCenterX(), 1);
			assertEquals(monitor.y + monitor.height / 3.0, result.getCenterY(), 1);
		}
	}

	@Test
	public void usesTaskbarReducedBounds()
	{
		Rectangle usable = new Rectangle(0, 40, 1920, 1000);
		Rectangle result = PalettePositioning.boundsFor(usable, DIALOG);
		assertEquals(960, result.getCenterX(), 1);
		assertEquals(40 + 1000 / 3.0, result.getCenterY(), 1);
	}

	@Test
	public void oversizedDialogIsClampedToUsableMonitor()
	{
		Rectangle usable = new Rectangle(-900, 80, 500, 300);
		Rectangle result = PalettePositioning.boundsFor(usable, new Dimension(800, 600));
		assertEquals(usable, result);
		assertTrue(usable.contains(result));
	}
}
