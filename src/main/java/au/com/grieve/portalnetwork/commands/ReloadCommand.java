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
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import java.io.IOException;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadCommand {
  public static CommandAPICommand build() {
    return new CommandAPICommand("reload")
        .withShortDescription("Reload Plugin")
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              try {
                PortalNetwork.getInstance().reload();
                CommandUtil.sendMessage(sender, ChatColor.YELLOW, "Reloaded PortalNetwork");
              } catch (IOException e) {
                throw CommandAPI.failWithString("Failed to reload PortalNetwork");
              }
            });
  }
}