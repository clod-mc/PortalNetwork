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
import au.com.grieve.portalnetwork.config.BlockConfig;
import au.com.grieve.portalnetwork.config.ItemConfig;
import au.com.grieve.portalnetwork.config.PortalConfig;
import au.com.grieve.portalnetwork.config.RecipeConfig;
import au.com.grieve.portalnetwork.config.SoundConfig;
import au.com.grieve.portalnetwork.listeners.PortalEvents;
import au.com.grieve.portalnetwork.portals.End;
import au.com.grieve.portalnetwork.portals.Hidden;
import au.com.grieve.portalnetwork.portals.Nether;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PortalNetwork extends JavaPlugin {
  public static PortalNetwork instance;

  private PortalManager portalManager;

  public PortalNetwork() {
    instance = this;
  }

  @Override
  public void onEnable() {
    this.getServer().getCommandMap().register("pn", new CommandDispatch());

    // Load Portal Manager
    portalManager = new PortalManager();
    portalManager.registerPortalClass(
        "nether",
        Nether.class,
        new PortalConfig(
            new ItemConfig(Material.GOLD_BLOCK, "Portal Block (nether)"),
            new BlockConfig(Material.BEACON, Material.GOLD_BLOCK),
            new SoundConfig(Sound.BLOCK_BEACON_ACTIVATE, Sound.BLOCK_BEACON_DEACTIVATE),
            new RecipeConfig(
                Stream.of("OOP", "ONO", "OOO").collect(Collectors.toList()),
                Map.of(
                    'N', Material.NETHERITE_INGOT,
                    'O', Material.OBSIDIAN,
                    'P', Material.ENDER_PEARL))));
    portalManager.registerPortalClass(
        "end",
        End.class,
        new PortalConfig(
            new ItemConfig(Material.GOLD_BLOCK, "Portal Block (end)"),
            new BlockConfig(Material.BEACON, Material.GOLD_BLOCK),
            new SoundConfig(Sound.BLOCK_BEACON_ACTIVATE, Sound.BLOCK_BEACON_DEACTIVATE),
            new RecipeConfig(
                Stream.of("EEP", "ENE", "EEE").collect(Collectors.toList()),
                Map.of(
                    'N', Material.NETHERITE_INGOT,
                    'E', Material.END_STONE,
                    'P', Material.ENDER_PEARL))));
    portalManager.registerPortalClass(
        "hidden",
        Hidden.class,
        new PortalConfig(
            new ItemConfig(Material.GOLD_BLOCK, "Portal Block (hidden)"),
            new BlockConfig(Material.BEACON, Material.GOLD_BLOCK),
            new SoundConfig(Sound.BLOCK_BEACON_ACTIVATE, Sound.BLOCK_BEACON_DEACTIVATE),
            new RecipeConfig(
                Stream.of("OOP", "ONO", "OOO").collect(Collectors.toList()),
                Map.of(
                    'N', Material.NETHERITE_BLOCK,
                    'O', Material.OBSIDIAN,
                    'P', Material.ENDER_PEARL))));

    new BukkitRunnable() {
      @Override
      public void run() {
        portalManager.load();
        getServer().getPluginManager().registerEvents(new PortalEvents(), PortalNetwork.this);
      }
    }.runTaskLater(this, 5);
  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
    if (portalManager != null) {
      portalManager.clear();
    }
    CommandMap commandMap = this.getServer().getCommandMap();
    Command command = commandMap.getCommand("pn");
    if (command != null) {
      command.unregister(commandMap);
    }
  }

  public PortalManager getPortalManager() {
    return this.portalManager;
  }

  // logging

  public static void logWarning(String message) {
    instance.getLogger().warning(message);
  }

  public static void logError(Throwable e) {
    instance
        .getLogger()
        .log(
            Level.SEVERE,
            e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(),
            e);
  }
}
