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

import au.com.grieve.portalnetwork.commands.MainCommand;
import au.com.grieve.portalnetwork.config.BlockConfig;
import au.com.grieve.portalnetwork.config.Config;
import au.com.grieve.portalnetwork.config.ItemConfig;
import au.com.grieve.portalnetwork.config.PortalConfig;
import au.com.grieve.portalnetwork.config.RecipeConfig;
import au.com.grieve.portalnetwork.config.SoundConfig;
import au.com.grieve.portalnetwork.listeners.PortalEvents;
import au.com.grieve.portalnetwork.portals.End;
import au.com.grieve.portalnetwork.portals.Hidden;
import au.com.grieve.portalnetwork.portals.Nether;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PortalNetwork extends JavaPlugin {
  private static PortalNetwork instance;

  private final File configFile = new File(getDataFolder(), "config.yml");
  private PortalManager portalManager;
  private Config configuration;

  @SuppressWarnings("unused")
  public PortalNetwork() {
    instance = this;
  }

  public static PortalNetwork getInstance() {
    return PortalNetwork.instance;
  }

  @Override
  public void onLoad() {
    // Register Commands
    CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(true));
    MainCommand.register();
  }

  @Override
  public void onEnable() {
    CommandAPI.onEnable();

    // Initialize Configs
    try {
      initConfig();
    } catch (IOException e) {
      throw new RuntimeException("Unable to load/save configuration file", e);
    }

    // Load Portal Manager
    portalManager = new PortalManager(this);
    portalManager.registerPortalClass(
        "nether",
        Nether.class,
        configuration
            .getPortal()
            .getOrDefault(
                "nether",
                new PortalConfig(
                    new ItemConfig(Material.GOLD_BLOCK, "Portal Block (nether)"),
                    new BlockConfig(Material.BEACON, Material.GOLD_BLOCK),
                    new SoundConfig(Sound.BLOCK_BEACON_ACTIVATE, Sound.BLOCK_BEACON_DEACTIVATE),
                    new RecipeConfig(
                        Stream.of(new String[] {"OOP", "ONO", "OOO"}).collect(Collectors.toList()),
                        Map.of(
                            'N', Material.NETHERITE_INGOT,
                            'O', Material.OBSIDIAN,
                            'P', Material.ENDER_PEARL)))));
    portalManager.registerPortalClass(
        "end",
        End.class,
        configuration
            .getPortal()
            .getOrDefault(
                "end",
                new PortalConfig(
                    new ItemConfig(Material.GOLD_BLOCK, "Portal Block (end)"),
                    new BlockConfig(Material.BEACON, Material.GOLD_BLOCK),
                    new SoundConfig(Sound.BLOCK_BEACON_ACTIVATE, Sound.BLOCK_BEACON_DEACTIVATE),
                    new RecipeConfig(
                        Stream.of(new String[] {"EEP", "ENE", "EEE"}).collect(Collectors.toList()),
                        Map.of(
                            'N', Material.NETHERITE_INGOT,
                            'E', Material.END_STONE,
                            'P', Material.ENDER_PEARL)))));
    portalManager.registerPortalClass(
        "hidden",
        Hidden.class,
        configuration
            .getPortal()
            .getOrDefault(
                "hidden",
                new PortalConfig(
                    new ItemConfig(Material.GOLD_BLOCK, "Portal Block (hidden)"),
                    new BlockConfig(Material.BEACON, Material.GOLD_BLOCK),
                    new SoundConfig(Sound.BLOCK_BEACON_ACTIVATE, Sound.BLOCK_BEACON_DEACTIVATE),
                    new RecipeConfig(
                        Stream.of(new String[] {"OOP", "ONO", "OOO"}).collect(Collectors.toList()),
                        Map.of(
                            'N', Material.NETHERITE_BLOCK,
                            'O', Material.OBSIDIAN,
                            'P', Material.ENDER_PEARL)))));

    new BukkitRunnable() {
      @Override
      public void run() {
        portalManager.load();

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PortalEvents(), PortalNetwork.this);
      }
    }.runTaskLater(PortalNetwork.getInstance(), 5);
  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
    if (portalManager != null) {
      portalManager.clear();
    }
    CommandAPI.onDisable();
  }

  private void initConfig() throws IOException {
    // Main Config
    if (!configFile.exists()) {
      //noinspection ResultOfMethodCallIgnored
      getDataFolder().mkdir();

      try (InputStream in = getClassLoader().getResourceAsStream("config.yml")) {
        //noinspection ConstantConditions
        Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    }

    reload();
  }

  public void reload() throws IOException {
    this.configuration = Config.load(this.configFile);
  }

  public PortalManager getPortalManager() {
    return this.portalManager;
  }
}
