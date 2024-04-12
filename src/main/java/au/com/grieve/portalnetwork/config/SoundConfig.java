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

package au.com.grieve.portalnetwork.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.bukkit.Sound;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SoundConfig(
    @JsonDeserialize(using = Converter.SoundDeserializer.class) Sound start,
    @JsonDeserialize(using = Converter.SoundDeserializer.class) Sound stop) {
  public SoundConfig(Sound start, Sound stop) {
    this.start = start;
    this.stop = stop;
  }

  @Override
  public Sound start() {
    return this.start;
  }

  @Override
  public Sound stop() {
    return this.stop;
  }
}
