package com.example.onboarding;

import com.example.TakeMeThereConfig;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.Keybind;
import org.junit.Test;

import static org.junit.Assert.*;

public class PaletteTipStateTest
{
	private final Map<String, String> values = new HashMap<>();
	private int writes;
	private final Keybind defaultKey = TakeMeThereConfig.DEFAULT_PALETTE_HOTKEY;
	private final Keybind customKey = new Keybind(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK);

	@Test
	public void newDefaultUserSeesTipWithoutPersistingDismissal()
	{
		PaletteTipState tip = state();
		assertTrue(tip.reconcile(defaultKey));
		assertFalse(tip.isDismissed());
		assertEquals(0, writes);
		assertTrue(values.isEmpty());
	}

	@Test
	public void persistedDismissalSurvivesReload()
	{
		values.put("paletteTipDismissed", "true");
		assertFalse(state().reconcile(defaultKey));
		assertTrue(state().isDismissed());
		assertEquals(0, writes);
	}

	@Test
	public void customAndUnsetBindingsDismissAndNeverReappear()
	{
		PaletteTipState tip = state();
		assertFalse(tip.reconcile(customKey));
		assertTrue(tip.isDismissed());
		assertEquals("true", values.get("paletteTipDismissed"));
		assertFalse(tip.reconcile(defaultKey));
		assertFalse(state().reconcile(defaultKey));
		assertEquals(1, writes);

		values.clear();
		PaletteTipState unset = state();
		assertFalse(unset.reconcile(Keybind.NOT_SET));
		assertFalse(unset.reconcile(defaultKey));
		assertFalse(state().reconcile(defaultKey));
		assertEquals(2, writes);
	}

	@Test
	public void existingCustomBindingAtStartupIsPersistentlyDismissed()
	{
		PaletteTipState first = state();
		assertFalse(first.reconcile(customKey));
		assertEquals("true", values.get("paletteTipDismissed"));
		assertFalse(state().reconcile(defaultKey));
	}

	@Test
	public void unrelatedActionsCannotDismissTip()
	{
		PaletteTipState tip = state();
		values.put("favourites", "[]");
		values.put("customLocations", "[]");
		values.put("learnedSearchHistory", "{}");
		assertTrue(tip.shouldShow(defaultKey));
		tip.openedWith(customKey); // successful custom opening is not default-tip discovery
		assertTrue(tip.shouldShow(defaultKey));
		assertEquals(0, writes);
	}

	private PaletteTipState state()
	{
		return new PaletteTipState(() -> values.get("paletteTipDismissed"), value ->
		{
			writes++;
			values.put("paletteTipDismissed", value);
		});
	}
}
