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
import au.com.grieve.portalnetwork.exceptions.InvalidPortalException;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveCommand {
  public static CommandAPICommand build() {

    return new CommandAPICommand("give")
        .withShortDescription("Give player a portal block")
        .withOptionalArguments(new PlayerArgument("player"))
        .withOptionalArguments(new StringArgument("type"))
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              if (!(sender instanceof ConsoleCommandSender || sender instanceof Player)) {
                return;
              }

              // player arg
              Player player;
              if (args.getOptional("player").isEmpty()) {
                if (sender instanceof ConsoleCommandSender) {
                  throw CommandAPI.failWithString("When console a player name is required");
                }
                player = (Player) args.getOrDefault("player", sender);
              } else {
                player = (Player) args.get("player");
              }
              assert player != null;

              // type arg
              String type =
                  ((String) args.getOrDefault("type", "nether")).toLowerCase(Locale.ENGLISH);
              List<String> types =
                  PortalNetwork.getInstance()
                      .getPortalManager()
                      .getPortalClasses()
                      .keySet()
                      .stream()
                      .map((s) -> s.toLowerCase(Locale.ENGLISH))
                      .toList();
              if (!types.contains(type)) {
                throw CommandAPI.failWithString(
                    "Invalid Portal Type: " + args.getOrDefault("type", "?"));
              }

              // give
              try {
                ItemStack item =
                    PortalNetwork.getInstance().getPortalManager().createPortalBlock(type);
                player.getInventory().addItem(item);

                if (!sender.equals(player)) {
                  CommandUtil.sendMessage(player, "You have received a Portal Block");
                } else {
                  CommandUtil.sendMessage(sender, "Giving " + player.getName() + " a Portal Block");
                }
              } catch (InvalidPortalException e) {
                throw CommandAPI.failWithString(e.getMessage());
              }
            });
  }
}
