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

package au.com.grieve.portalnetwork;

import au.com.grieve.portalnetwork.portals.Portal;
import au.com.grieve.portalnetwork.portals.PortalTypes;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public class PortalEvents implements Listener {
  final Map<Entity, BlockVector> ignore = new HashMap<>();

  // Stop burning portal
  @EventHandler(ignoreCancelled = true)
  public void onBlockBurnEvent(BlockBurnEvent event) {
    Portal portal = PortalNetwork.manager.find(event.getBlock().getLocation());
    if (portal != null) {
      portal.handleBlockBurn();
    }
  }

  // Stop Exploding
  @EventHandler(ignoreCancelled = true)
  public void onBlockExplodeEvent(BlockExplodeEvent event) {
    Portal portal = PortalNetwork.manager.find(event.getBlock().getLocation());
    if (portal != null) {
      portal.handleBlockExplode();
    }
  }

  // Stop ignition
  @EventHandler(ignoreCancelled = true)
  public void onBlockIgniteEvent(BlockIgniteEvent event) {
    Portal portal = PortalNetwork.manager.find(event.getBlock().getLocation());
    if (portal != null) {
      portal.handleBlockIgnite();
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreakEvent(BlockBreakEvent event) {
    // Check if player is breaking a portal block
    Portal portal = PortalNetwork.manager.getPortal(event.getBlock().getLocation());
    if (portal == null) {
      // Check the rest of the portal
      portal = PortalNetwork.manager.find(event.getBlock().getLocation());
    }

    if (portal == null) {
      return;
    }

    // If its the portal block we remove portal and drop the block
    if (event
        .getBlock()
        .getLocation()
        .toVector()
        .toBlockVector()
        .equals(portal.getLocation().toVector().toBlockVector())) {
      event.setDropItems(false);
      if (event.getPlayer().getGameMode() != GameMode.CREATIVE
          && event.getBlock().getLocation().getWorld() != null) {
        event
            .getBlock()
            .getLocation()
            .getWorld()
            .dropItemNaturally(
                event.getBlock().getLocation(), PortalManager.createPortalBlock(portal.getType()));
      }
      portal.remove();
      return;
    }

    portal.handleBlockBreak(event);
  }

  @EventHandler
  public void onPlayerInteractEvent(PlayerInteractEvent event) {
    // Ignore if player is sneaking
    if (event.getPlayer().isSneaking()) {
      return;
    }

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
      return;
    }

    Portal portal = PortalNetwork.manager.find(event.getClickedBlock().getLocation());
    if (portal == null) {
      return;
    }

    portal.handlePlayerInteract(event);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    // Grant knowledge of the crafting recipe to all players
    event
        .getPlayer()
        .discoverRecipes(PortalTypes.getRecipes().stream().map(CraftingRecipe::getKey).toList());
  }

  @EventHandler
  public void onPlayerQuitEvent(PlayerQuitEvent event) {
    this.ignore.remove(event.getPlayer());
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerMoveEvent(PlayerMoveEvent event) {
    // If player has not actually moved, ignore
    if (event.getFrom().toVector().toBlockVector() == event.getTo().toVector().toBlockVector()) {
      return;
    }

    // If ignored player has moved enough we stop ignoring
    if (this.ignore.containsKey(event.getPlayer())) {
      if (this.ignore
              .get(event.getPlayer())
              .distanceSquared(event.getPlayer().getLocation().toVector())
          > 4) {
        this.ignore.remove(event.getPlayer());
      } else {
        return;
      }
    }

    Vector velocity = event.getTo().toVector().subtract(event.getFrom().toVector());

    Location loc =
        event
            .getFrom()
            .clone()
            .add(new Vector(velocity.getX() < 0 ? 0.2 : -0.2, 0, velocity.getZ() < 0 ? 0.2 : -0.2));
    loc.setX(Math.round(loc.getX()));
    loc.setZ(Math.round(loc.getZ()));

    Portal portal = PortalNetwork.manager.findByPortal(loc);
    if (portal == null) {
      return;
    }

    portal.handlePlayerMove(event);
    this.ignore.put(event.getPlayer(), event.getPlayer().getLocation().toVector().toBlockVector());
  }

  // Handle Vehicle moves
  @EventHandler(ignoreCancelled = true)
  public void onVehicleMoveEvent(VehicleMoveEvent event) {
    // If ignored player has moved enough we stop ignoring
    if (this.ignore.containsKey(event.getVehicle())) {
      if (this.ignore
              .get(event.getVehicle())
              .distanceSquared(event.getVehicle().getLocation().toVector())
          > 9) {
        this.ignore.remove(event.getVehicle());
      }
      return;
    }

    // If vehicle has not actually moved, ignore
    if (event.getFrom().toVector().toBlockVector() == event.getTo().toVector().toBlockVector()) {
      return;
    }

    Vector velocity = event.getTo().toVector().subtract(event.getFrom().toVector());

    Location loc = event.getFrom().clone();
    if (velocity.getZ() < 0) {
      loc = loc.add(new Vector(0, 0, 0.5));
    } else {
      loc = loc.add(new Vector(0, 0, -0.5));
    }

    if (velocity.getX() < 0) {
      loc = loc.add(new Vector(0.5, 0, 0));
    } else {
      loc = loc.add(new Vector(-0.5, 0, 0));
    }

    // X and Z to nearest whole number
    loc.setX(Math.round(loc.getX()));
    loc.setZ(Math.round(loc.getZ()));

    Portal portal = PortalNetwork.manager.findByPortal(loc);
    if (portal == null) {
      return;
    }

    portal.handleVehicleMove(event);
    this.ignore.put(
        event.getVehicle(), event.getVehicle().getLocation().toVector().toBlockVector());
  }

  // Probably should move this inside nether/end portal class
  @EventHandler(priority = EventPriority.LOW)
  public void onEntityPortalEvent(EntityPortalEvent event) {
    Portal portal = PortalNetwork.manager.find(event.getFrom(), 2);
    if (portal == null) {
      return;
    }

    // Cancel event
    event.setCancelled(true);
  }

  // Probably should move this inside nether portal class
  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerPortalEvent(PlayerPortalEvent event) {
    if (event.isCancelled()) {
      return;
    }

    // Only interested if its one of the 3 portal types
    if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
        && event.getCause() != PlayerTeleportEvent.TeleportCause.END_GATEWAY
        && event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
      return;
    }

    Portal portal = PortalNetwork.manager.find(event.getFrom(), 2);
    if (portal == null) {
      return;
    }

    // Cancel event
    event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockPlaceEvent(BlockPlaceEvent event) {
    // Ignore if player is sneaking
    if (event.getPlayer().isSneaking()) {
      return;
    }

    // Check if player is trying to place a portal block
    ItemMeta meta = event.getItemInHand().getItemMeta();
    if (meta != null) {
      meta.getPersistentDataContainer();
      if (meta.getPersistentDataContainer().has(Portal.PortalTypeKey, PersistentDataType.STRING)) {
        String portalType =
            meta.getPersistentDataContainer().get(Portal.PortalTypeKey, PersistentDataType.STRING);
        try {
          PortalNetwork.manager.createPortal(portalType, event.getBlockPlaced().getLocation());
        } catch (InvalidPortalException e) {
          PortalNetwork.logError(e);
        }
      }
    }

    Portal portal = PortalNetwork.manager.find(event.getBlock().getLocation(), 2);
    if (portal == null) {
      return;
    }

    portal.handleBlockPlace();
  }
}
