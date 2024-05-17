/*
 * PortalNetwork - Portals for Players
 * Copyright (C) 2022 PortalNetwork Developers
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

package au.com.grieve.portalnetwork;

import au.com.grieve.portalnetwork.portals.EndPortal;
import au.com.grieve.portalnetwork.portals.HiddenPortal;
import au.com.grieve.portalnetwork.portals.NetherPortal;
import au.com.grieve.portalnetwork.portals.Portal;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class PortalManager {
  private final BiMap<String, Class<? extends Portal>> portalClasses = HashBiMap.create();

  private final Map<String, PortalConfig> portalConfig = new HashMap<>();

  // Portals
  private final List<Portal> portals = new ArrayList<>();

  // Location Maps
  private final Hashtable<BlockVector, Portal> indexFrames = new Hashtable<>();
  private final Hashtable<BlockVector, Portal> indexPortals = new Hashtable<>();
  private final Hashtable<BlockVector, Portal> indexBases = new Hashtable<>();
  private final Hashtable<BlockVector, Portal> indexPortalBlocks = new Hashtable<>();

  // Recipes
  private final List<NamespacedKey> recipes = new ArrayList<>();

  // Register a new Portal Class
  public void registerPortalClass(
      String name, Class<? extends Portal> portalClass, PortalConfig config) {
    portalClasses.put(name, portalClass);
    portalConfig.put(name, config);

    // Handle custom recipe
    if (config.recipe() != null) {
      PortalConfig.RecipeConfig r = config.recipe();
      try {
        ItemStack item = createPortalBlock(name);
        NamespacedKey key = new NamespacedKey(PortalNetwork.instance, name);
        ShapedRecipe recipe = new ShapedRecipe(key, item);
        recipe.shape(r.items().toArray(new String[0]));
        for (Map.Entry<Character, Material> ingredient : r.mapping().entrySet()) {
          recipe.setIngredient(ingredient.getKey(), ingredient.getValue());
        }
        PortalNetwork.instance.getServer().addRecipe(recipe);
        recipes.add(key);
      } catch (InvalidPortalException e) {
        // ignored
      }
    }
  }

  public void clear() {
    while (!portals.isEmpty()) {
      Portal portal = portals.removeFirst();
      portal.remove();
    }
  }

  public void load() {
    // Portal Data
    YamlConfiguration portalConfig = new YamlConfiguration();
    try {
      portalConfig.load(new File(PortalNetwork.instance.getDataFolder(), "portal-data.yml"));
    } catch (FileNotFoundException e) {
      // ignored
    } catch (IOException | InvalidConfigurationException e) {
      PortalNetwork.logWarning(
          "Failed to load 'portal-data.yml'. Ignoring but portal data may be lost");
    }

    // Initialize all portals
    Map<Portal, Integer> dialed = new HashMap<>();

    ConfigurationSection portalsData = portalConfig.getConfigurationSection("portals");
    if (portalsData != null) {
      for (String key : portalsData.getKeys(false)) {
        ConfigurationSection portalData = portalsData.getConfigurationSection(key);
        if (portalData == null) {
          continue;
        }

        Portal portal;
        try {
          portal =
              createPortal(portalData.getString("portal_type"), portalData.getLocation("location"));
        } catch (InvalidPortalException e) {
          PortalNetwork.logError(e);
          continue;
        }

        if (portalData.contains("dialled")) {
          dialed.put(portal, portalData.getInt("dialled"));
        }
      }
    }

    // Dial Portals
    for (Map.Entry<Portal, Integer> dialedPortal : dialed.entrySet()) {
      dialedPortal.getKey().dial(dialedPortal.getValue());
    }
  }

  public void save() {
    // Portal Data
    YamlConfiguration portalConfig = new YamlConfiguration();
    ConfigurationSection portalsData = portalConfig.createSection("portals");
    for (int i = 0; i < portals.size(); i++) {
      Portal portal = portals.get(i);
      ConfigurationSection portalData = portalsData.createSection(Integer.toString(i));

      if (portal.getDialledPortal() != null) {
        portalData.set("dialled", portal.getDialledPortal().getAddress());
      }
      portalData.set("portal_type", portalClasses.inverse().get(portal.getClass()));
      portalData.set("location", portal.getLocation());
      portalData.set("valid", portal.isValid());
    }

    try {
      portalConfig.save(new File(PortalNetwork.instance.getDataFolder(), "portal-data.yml"));
    } catch (IOException e) {
      PortalNetwork.logWarning(
          "Failed to save 'portal-data.yml'. Ignoring but portal data may be lost");
    }
  }

  // Create a new portal
  public Portal createPortal(String portalType, Location location) throws InvalidPortalException {
    if (portalType == null) {
      throw new InvalidPortalException("Missing portal type");
    }

    Portal portal =
        switch (portalType) {
          case "end" -> new EndPortal(location, this.portalConfig.get(portalType));
          case "nether" -> new NetherPortal(location, this.portalConfig.get(portalType));
          case "hidden" -> new HiddenPortal(location, this.portalConfig.get(portalType));
          default -> throw new InvalidPortalException("No such portal type");
        };

    if (!portalClasses.containsKey(portalType)) {
      throw new InvalidPortalException("No such portal type");
    }
    portals.add(portal);
    return portal;
  }

  public void removePortal(Portal portal) {
    portals.remove(portal);
    indexFrames.values().removeIf(v -> v.equals(portal));
    indexPortals.values().removeIf(v -> v.equals(portal));
    indexBases.values().removeIf(v -> v.equals(portal));
    indexPortalBlocks.values().removeIf(v -> v.equals(portal));
  }

  // Create block based upon portal
  public ItemStack createPortalBlock(Portal portal) throws InvalidPortalException {
    return createPortalBlock(getPortalClasses().inverse().get(portal.getClass()));
  }

  public ItemStack createPortalBlock(String portalType) throws InvalidPortalException {
    if (!portalClasses.containsKey(portalType) || !portalConfig.containsKey(portalType)) {
      throw new InvalidPortalException("No such portal type");
    }

    PortalConfig pc = portalConfig.get(portalType);

    // Create a Portal Block
    ItemStack item = new ItemStack(pc.item().block(), 1);
    ItemMeta meta = item.getItemMeta();

    assert meta != null;
    meta.displayName(Component.text(pc.item().name()));
    meta.getPersistentDataContainer()
        .set(Portal.PortalTypeKey, PersistentDataType.STRING, portalType);
    item.setItemMeta(meta);
    return item;
  }

  public void reindexPortal(Portal portal) {
    indexPortalBlocks.values().removeIf(v -> v.equals(portal));
    indexPortalBlocks.put(portal.getLocation().toVector().toBlockVector(), portal);

    indexFrames.values().removeIf(v -> v.equals(portal));
    for (Iterator<BlockVector> it = portal.getPortalFrameIterator(); it.hasNext(); ) {
      BlockVector loc = it.next();
      indexFrames.put(loc, portal);
    }

    indexPortals.values().removeIf(v -> v.equals(portal));
    for (Iterator<BlockVector> it = portal.getPortalIterator(); it.hasNext(); ) {
      BlockVector loc = it.next();
      indexPortals.put(loc, portal);
    }

    indexBases.values().removeIf(v -> v.equals(portal));
    for (Iterator<BlockVector> it = portal.getPortalBaseIterator(); it.hasNext(); ) {
      BlockVector loc = it.next();
      indexBases.put(loc, portal);
    }
  }

  // Find a portal
  public Portal find(Integer network, Integer address, Boolean valid) {
    for (Portal portal : portals) {
      if (valid != null && portal.isValid() != valid) {
        continue;
      }

      if (!Objects.equals(portal.getNetwork(), network)) {
        continue;
      }

      if (!Objects.equals(portal.getAddress(), address)) {
        continue;
      }

      return portal;
    }
    return null;
  }

  public Portal find(Integer network, Integer address) {
    return find(network, address, null);
  }

  // Get a portal at location
  public Portal find(@NotNull BlockVector search, Boolean valid) {
    Portal portal =
        Stream.concat(
                indexFrames.entrySet().stream(),
                Stream.concat(indexPortals.entrySet().stream(), indexBases.entrySet().stream()))
            .filter(e -> e.getKey().equals(search))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);

    if (portal != null) {
      if (valid == null || valid == portal.isValid()) {
        return portal;
      }
    }
    return null;
  }

  // Get a portal at location
  public Portal find(@NotNull Location location, Boolean valid, int distance) {
    BlockVector search = location.toVector().toBlockVector();
    Portal portal;

    // Check exact match
    portal = find(search, valid);

    if (portal != null) {
      return portal;
    }

    for (int x = -distance; x < distance; x++) {
      for (int y = -distance; y < distance; y++) {
        for (int z = -distance; z < distance; z++) {
          portal = find(search.clone().add(new Vector(x, y, z)).toBlockVector(), valid);
          if (portal != null) {
            return portal;
          }
        }
      }
    }

    return null;
  }

  public Portal find(@NotNull Location location) {
    return find(location, null, 0);
  }

  public Portal find(@NotNull Location location, int distance) {
    return find(location, null, distance);
  }

  // Get a portal based upon its inside
  public Portal findByPortal(@NotNull BlockVector search, Boolean valid) {
    Portal portal =
        indexPortals.entrySet().stream()
            .filter(e -> e.getKey().equals(search))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);

    if (portal != null) {
      if (valid == null || valid == portal.isValid()) {
        return portal;
      }
    }
    return null;
  }

  public Portal findByPortal(@NotNull Location location) {
    return findByPortal(location.toVector().toBlockVector(), null);
  }

  public Portal getPortal(@NotNull Location location) {
    return indexPortalBlocks.get(location.toVector().toBlockVector());
  }

  public BiMap<String, Class<? extends Portal>> getPortalClasses() {
    return this.portalClasses;
  }

  public List<Portal> getPortals() {
    return this.portals;
  }

  public List<NamespacedKey> getRecipes() {
    return this.recipes;
  }
}
