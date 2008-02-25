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
        super(source, entity, GAME_ENTITY_CHANGE);
        this.movePath = movePath;
    }

    /**
     * @return the movePath.
     */
    public Vector<UnitLocation> getMovePath() {
        return movePath;
    }

    public String toString() {
        try {
            return getEntity().toString() + " moved to "
                    + movePath.lastElement().getCoords().toFriendlyString();
        } catch (NoSuchElementException nsee) {
            if (getEntity() != null) {
                return getEntity().toString() + " probably deployed.";
            }
        } catch (NullPointerException npe) {
        }

        return "Something happened.";
    }
}
