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

import java.util.NoSuchElementException;
import java.util.Vector;

import megamek.common.Entity;
import megamek.common.UnitLocation;

/**
 * Instances of this class are sent game entity is changed
 * 
 * @see GameListener
 */
public class GameEntityChangeEvent extends GameEntityEvent {
    private static final long serialVersionUID = -7241101183271789555L;
    protected Vector<UnitLocation> movePath;
    protected Entity oldEntity;

    /**
     * Constructs new GameEntityChangeEvent
     * 
     * @param source
     * @param entity
     */
    public GameEntityChangeEvent(final Object source, final Entity entity) {
        this(source, entity, null);
    }

    /**
     * Constructs new GameEntityChangeEvent
     * 
     * @param source
     * @param entity
     * @param movePath
     */
    public GameEntityChangeEvent(final Object source, final Entity entity,
            final Vector<UnitLocation> movePath) {
        super(source, entity);
        oldEntity = null;
        this.movePath = movePath;
    }
    
    /**
     * Constructs new GameEntityChangeEvent, storing the entity prior to changes.
     * This old entity may be needed in certain cases, like when a Dropship is
     * taking off, since some of the old state is important.
     * 
     * @param source
     * @param entity
     * @param movePath
     */
    public GameEntityChangeEvent(final Object source, final Entity entity,
            final Vector<UnitLocation> movePath, Entity oldEntity) {
        super(source, entity);
        this.oldEntity = oldEntity;
        this.movePath = movePath;
    }

    /**
     * @return the movePath.
     */
    public Vector<UnitLocation> getMovePath() {
        return movePath;
    }

    public Entity getOldEntity() {
        return oldEntity;
    }  
    
    @Override
    public String toString() {
        if (movePath == null)
            return "There is nothing to move!";
        
        try {
            return getEntity().toString() + " moved to "
                    + movePath.lastElement().getCoords().toFriendlyString();
        } catch (NoSuchElementException nsee) {
            if (getEntity() != null) {
                return getEntity().toString() + " probably deployed.";
            }
        } catch (NullPointerException npe) {
            return "There is nothing to move!";
        }

        return "Something happened.";
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameEntityChange(this);
    }

    @Override
    public String getEventName() {
        return "Entity Change";
    }
}
