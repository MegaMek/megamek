/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

/**
 * Classes that implement this interface have the ability to load, carry, and
 * unload units in the game. It is anticipated that classes will exist for
 * passenger compartments, battle armor steps, Mek bays, Aerospace hangers, and
 * vehicle garages. Other possible classes include cargo bays and Dropship
 * docks.
 */

public interface Transporter extends Serializable {

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     * 
     * @param unit - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    public boolean canLoad(Entity unit);

    /**
     * Load the given unit.
     * 
     * @param unit - the <code>Entity</code> to be loaded.
     * @exception - If the unit can't be loaded, an
     *                <code>IllegalArgumentException</code> exception will be
     *                thrown.
     */
    public void load(Entity unit) throws IllegalArgumentException;

    /**
     * Get a <code>Vector</code> of the units currently loaded into this
     * payload.
     * 
     * @return A <code>Vector</code> of loaded <code>Entity</code> units.
     *         This list will never be <code>null</code>, but it may be
     *         empty. The returned <code>Vector</code> is independant from the
     *         under- lying data structure; modifying one does not affect the
     *         other.
     */
    public Vector<Entity> getLoadedUnits();

    /**
     * Unload the given unit.
     * 
     * @param unit - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contained in this space,
     *         <code>false</code> otherwise.
     */
    public boolean unload(Entity unit);

    /**
     * Return a string that identifies the unused capacity of this transporter.
     * 
     * @return A <code>String</code> meant for a human.
     */
    public String getUnusedString();

    /**
     * Determine if transported units prevent a weapon in the given location
     * from firing.
     * 
     * @param loc - the <code>int</code> location attempting to fire.
     * @param isRear - a <code>boolean</code> value stating if the given
     *            location is rear facing; if <code>false</code>, the
     *            location is front facing.
     * @return <code>true</code> if a transported unit is in the way,
     *         <code>false</code> if the weapon can fire.
     */
    public boolean isWeaponBlockedAt(int loc, boolean isRear);

    /**
     * If a unit is being transported on the outside of the transporter, it can
     * suffer damage when the transporter is hit by an attack. Currently, no
     * more than one unit can be at any single location; that same unit can be
     * "spread" over multiple locations.
     * 
     * @param loc - the <code>int</code> location hit by attack.
     * @param isRear - a <code>boolean</code> value stating if the given
     *            location is rear facing; if <code>false</code>, the
     *            location is front facing.
     * @return The <code>Entity</code> being transported on the outside at
     *         that location. This value will be <code>null</code> if no unit
     *         is transported on the outside at that location.
     */
    public Entity getExteriorUnitAt(int loc, boolean isRear);

    /**
     * @return list of all units carried externally by this transporter
     */
    public List<Entity> getExternalUnits();

    /**
     * @return the MP reduction due to cargo carried by this transporter
     */
    public int getCargoMpReduction();

} // End public interface Transporter

