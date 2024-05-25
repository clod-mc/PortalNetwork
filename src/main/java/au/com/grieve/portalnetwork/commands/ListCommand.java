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

package au.com.grieve.portalnetwork.commands;

import au.com.grieve.portalnetwork.PortalNetwork;
import au.com.grieve.portalnetwork.portals.Portal;
import java.util.List;
import java.util.StringJoiner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ListCommand implements SimpleCommand {
  @Override
  public void execute(@NotNull CommandSender sender, @NotNull String[] args) throws CommandError {
    if (args.length != 0) {
      throw new CommandError("Unexpected argument(s)");
    }

    for (Portal portal : PortalNetwork.manager.getPortals()) {
      if (portal.getLocation().getWorld() == null) {
        continue;
      }

      String location =
          new StringJoiner(" ")
              .add(String.valueOf(portal.getLocation().getX()))
              .add(String.valueOf(portal.getLocation().getY()))
              .add(String.valueOf(portal.getLocation().getZ()))
              .toString();

      String worldName = portal.getLocation().getWorld().getName();
      switch (worldName) {
        case "world" -> worldName = "overworld";
        case "world_nether" -> worldName = "nether";
        case "world_the_end" -> worldName = "the_end";
      }
      String tpLocation =
          new StringJoiner(" ")
              .add(String.valueOf(portal.getLocation().getX()))
              .add(String.valueOf(portal.getLocation().getY() + 1))
              .add(String.valueOf(portal.getLocation().getZ()))
              .toString();
      String tpCommand =
          new StringJoiner(" ")
              .add("/execute")
              .add("in")
              .add(worldName)
              .add("run")
              .add("tp")
              .add(sender.getName())
              .add(tpLocation)
              .toString();

      TextComponent msg =
          Component.text("")
              .append(
                  Component.text(location)
                      .color(NamedTextColor.GREEN)
                      .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                      .hoverEvent(
                          HoverEvent.hoverEvent(
                              HoverEvent.Action.SHOW_TEXT, Component.text("teleport to portal"))))
              .append(Component.text(" "));

      if (!portal.isValid()) {
        msg = msg.append(Component.text("(invalid)").color(NamedTextColor.RED));
      } else {
        msg =
            msg.append(
                Component.text(portal.getNetwork() + ":" + portal.getAddress() + " ")
                    .color(NamedTextColor.YELLOW));
        if (portal.getDialledPortal() == null) {
          msg = msg.append(Component.text("(disconnected)").color(NamedTextColor.RED));
        } else {
          msg = msg.append(Component.text("connected:" + portal.getDialledPortal().getAddress()));
        }
      }

      sender.sendMessage(msg);
    }
  }

  @Override
  public List<String> complete(@NotNull String[] args) {
    return List.of();
  }
}
