/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.event;

import megamek.common.MapSettings;

public class GameMapQueryEvent extends GameEvent {

    protected MapSettings settings;

    /**
     * @param source
     * @param type
     */
    public GameMapQueryEvent(Object source, MapSettings settings) {
        super(source, GAME_MAP_QUERY);
        this.settings = settings;
    }

    /**
    * @return Returns the settings.
    */
   public MapSettings getSettings() {
      return settings;
   }
}
