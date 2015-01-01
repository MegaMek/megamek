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
 * Instances of this class are sent when entity is added to game
 */
public class GameEntityNewEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -1223834507320730181L;
    protected Vector<Entity> entities;

    /**
     * @param source
     * @param entity
     */
    public GameEntityNewEvent(Object source, Entity entity) {
        super(source, GAME_ENTITY_NEW);
        entities = new Vector<Entity>();
        entities.addElement(entity);
    }

    /**
     * @param source
     * @param entities
     */
    public GameEntityNewEvent(Object source, Vector<Entity> entities) {
        super(source, GAME_ENTITY_NEW);
        this.entities = entities;
    }

    public Vector<Entity> GetEntities() {
        return entities;
    }

    public int getNumberOfEntities() {
        return entities.size();
    }
}
