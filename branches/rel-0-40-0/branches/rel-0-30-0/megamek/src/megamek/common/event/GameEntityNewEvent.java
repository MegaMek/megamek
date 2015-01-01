/*
 * MegaMek - Copyright (C) 2005,2006 Ben Mazur (bmazur@sev.org)
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
    static final long serialVersionUID = -7053992450656518976L;

    protected Vector entities; 

    /**
     * 
     * @param source
     * @param entity
     */
    public GameEntityNewEvent(Object source, Entity entity) {
        super(source,GAME_ENTITY_NEW);
        entities = new Vector();
        entities.addElement(entity);
    }

    /**
     * 
     * @param source
     * @param entities
     */
    public GameEntityNewEvent(Object source, Vector entities) {
        super(source,GAME_ENTITY_NEW);
        this.entities = entities;
    }
    
    public Vector GetEntities(){
        return entities;
    }
}
