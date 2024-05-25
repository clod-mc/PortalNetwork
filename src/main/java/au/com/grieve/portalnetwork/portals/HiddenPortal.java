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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

public class HiddenPortal extends Portal {
  public static PortalRecipe RECIPE =
      new PortalRecipe(
          List.of("OOP", "ONO", "OOO"),
          Map.of(
              'N', Material.NETHERITE_BLOCK,
              'O', Material.OBSIDIAN,
              'P', Material.ENDER_PEARL));

  public HiddenPortal(@NotNull Location location) {
    super("hidden", location);
  }

  // Activate Portal using type of portal as to what is seen/heard
  @Override
  public void activate() {
    if (!this.valid || this.dialledPortal == null || this.location.getWorld() == null) {
      return;
    }

    updateBlock();

    // Draw frame
    for (Iterator<BlockVector> it = getPortalFrameIterator(); it.hasNext(); ) {
      BlockVector loc = it.next();
      Block block = loc.toLocation(this.location.getWorld()).getBlock();
      if (block.getType() != Material.AIR && !GLASS_MAPPINGS.contains(block.getType())) {
        continue;
      }

      block.setType(GLASS_MAPPINGS.get(dialledPortal.getAddress()));
    }

    this.playStartSound(this.location);
  }

  // Deactivate Portal
  @Override
  public void deactivate() {
    if (this.location.getWorld() == null) {
      return;
    }

    // Remove frame
    for (Iterator<BlockVector> it = getPortalFrameIterator(); it.hasNext(); ) {
      BlockVector loc = it.next();
      Block block = loc.toLocation(this.location.getWorld()).getBlock();
      if (!GLASS_MAPPINGS.contains(block.getType())) {
        continue;
      }

      block.setType(Material.AIR);
    }

    updateBlock();

    this.playStopSound(this.location);
  }
}
