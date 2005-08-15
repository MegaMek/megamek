/*
 * MegaMek -
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Created on July 5, 2005
 */
package megamek.common;

import java.io.Serializable;

/**
 * A simple class to specify a location and facing for a unit.
 */
public class UnitLocation implements Serializable
{
    /** The entity ID of the unit at this location. */
    private final int entityId;

    /** The coordinates of this location. */
    private final Coords coords;

    /** The facing of the unit at this location. */
    private final int facing;

    /**
     * Create a new location object.
     *
     * @param   id the unit's <code>int</code> ID number.
     * @param   coords the <code>Coords</code> of this location.
     * @param   facing the unit's <code>int</code> facing at this location.
     */
    public UnitLocation (int id, Coords coords, int facing) {
        this.entityId = id;
        this.coords = coords;
        this.facing = facing;
    }

    /**
     * Get the ID number of the entity at this location.
     *
     * @return the <code>int</code> ID of the entity.
     */
    public int getId() {
        return entityId;
    }

    /**
     * Get the coordinates of this location.
     *
     * @return the <code>Coords</code> coordinates of the location.
     */
    public Coords getCoords() {
        return coords;
    }

    /**
     * Get the facing of the entity at this location.
     *
     * @return the <code>int</code> facing of the entity.
     */
    public int getFacing() {
        return facing;
    }

}
