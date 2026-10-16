package com.example.ui;

import com.example.model.CategoryDefinition;
import com.example.model.LocationCatalog;
import com.example.model.LocationDefinition;
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

public class TakeMeTherePanelSearchTest
{
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
				TakeMeTherePanel panel = new TakeMeTherePanel(new LocationCatalog(categories, byCategory, byId));

				Field favouriteField = TakeMeTherePanel.class.getDeclaredField("favouriteIds");
				favouriteField.setAccessible(true);
				@SuppressWarnings("unchecked")
				LinkedHashSet<String> favouriteIds = (LinkedHashSet<String>) favouriteField.get(panel);
				favouriteIds.add(second.getId());

				Field searchField = TakeMeTherePanel.class.getDeclaredField("searchField");
				searchField.setAccessible(true);
				((IconTextField) searchField.get(panel)).setText("chambers xeric");

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
			}
			catch (ReflectiveOperationException e)
			{
				throw new AssertionError(e);
			}
		});
	}
}
