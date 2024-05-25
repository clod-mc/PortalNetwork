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

package au.com.grieve.portalnetwork.portals;

import au.com.grieve.portalnetwork.PortalNetwork;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Portal {
  public static final NamespacedKey PortalTypeKey =
      new NamespacedKey(PortalNetwork.instance, "portal_type");

  static final List<Material> WOOL_MAPPINGS =
      List.of(
          Material.WHITE_WOOL,
          Material.ORANGE_WOOL,
          Material.MAGENTA_WOOL,
          Material.LIGHT_BLUE_WOOL,
          Material.YELLOW_WOOL,
          Material.LIME_WOOL,
          Material.PINK_WOOL,
          Material.GRAY_WOOL,
          Material.LIGHT_GRAY_WOOL,
          Material.CYAN_WOOL,
          Material.PURPLE_WOOL,
          Material.BLUE_WOOL,
          Material.BROWN_WOOL,
          Material.GREEN_WOOL,
          Material.RED_WOOL,
          Material.BLACK_WOOL);
  static final List<Material> GLASS_MAPPINGS =
      List.of(
          Material.WHITE_STAINED_GLASS,
          Material.ORANGE_STAINED_GLASS,
          Material.MAGENTA_STAINED_GLASS,
          Material.LIGHT_BLUE_STAINED_GLASS,
          Material.YELLOW_STAINED_GLASS,
          Material.LIME_STAINED_GLASS,
          Material.PINK_STAINED_GLASS,
          Material.GRAY_STAINED_GLASS,
          Material.LIGHT_GRAY_STAINED_GLASS,
          Material.CYAN_STAINED_GLASS,
          Material.PURPLE_STAINED_GLASS,
          Material.BLUE_STAINED_GLASS,
          Material.BROWN_STAINED_GLASS,
          Material.GREEN_STAINED_GLASS,
          Material.RED_STAINED_GLASS,
          Material.BLACK_STAINED_GLASS);

  final String type;
  final Location location;
  Integer network;
  Integer address;

  boolean valid = false;
  BlockVector left;
  BlockVector right;
  Portal dialledPortal;

  public Portal(@NotNull String type, @NotNull Location location) {
    this.type = type;
    this.location = location;
    this.update();
  }

  public abstract void activate();

  public abstract void deactivate();

  // Load configuration from manager for this portal type
  public Location getLocation() {
    return this.location.clone();
  }

  public String getType() {
    return this.type;
  }

  protected void setValid(boolean state) {
    this.dial(null);
    this.valid = state;
    this.updateBlock();
  }

  // Update Portal block based upon state
  protected void updateBlock() {
    if (this.valid) {
      if (this.dialledPortal != null) {
        this.location.getBlock().setType(GLASS_MAPPINGS.get(this.dialledPortal.getAddress()));
      } else {
        this.location.getBlock().setType(Material.BEACON);
      }
    } else {
      this.location.getBlock().setType(Material.GOLD_BLOCK);
    }
  }

  // Update Portal
  public void update() {
    // Check that wool only appears on 3 sides
    List<Location> blocks =
        Arrays.asList(
            this.location.clone().add(1, 0, 0),
            this.location.clone().add(0, 0, 1),
            this.location.clone().add(-1, 0, 0),
            this.location.clone().add(0, 0, -1));

    int count = 0;
    int nonIdx = -1;

    for (int idx = 0; idx < blocks.size(); idx++) {
      if (Tag.WOOL.isTagged(blocks.get(idx).getBlock().getType())) {
        count += 1;
      } else {
        nonIdx = idx;
      }
    }

    if (count != 3 || nonIdx == -1) {
      this.setValid(false);
      PortalNetwork.manager.reindexPortal(this);
      return;
    }

    // Determine address block. It should be opposite non_idx
    Location addressBlock = blocks.get((nonIdx + 2) % 4);
    this.address = WOOL_MAPPINGS.indexOf(addressBlock.getBlock().getType());
    this.location.setDirection(this.location.toVector().subtract(addressBlock.toVector()));

    // Net block is previous and next to non_idx
    Location leftBlock = blocks.get(((nonIdx - 1) % 4 + 4) % 4);
    Location rightBlock = blocks.get(((nonIdx + 1) % 4 + 4) % 4);
    this.network =
        (WOOL_MAPPINGS.indexOf(leftBlock.getBlock().getType()) << 4)
            + WOOL_MAPPINGS.indexOf(rightBlock.getBlock().getType());

    // If address and network already exist pop out the address block
    Portal p = PortalNetwork.manager.find(this.network, this.address);
    if (p != null && p != this) {
      Material material = addressBlock.getBlock().getType();
      addressBlock.getBlock().setType(Material.AIR);
      if (addressBlock.getWorld() != null) {
        addressBlock.getWorld().dropItemNaturally(addressBlock, new ItemStack(material));
      }
      this.setValid(false);
      PortalNetwork.manager.reindexPortal(this);
      return;
    }

    // Get Width of portal by counting obsidian blocks to a max of 10 each direction
    Vector leftUnitVector = leftBlock.toVector().subtract(this.location.toVector()).normalize();
    this.left = leftUnitVector.toBlockVector();
    for (int i = 0; i < 10; i++) {
      Vector testLeft = this.left.clone().add(leftUnitVector);
      if (this.location.clone().add(testLeft).getBlock().getType() != Material.OBSIDIAN) {
        break;
      }
      this.left = testLeft.toBlockVector();
    }

    Vector rightUnitVector = rightBlock.toVector().subtract(this.location.toVector()).normalize();
    this.right = rightUnitVector.toBlockVector();
    for (int i = 0; i < 10; i++) {
      Vector testRight = this.right.clone().add(rightUnitVector);
      if (this.location.clone().add(testRight).getBlock().getType() != Material.OBSIDIAN) {
        break;
      }
      this.right = testRight.toBlockVector();
    }

    this.setValid(true);
    PortalNetwork.manager.reindexPortal(this);
  }

  // Return portal width
  public int getWidth() {
    if (!this.valid) {
      return 1;
    }
    int width = (int) this.left.distance(this.right);
    return width + 1;
  }

  // Return Portal height
  public int getHeight() {
    if (!this.valid) {
      return 1;
    }
    return (int) Math.ceil(this.getWidth() / 2f) + 2;
  }

  public boolean dial(@Nullable Integer address) {
    if (address == null) {
      this.dial(null, null);
      return true;
    }

    if (!this.valid) {
      return false;
    }

    Portal portal = PortalNetwork.manager.find(this.network, address);
    if (portal == null) {
      return false;
    }

    this.dial(portal, null);
    return true;
  }

  public void dial(@Nullable Portal portal, @Nullable Portal from) {
    if (portal == null) {
      if (this.dialledPortal == null) {
        return;
      }

      // If we are not connected to from we will get our dialed to undial
      if (from != this.dialledPortal) {
        this.dialledPortal.dial(null, this);
      }

      this.dialledPortal = null;
      this.deactivate();
      return;
    }

    // Dialing

    // Already Dialed to portal?
    if (this.dialledPortal == portal) {
      return;
    }

    // If we are not connected to from we will get our dialed to undial
    if (this.dialledPortal != null && from != this.dialledPortal) {
      this.dialledPortal.dial(null, this);
    }

    // If portal is not from we will get it to dial us
    if (portal != from) {
      portal.dial(this, this);
    }

    this.dialledPortal = portal;
    this.activate();
  }

  // Dial next available address, otherwise we deactivate.
  public void dialNext() {
    if (!this.valid) {
      return;
    }

    int startAddress = this.dialledPortal == null ? 16 : this.dialledPortal.getAddress();

    for (int i = 1; i < 17; i++) {
      int checkAddress = (startAddress + i) % 17;

      if (checkAddress == this.address) {
        continue;
      }

      // Address 16 to terminal call
      if (checkAddress == 16) {
        break;
      }

      if (this.dial(checkAddress)) {
        return;
      }
    }

    // Deactivate
    this.dial(null);
  }

  // Return an iterator over the portal part of the portal
  public @NotNull Iterator<BlockVector> getPortalIterator() {

    final int maxWidth = this.getWidth();
    final int maxHeight = this.getHeight();

    return new Iterator<>() {
      int width = 1;
      int height = 1;
      BlockVector next;

      private BlockVector getNext() throws NoSuchElementException {
        if (!Portal.this.isValid()) {
          throw new NoSuchElementException();
        }

        while (this.width < maxWidth - 1) {
          if (this.height < maxHeight - 1) {
            Location check =
                Portal.this
                    .location
                    .clone()
                    .add(Portal.this.left)
                    .add(Portal.this.right.clone().normalize().multiply(this.width));

            // If this location is blocked we continue
            if (check.clone().add(new Vector(0, 1, 0).multiply(this.height)).getBlock().getType()
                == Material.OBSIDIAN) {
              break;
            }

            Location ret = check.clone().add(new Vector(0, 1, 0).multiply(this.height));

            // Something blocking above us? We will reset next round
            if (check
                    .clone()
                    .add(new Vector(0, 1, 0).multiply(this.height + 1))
                    .getBlock()
                    .getType()
                == Material.OBSIDIAN) {
              this.height = 1;
              this.width++;
            } else {
              this.height++;
            }

            return ret.toVector().toBlockVector();
          }
          this.width++;
          this.height = 1;
        }
        throw new NoSuchElementException();
      }

      @Override
      public boolean hasNext() {
        if (this.next == null) {
          try {
            this.next = this.getNext();
          } catch (NoSuchElementException e) {
            return false;
          }
        }
        return true;
      }

      @Override
      public BlockVector next() {
        if (this.hasNext()) {
          BlockVector ret = this.next;
          this.next = null;
          return ret;
        }
        throw new NoSuchElementException();
      }
    };
  }

  // Return an iterator over the portal base
  public @NotNull Iterator<BlockVector> getPortalBaseIterator() {
    final int maxWidth = this.getWidth();

    return new Iterator<>() {
      int width = 0;
      BlockVector next;

      private BlockVector getNext() throws NoSuchElementException {
        if (!Portal.this.isValid()) {
          // A non-valid portal just returns its own location
          if (this.width == 0) {
            this.width++;
            return Portal.this.location.toVector().toBlockVector();
          }
          throw new NoSuchElementException();
        }

        if (this.width < maxWidth) {
          Location ret =
              Portal.this
                  .location
                  .clone()
                  .add(Portal.this.left)
                  .add(Portal.this.right.clone().normalize().multiply(this.width));
          this.width++;
          return ret.toVector().toBlockVector();
        }

        // Address block.
        if (this.width == maxWidth) {
          Location ret = Portal.this.location.clone().subtract(Portal.this.location.getDirection());
          this.width++;
          return ret.toVector().toBlockVector();
        }

        throw new NoSuchElementException();
      }

      @Override
      public boolean hasNext() {
        if (this.next == null) {
          try {
            this.next = this.getNext();
          } catch (NoSuchElementException e) {
            return false;
          }
        }

        return true;
      }

      @Override
      public BlockVector next() {
        if (this.hasNext()) {
          BlockVector ret = this.next;
          this.next = null;
          return ret;
        }

        throw new NoSuchElementException();
      }
    };
  }

  // Return an iterator over the portal frame
  public @NotNull Iterator<BlockVector> getPortalFrameIterator() {
    final int maxWidth = this.getWidth();
    final int maxHeight = this.getHeight();

    return new Iterator<>() {
      int width = 0;
      int height = 1;
      BlockVector next;

      private BlockVector getNext() throws NoSuchElementException {
        if (!Portal.this.isValid()) {
          throw new NoSuchElementException();
        }

        while (this.width < maxWidth) {
          while (this.height < maxHeight) {
            Location check =
                Portal.this
                    .location
                    .clone()
                    .add(Portal.this.left)
                    .add(Portal.this.right.clone().normalize().multiply(this.width));

            // If this location is blocked we continue
            if (check.clone().add(new Vector(0, 1, 0).multiply(this.height)).getBlock().getType()
                == Material.OBSIDIAN) {
              break;
            }

            // If we are on either end of the portal, then every height is part of the frame unless
            // blocked
            if (this.width == 0 || this.width == maxWidth - 1) {
              Location ret = check.clone().add(new Vector(0, 1, 0).multiply(this.height));
              if (check
                      .clone()
                      .add(new Vector(0, 1, 0).multiply(this.height + 1))
                      .getBlock()
                      .getType()
                  == Material.OBSIDIAN) {
                this.height = 1;
                this.width++;
              } else {
                this.height++;
              }
              return ret.toVector().toBlockVector();
            }

            // Max height is frame
            if (this.height == maxHeight - 1) {
              Location ret = check.clone().add(new Vector(0, 1, 0).multiply(this.height));
              this.height = 1;
              this.width++;
              return ret.toVector().toBlockVector();
            }

            // Something blocking above us? We don't draw frame
            if (check
                    .clone()
                    .add(new Vector(0, 1, 0).multiply(this.height + 1))
                    .getBlock()
                    .getType()
                == Material.OBSIDIAN) {
              break;
            }

            // Else
            this.height++;
          }
          this.width++;
          this.height = 1;
        }
        throw new NoSuchElementException();
      }

      @Override
      public boolean hasNext() {
        if (this.next == null) {
          try {
            this.next = this.getNext();
          } catch (NoSuchElementException e) {
            return false;
          }
        }

        return true;
      }

      @Override
      public BlockVector next() {
        if (this.hasNext()) {
          BlockVector ret = this.next;
          this.next = null;
          return ret;
        }

        throw new NoSuchElementException();
      }
    };
  }

  public void handlePlayerInteract(PlayerInteractEvent event) {
    if (event.getClickedBlock() == null || !this.valid) {
      return;
    }
    // If its not our base we are not interested
    if (Streams.stream(this.getPortalBaseIterator())
        .noneMatch(
            (BlockVector blockVector) ->
                event
                    .getClickedBlock()
                    .getLocation()
                    .toVector()
                    .toBlockVector()
                    .equals(blockVector))) {
      return;
    }

    event.setCancelled(true);

    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }

    // Player has right clicked portal so lets dial next address if any, else deactivate
    if (this.valid) {
      this.dialNext();
      PortalNetwork.manager.save();
    }
  }

  // Return new position and velocity of an entity to dialled portal
  PositionVelocity calculatePosition(Entity entity) {
    if (this.getDialledPortal() == null) {
      return null;
    }

    Location to = entity.getLocation().clone().add(entity.getVelocity());

    // teleport to relative portal position

    Location fromPortalLocation = this.getLocation().add(new Vector(0.5, 0, 0.5));
    Location toPortalLocation = this.dialledPortal.getLocation().add(new Vector(0.5, 0, 0.5));
    float yawDiff = fromPortalLocation.getYaw() - toPortalLocation.getYaw();

    Vector playerRelativePosition = to.toVector().subtract(fromPortalLocation.toVector());
    playerRelativePosition.rotateAroundY(Math.toRadians(yawDiff));

    Location destination = toPortalLocation.clone().add(playerRelativePosition);

    // Make sure Y is 1 block higher than dest portal
    if (destination.getY() <= this.dialledPortal.getLocation().getY()) {
      destination.setY(this.dialledPortal.getLocation().getY() + 1);
    }

    // If destination portal is not wide or tall enough we clip it
    if (destination.getY()
        > this.dialledPortal.getLocation().getY() + this.dialledPortal.getHeight() - 2) {
      destination.setY(
          this.dialledPortal.getLocation().getY() + this.dialledPortal.getHeight() - 2);
    }

    Location destinationCheck = destination.clone();
    destinationCheck.setY(this.dialledPortal.getLocation().getY());
    if (this.dialledPortal.getLocation().distance(destinationCheck)
        > ((this.dialledPortal.getWidth() - 2) / 2f)) {
      destination.setX(toPortalLocation.getX());
      destination.setZ(toPortalLocation.getZ());
    }

    destination.setYaw(entity.getLocation().getYaw() - yawDiff);
    destination.setPitch(entity.getLocation().getPitch());

    Vector oldVelocity = entity.getVelocity();
    Vector newVelocity;

    // Check if destination is unblocked else we will flip the player around
    boolean passable = true;
    for (int i = 0; i < 3; i++) {
      if (!destination
          .clone()
          .add(destination.getDirection().multiply(i + 1))
          .add(new Vector(0, 1, 0))
          .getBlock()
          .isPassable()) {
        passable = false;
        break;
      }
    }

    if (!passable) {
      destination.setYaw(destination.getYaw() + 180);
      newVelocity = oldVelocity.clone().rotateAroundY(Math.toRadians(yawDiff + 180));
    } else {
      newVelocity = oldVelocity.clone().rotateAroundY(Math.toRadians(yawDiff));
    }

    return new PositionVelocity(destination, newVelocity, yawDiff);
  }

  public void handleVehicleMove(VehicleMoveEvent event) {
    if (this.getDialledPortal() == null) {
      return;
    }

    Entity entity = event.getVehicle();

    // Dismount all passengers first.
    List<Entity> passengers = new ArrayList<>(entity.getPassengers());
    for (Entity passenger : passengers) {
      entity.removePassenger(passenger);
    }

    entity.setVelocity(event.getTo().toVector().subtract(event.getFrom().toVector()));
    PositionVelocity pv = this.calculatePosition(entity);

    entity.setVelocity(pv.velocity());
    entity.teleport(pv.location());

    // Rotate and Mount all passengers
    for (Entity passenger : passengers) {
      passenger.teleport(pv.location().clone().setDirection(pv.velocity().normalize()));
      entity.addPassenger(passenger);
    }
  }

  public void handlePlayerMove(PlayerMoveEvent event) {
    if (this.getDialledPortal() == null) {
      return;
    }

    Player player = event.getPlayer();

    // If player is a passenger take care of the vehicle and other passengers
    boolean insideVehicle = player.isInsideVehicle() && player.getVehicle() != null;
    List<Entity> passengers = new ArrayList<>();
    Entity vehicle = player.getVehicle();

    if (insideVehicle) {
      passengers.addAll(vehicle.getPassengers());
      for (Entity passenger : passengers) {
        vehicle.removePassenger(passenger);
      }
    }

    player.setVelocity(event.getTo().toVector().subtract(event.getFrom().toVector()));
    PositionVelocity pv = this.calculatePosition(player);

    player.setVelocity(pv.velocity());
    player.teleport(pv.location());

    if (insideVehicle) {
      vehicle.teleport(pv.location());
      vehicle.setVelocity(pv.velocity());
      new BukkitRunnable() {

        @Override
        public void run() {
          for (Entity passenger : passengers) {
            passenger.teleport(pv.location());
            vehicle.addPassenger(passenger);
          }
        }
      }.runTaskLater(PortalNetwork.instance, 1);
    }
  }

  public @NotNull Integer getNetwork() {
    return this.network;
  }

  public @NotNull Integer getAddress() {
    return this.address;
  }

  public boolean isValid() {
    return this.valid;
  }

  public @Nullable Portal getDialledPortal() {
    return this.dialledPortal;
  }

  record PositionVelocity(@NotNull Location location, @NotNull Vector velocity, double yawDiff) {}

  public void handleBlockBreak(BlockBreakEvent event) {
    // If it's the frame we cancel drops
    if (Streams.stream(this.getPortalFrameIterator())
        .anyMatch(
            (BlockVector blockVector) ->
                event.getBlock().getLocation().toVector().toBlockVector().equals(blockVector))) {
      event.setDropItems(false);
    }

    this.dial(null);

    new BukkitRunnable() {
      @Override
      public void run() {
        Portal.this.update();
        PortalNetwork.manager.save();
      }
    }.runTaskLater(PortalNetwork.instance, 3);
  }

  public void handleBlockBurn() {
    this.dial(null);
    new BukkitRunnable() {
      @Override
      public void run() {
        Portal.this.update();
        PortalNetwork.manager.save();
      }
    }.runTaskLater(PortalNetwork.instance, 3);
  }

  public void handleBlockExplode() {
    this.dial(null);
    new BukkitRunnable() {
      @Override
      public void run() {
        Portal.this.update();
        PortalNetwork.manager.save();
      }
    }.runTaskLater(PortalNetwork.instance, 3);
  }

  public void handleBlockIgnite() {
    this.dial(null);
    new BukkitRunnable() {
      @Override
      public void run() {
        Portal.this.update();
        PortalNetwork.manager.save();
      }
    }.runTaskLater(PortalNetwork.instance, 3);
  }

  public void handleBlockPlace() {
    this.dial(null);
    this.update();
    PortalNetwork.manager.save();
  }

  // Remove portal cleanly
  public void remove() {
    this.dial(null);
    this.deactivate();
    this.location.getBlock().setType(Material.STONE);
    PortalNetwork.manager.removePortal(this);
  }

  protected void playStartSound(@NotNull Location location) {
    location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1);
  }

  protected void playStopSound(@NotNull Location location) {
    location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1);
  }

  @Override
  public String toString() {
    String dialed =
        this.dialledPortal == null ? "[disconnected]" : this.dialledPortal.getAddress().toString();
    return getClass().getName()
        + "("
        + new StringJoiner(", ")
            .add("location=" + this.location)
            .add("left=" + this.left)
            .add("right=" + this.right)
            .add("network=" + this.network)
            .add("address=" + this.address)
            .add("dialed=" + dialed)
        + ")";
  }
}
