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
import au.com.grieve.portalnetwork.portals.PortalTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GiveCommand implements SimpleCommand {
  @Override
  public void execute(@NotNull CommandSender sender, @NotNull String[] args) throws CommandError {
    if (args.length > 2) {
      throw new CommandError("Unexpected argument(s)");
    }

    Player player;
    if (args.length == 0 || args[0].isEmpty() || args[0].equals("@p")) {
      if (!(sender instanceof Player)) {
        throw new CommandError("player name required");
      }
      player = (Player) sender;
    } else {
      player = Bukkit.getPlayerExact(args[0]);
      if (player == null) {
        throw new CommandError("Unknown player: " + args[0]);
      }
    }

    String type = "nether";
    if (args.length == 2) {
      type = args[1].toLowerCase();
      if (!PortalTypes.TYPES.contains(type)) {
        throw new CommandError("Invalid portal type: " + type);
      }
    }

    ItemStack item = PortalManager.createPortalBlock(type);
    player.getInventory().addItem(item);

    if (!sender.equals(player)) {
      player.sendRichMessage("<yellow>You have received a Portal Block");
    } else {
      sender.sendRichMessage("<yellow>Giving " + player.getName() + " a Portal Block");
    }
  }

  @Override
  public List<String> complete(@NotNull String[] args) {
    if (args.length <= 1) {
      ArrayList<String> playerNames =
          new ArrayList<>(
              Bukkit.getOnlinePlayers().stream()
                  .map(Player::getName)
                  .map(String::toLowerCase)
                  .toList());
      playerNames.add("@p");
      String prefix = args.length == 0 ? "" : args[0].toLowerCase();
      return playerNames.stream()
          .filter((String name) -> name.startsWith(prefix))
          .sorted(String::compareToIgnoreCase)
          .toList();
    }
    if (args.length == 2) {
      return Stream.of("nether", "end", "hidden")
          .filter((String name) -> name.startsWith(args[1].toLowerCase()))
          .toList();
    }
    return List.of();
  }
}
