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

/**
 * Instances of this class are sent when Client is asked for the Map
 */
public class GameMapQueryEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -2525971548410030612L;
    protected MapSettings settings;

    /**
     * @param source
     * @param settings
     */
    public GameMapQueryEvent(Object source, MapSettings settings) {
        super(source);
        this.settings = settings;
    }

    /**
     * @return
     */
    public MapSettings getSettings() {
        return settings;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameMapQuery(this);
    }

    @Override
    public String getEventName() {
        return "Game Map Query";
    }
}
