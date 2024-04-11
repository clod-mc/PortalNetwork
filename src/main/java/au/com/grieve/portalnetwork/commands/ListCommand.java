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

import au.com.grieve.portalnetwork.PortalManager;
import au.com.grieve.portalnetwork.PortalNetwork;
import au.com.grieve.portalnetwork.portals.BasePortal;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.StringJoiner;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;

public class ListCommand {
  public static CommandAPICommand build() {
    return new CommandAPICommand("list")
        .withShortDescription("List placed portals")
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              PortalManager portalManager = PortalNetwork.getInstance().getPortalManager();

              CommandUtil.sendMessage(
                  sender, ChatColor.AQUA, "========= [ List of Portals ] =========");

              for (BasePortal portal : portalManager.getPortals()) {
                if (portal.getLocation().getWorld() == null) {
                  continue;
                }

                String worldName = portal.getLocation().getWorld().getName();
                switch (worldName) {
                  case "world" -> worldName = "overworld";
                  case "world_nether" -> worldName = "nether";
                  case "world_the_end" -> worldName = "the_end";
                }

                StringJoiner location =
                    new StringJoiner(";")
                        .add(String.valueOf(portal.getLocation().getX()))
                        .add(String.valueOf(portal.getLocation().getY()))
                        .add(String.valueOf(portal.getLocation().getZ()));

                String tpLocation =
                    new StringJoiner(" ")
                        .add(String.valueOf(portal.getLocation().getX()))
                        .add(String.valueOf(portal.getLocation().getY() + 1))
                        .add(String.valueOf(portal.getLocation().getZ()))
                        .toString();
                String command =
                    new StringJoiner(" ")
                        .add("/execute")
                        .add("in")
                        .add(worldName)
                        .add("run")
                        .add("tp")
                        .add(sender.getName())
                        .add(tpLocation)
                        .toString();

                ComponentBuilder msg =
                    new ComponentBuilder("[" + location + "] ")
                        .color(ChatColor.GREEN)
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .event(
                            new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                new Text(new ComponentBuilder("Teleport to Portal").create())))
                        .append("")
                        .event((HoverEvent) null)
                        .event((ClickEvent) null);

                if (!portal.isValid()) {
                  msg.append("[invalid]").color(ChatColor.RED);
                } else {
                  msg.append(portal.getNetwork() + ":" + portal.getAddress() + " ")
                      .color(ChatColor.YELLOW);
                  if (portal.getDialledPortal() == null) {
                    msg.append("[disconnected]").color(ChatColor.RED);
                  } else {
                    msg.append("connected:" + portal.getDialledPortal().getAddress());
                  }
                }

                sender.spigot().sendMessage(msg.create());
              }

              CommandUtil.sendMessage(
                  sender, ChatColor.AQUA, "=======================================");
            });
  }
}
