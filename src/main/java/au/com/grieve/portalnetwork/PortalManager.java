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

import au.com.grieve.portalnetwork.portals.Portal;
import au.com.grieve.portalnetwork.portals.PortalTypes;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PortalManager {
  // Live portals
  private final List<Portal> portals = new ArrayList<>();

  // Location Maps
  private final Hashtable<BlockVector, Portal> indexFrames = new Hashtable<>();
  private final Hashtable<BlockVector, Portal> indexPortals = new Hashtable<>();
  private final Hashtable<BlockVector, Portal> indexBases = new Hashtable<>();
  private final Hashtable<BlockVector, Portal> indexPortalBlocks = new Hashtable<>();

  public static @NotNull ItemStack createPortalBlock(@NotNull String type) {
    ItemStack item = new ItemStack(Material.GOLD_BLOCK, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text("Portal Block (" + type + ")"));
    meta.getPersistentDataContainer().set(Portal.PortalTypeKey, PersistentDataType.STRING, type);
    item.setItemMeta(meta);
    return item;
  }

  public void clear() {
    while (!this.portals.isEmpty()) {
      Portal portal = this.portals.removeFirst();
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
              this.createPortal(
                  portalData.getString("portal_type"), portalData.getLocation("location"));
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
    for (int i = 0; i < this.portals.size(); i++) {
      Portal portal = this.portals.get(i);
      ConfigurationSection portalData = portalsData.createSection(Integer.toString(i));

      if (portal.getDialledPortal() != null) {
        portalData.set("dialled", portal.getDialledPortal().getAddress());
      }
      portalData.set("portal_type", portal.getType());
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

    Portal portal = PortalTypes.createPortalAt(portalType, location);
    this.portals.add(portal);
    return portal;
  }

  public void removePortal(@NotNull Portal portal) {
    this.portals.remove(portal);
    this.indexFrames.values().removeIf((Portal v) -> v.equals(portal));
    this.indexPortals.values().removeIf((Portal v) -> v.equals(portal));
    this.indexBases.values().removeIf((Portal v) -> v.equals(portal));
    this.indexPortalBlocks.values().removeIf((Portal v) -> v.equals(portal));
  }

  public void reindexPortal(Portal portal) {
    this.indexPortalBlocks.values().removeIf((Portal v) -> v.equals(portal));
    this.indexPortalBlocks.put(portal.getLocation().toVector().toBlockVector(), portal);

    this.indexFrames.values().removeIf((Portal v) -> v.equals(portal));
    for (Iterator<BlockVector> it = portal.getPortalFrameIterator(); it.hasNext(); ) {
      BlockVector loc = it.next();
      this.indexFrames.put(loc, portal);
    }

    this.indexPortals.values().removeIf((Portal v) -> v.equals(portal));
    for (Iterator<BlockVector> it = portal.getPortalIterator(); it.hasNext(); ) {
      BlockVector loc = it.next();
      this.indexPortals.put(loc, portal);
    }

    this.indexBases.values().removeIf((Portal v) -> v.equals(portal));
    for (Iterator<BlockVector> it = portal.getPortalBaseIterator(); it.hasNext(); ) {
      BlockVector loc = it.next();
      this.indexBases.put(loc, portal);
    }
  }

  // Find a portal
  public @Nullable Portal find(
      @NotNull Integer network, @NotNull Integer address, @Nullable Boolean valid) {
    for (Portal portal : this.portals) {
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

  public @Nullable Portal find(@NotNull Integer network, @NotNull Integer address) {
    return this.find(network, address, null);
  }

  // Get a portal at location
  public @Nullable Portal find(@NotNull BlockVector search, @Nullable Boolean valid) {
    Portal portal =
        Stream.concat(
                this.indexFrames.entrySet().stream(),
                Stream.concat(
                    this.indexPortals.entrySet().stream(), this.indexBases.entrySet().stream()))
            .filter((Map.Entry<BlockVector, Portal> e) -> e.getKey().equals(search))
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
  public @Nullable Portal find(@NotNull Location location, @Nullable Boolean valid, int distance) {
    BlockVector search = location.toVector().toBlockVector();
    Portal portal;

    // Check exact match
    portal = this.find(search, valid);

    if (portal != null) {
      return portal;
    }

    for (int x = -distance; x < distance; x++) {
      for (int y = -distance; y < distance; y++) {
        for (int z = -distance; z < distance; z++) {
          portal = this.find(search.clone().add(new Vector(x, y, z)).toBlockVector(), valid);
          if (portal != null) {
            return portal;
          }
        }
      }
    }

    return null;
  }

  public @Nullable Portal find(@NotNull Location location) {
    return this.find(location, null, 0);
  }

  public @Nullable Portal find(@NotNull Location location, int distance) {
    return this.find(location, null, distance);
  }

  // Get a portal based upon its inside
  public @Nullable Portal findByPortal(@NotNull BlockVector search, @Nullable Boolean valid) {
    Portal portal =
        this.indexPortals.entrySet().stream()
            .filter((Map.Entry<BlockVector, Portal> e) -> e.getKey().equals(search))
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

  public @Nullable Portal findByPortal(@NotNull Location location) {
    return this.findByPortal(location.toVector().toBlockVector(), null);
  }

  public @Nullable Portal getPortal(@NotNull Location location) {
    return this.indexPortalBlocks.get(location.toVector().toBlockVector());
  }

  public @NotNull List<Portal> getPortals() {
    return this.portals;
  }
}
