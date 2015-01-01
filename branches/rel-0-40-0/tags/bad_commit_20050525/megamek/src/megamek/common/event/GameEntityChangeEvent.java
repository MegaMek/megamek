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

/**
 * Instances of this class are sent game entity is changed 
 *
 * @see GameListener
 */
public class GameEntityChangeEvent extends GameEntityEvent {

    protected Vector movePath;
    
    /**
     * Constructs new GameEntityChangeEvent
     * @param source
     * @param entity
     */
    public GameEntityChangeEvent(Object source, Entity entity) {
        this(source, entity, null);
    }
    
    /**
     * Constructs new GameEntityChangeEvent
     * @param source
     * @param entity
     * @param movePath
     */
    public GameEntityChangeEvent(Object source, Entity entity, Vector movePath) {
        super(source, entity, GAME_ENTITY_CHANGE);
        this.movePath = movePath;
    }
    
    /**
     * @return the movePath.
     */
    public Vector getMovePath() {
        return movePath;
    }
}
