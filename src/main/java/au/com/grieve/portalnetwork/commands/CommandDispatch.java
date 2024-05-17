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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandDispatch extends Command {
  private static final Map<String, SimpleCommand> commands = new HashMap<>(2);

  public CommandDispatch() {
    super(
        "pn",
        "/pn <give|list|reload> [args]",
        "A portal system that allows players to create portals that can dial each other",
        List.of());
    commands.put("give", new GiveCommand());
    commands.put("list", new ListCommand());
  }

  @Override
  public boolean execute(
      @NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
    if (!(sender instanceof ConsoleCommandSender || sender instanceof Player)) {
      return false;
    }
    if (sender instanceof Player player && !player.isOp()) {
      return false;
    }
    try {
      if (args.length == 0) {
        sender.sendRichMessage("<yellow>" + this.getUsage());
      } else {
        SimpleCommand command = commands.get(args[0]);
        if (command == null) {
          throw new CommandError("Incorrect Argument For Command");
        }
        command.execute(sender, Arrays.copyOfRange(args, 1, args.length));
      }
    } catch (CommandError e) {
      sender.sendRichMessage("<red>" + e.getMessage());
    } catch (Throwable e) {
      PortalNetwork.logError(e);
      sender.sendRichMessage("<red>Internal error while handling command: " + e.getMessage());
    }

    return true;
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
      throws IllegalArgumentException {
    if (args.length <= 1) {
      String prefix = args.length == 0 ? "" : args[0];
      return commands.keySet().stream()
          .filter((String name) -> name.startsWith(prefix))
          .sorted()
          .toList();
    }
    SimpleCommand command = commands.get(args[0]);
    if (command == null) {
      throw new IllegalArgumentException("Incorrect Argument For Command");
    }
    return command.complete(Arrays.copyOfRange(args, 1, args.length));
  }
}
