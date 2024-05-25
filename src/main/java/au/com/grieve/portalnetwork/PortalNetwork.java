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

package au.com.grieve.portalnetwork;

import au.com.grieve.portalnetwork.commands.CommandDispatch;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class PortalNetwork extends JavaPlugin {
  public static PortalNetwork instance;
  public static PortalManager manager;

  public PortalNetwork() {
    instance = this;
    manager = new PortalManager();
  }

  @Override
  public void onEnable() {
    manager.load();
    getServer().getPluginManager().registerEvents(new PortalEvents(), PortalNetwork.this);
    this.getServer().getCommandMap().register("pn", new CommandDispatch());
  }

  @Override
  public void onDisable() {
    manager.clear();
    CommandMap commandMap = this.getServer().getCommandMap();
    Command command = commandMap.getCommand("pn");
    if (command != null) {
      command.unregister(commandMap);
    }
  }

  // logging

  public static void logWarning(@NotNull String message) {
    instance.getLogger().warning(message);
  }

  public static void logError(@NotNull Throwable e) {
    instance
        .getLogger()
        .log(
            Level.SEVERE,
            e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(),
            e);
  }
}
