/*
 * PortalNetwork - Portals for Players
 * Copyright (C) 2024 PortalNetwork Developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.com.grieve.portalnetwork.portals;

import au.com.grieve.portalnetwork.InvalidPortalException;
import au.com.grieve.portalnetwork.PortalManager;
import au.com.grieve.portalnetwork.PortalNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

public class PortalTypes {
  private PortalTypes() {}

  public static List<String> TYPES = List.of("end", "nether", "hidden");
  private static List<ShapedRecipe> RECIPES;

  public static @NotNull Portal createPortalAt(@NotNull String type, @NotNull Location location)
      throws InvalidPortalException {
    return switch (type) {
      case "end" -> new EndPortal(location);
      case "nether" -> new NetherPortal(location);
      case "hidden" -> new HiddenPortal(location);
      default -> throw new InvalidPortalException("No such portal type");
    };
  }

  public static @NotNull List<ShapedRecipe> getRecipes() {
    if (RECIPES == null) {
      RECIPES = new ArrayList<>(3);
      for (String type : TYPES) {
        PortalRecipe config =
            switch (type) {
              case "end" -> EndPortal.RECIPE;
              case "nether" -> NetherPortal.RECIPE;
              case "hidden" -> HiddenPortal.RECIPE;
              default -> throw new RuntimeException("invalid portal");
            };

        ItemStack item = PortalManager.createPortalBlock(type);
        NamespacedKey key = new NamespacedKey(PortalNetwork.instance, type);
        ShapedRecipe recipe = new ShapedRecipe(key, item);
        recipe.shape(config.items().toArray(new String[0]));
        for (Map.Entry<Character, Material> ingredient : config.mapping().entrySet()) {
          recipe.setIngredient(ingredient.getKey(), ingredient.getValue());
        }

        RECIPES.add(recipe);
      }
    }
    return RECIPES;
  }
}
