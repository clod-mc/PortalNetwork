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

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class MainCommand {
  public static void register() {
    new CommandAPICommand("portalnetwork")
        .withAliases("pn")
        .withShortDescription("PortalNetwork")
        .withFullDescription(
            "A portal system that allows players to create portals that can dial each other")
        .withPermission(CommandPermission.OP)
        .withSubcommand(ReloadCommand.build())
        .withSubcommand(ListCommand.build())
        .withSubcommand(GiveCommand.build())
        .withSubcommand(HelpCommand.build())
        .withUsage(
            "/portalnetwork reload", "/portalnetwork list", "/portalnetwork give [player] [type]")
        .executes(
            (CommandSender sender, CommandArguments args) ->
                Bukkit.dispatchCommand(sender, "help portalnetwork"))
        .register();
  }
}
