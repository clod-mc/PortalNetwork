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

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.CommandSender;

public class CommandUtil {
  protected static void sendMessage(CommandSender sender, String... lines) {
    for (String line : lines) {
      sender.spigot().sendMessage(new ComponentBuilder(line).create());
    }
  }

  protected static void sendMessage(CommandSender sender, ChatColor colour, String... lines) {
    for (String line : lines) {
      sender.spigot().sendMessage(new ComponentBuilder(line).color(colour).create());
    }
  }
}
