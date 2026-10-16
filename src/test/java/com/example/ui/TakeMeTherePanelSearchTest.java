package com.example.ui;

import com.example.model.CategoryDefinition;
import com.example.model.LocationCatalog;
import com.example.model.LocationDefinition;
import com.example.search.SearchMetadata;
import com.example.search.SearchService;
import net.runelite.client.ui.components.IconTextField;
import org.junit.Test;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TakeMeTherePanelSearchTest
{
	@Test
	public void onboardingTipCanHideWithoutChangingSidebarSearch() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			TakeMeTherePanel panel = new TakeMeTherePanel(new LocationCatalog(List.of(), Map.of(), Map.of()));
			assertFalse(panel.isPaletteTipVisible());
			panel.setPaletteTipVisible(true);
			assertTrue(panel.isPaletteTipVisible());
			panel.setPaletteTipVisible(false);
			assertFalse(panel.isPaletteTipVisible());
		});
	}

	@Test
	public void sharedMatcherFiltersWithoutChangingCategoryOrFavouriteOrder() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				LocationDefinition first = new LocationDefinition("first:z", "Z Chambers of Xeric", 1, 2, 0);
				LocationDefinition second = new LocationDefinition("first:a", "A Chambers of Xeric", 3, 4, 0);
				LocationDefinition other = new LocationDefinition("second:other", "Other Place", 5, 6, 0);
				List<CategoryDefinition> categories = List.of(
					new CategoryDefinition("first", "First"),
					new CategoryDefinition("second", "Second")
				);
				Map<String, List<LocationDefinition>> byCategory = new LinkedHashMap<>();
				byCategory.put("first", List.of(first, second));
				byCategory.put("second", List.of(other));
				Map<String, LocationDefinition> byId = new LinkedHashMap<>();
				byId.put(first.getId(), first);
				byId.put(second.getId(), second);
				byId.put(other.getId(), other);
				SearchService searchService = new SearchService(new SearchMetadata(Map.of(
					first.getId(), List.of("cox"), second.getId(), List.of("cox"))));
				TakeMeTherePanel panel = new TakeMeTherePanel(
					new LocationCatalog(categories, byCategory, byId), null, null, null, searchService);

				Field favouriteField = TakeMeTherePanel.class.getDeclaredField("favouriteIds");
				favouriteField.setAccessible(true);
				@SuppressWarnings("unchecked")
				LinkedHashSet<String> favouriteIds = (LinkedHashSet<String>) favouriteField.get(panel);
				favouriteIds.add(second.getId());

				Field searchField = TakeMeTherePanel.class.getDeclaredField("searchField");
				searchField.setAccessible(true);
				IconTextField input = (IconTextField) searchField.get(panel);
				input.setText("chambers xeric");

				Field treeField = TakeMeTherePanel.class.getDeclaredField("tree");
				treeField.setAccessible(true);
				JTree tree = (JTree) treeField.get(panel);
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
				assertEquals(2, root.getChildCount()); // favourites, then matching catalog category
				DefaultMutableTreeNode favourites = (DefaultMutableTreeNode) root.getChildAt(0);
				assertEquals(second, ((DefaultMutableTreeNode) favourites.getChildAt(0)).getUserObject());
				DefaultMutableTreeNode catalogCategory = (DefaultMutableTreeNode) root.getChildAt(1);
				assertEquals(first, ((DefaultMutableTreeNode) catalogCategory.getChildAt(0)).getUserObject());
				assertEquals(second, ((DefaultMutableTreeNode) catalogCategory.getChildAt(1)).getUserObject());

				input.setText("cox");
				DefaultMutableTreeNode aliasRoot = (DefaultMutableTreeNode) tree.getModel().getRoot();
				DefaultMutableTreeNode aliasCategory = (DefaultMutableTreeNode) aliasRoot.getChildAt(1);
				assertEquals(first, ((DefaultMutableTreeNode) aliasCategory.getChildAt(0)).getUserObject());
				assertEquals(second, ((DefaultMutableTreeNode) aliasCategory.getChildAt(1)).getUserObject());
				input.setText("chmabers xeric");
				assertEquals(2, ((DefaultMutableTreeNode) tree.getModel().getRoot()).getChildCount());
			}
			catch (ReflectiveOperationException e)
			{
				throw new AssertionError(e);
			}
		});
	}
}
