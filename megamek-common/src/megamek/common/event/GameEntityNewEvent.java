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

import java.util.List;
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
    protected List<Entity> entities;

    /**
     * @param source
     * @param entity
     */
    public GameEntityNewEvent(Object source, Entity entity) {
        super(source);
        entities = new Vector<Entity>();
        entities.add(entity);
    }

    /**
     * @param source
     * @param entities
     */
    public GameEntityNewEvent(Object source, List<Entity> entities) {
        super(source);
        this.entities = entities;
    }

    public List<Entity> GetEntities() {
        return entities;
    }

    public int getNumberOfEntities() {
        return entities.size();
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameEntityNew(this);
    }

    @Override
    public String getEventName() {
        return "New Entities";
    }
}
