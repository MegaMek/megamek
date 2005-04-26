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

import java.util.Vector;

import megamek.common.Entity;

public class GameEntityRemoveEvent extends GameEntityEvent {

    protected Vector movePath;

    /**
     * @param source
     * @param type
     */
    public GameEntityRemoveEvent(Object source, Entity entity) {
        this(source, entity, null);
    }

    /**
     * @param source
     * @param type
     * @param movePath
     */
    public GameEntityRemoveEvent(Object source, Entity entity, Vector movePath) {
        super(source, entity, GAME_ENTITY_REMOVE);
        this.movePath = movePath;
    }

    /**
     * @return Returns the movePath.
     */
    public Vector getMovePath() {
        return movePath;
    }
}
