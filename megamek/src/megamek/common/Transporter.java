/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.common.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

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
    boolean canLoad(Entity unit);
    
    /**
     * Determines if this transporter can tow the given unit. By default, no.
     */
    default boolean canTow(Entity unit) { return false; }

    /**
     * Load the given unit.
     *
     * @param unit the <code>Entity</code> to be loaded.
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    void load(Entity unit) throws IllegalArgumentException;

    /**
     * Get a <code>Vector</code> of the units currently loaded into this
     * payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units.
     *         This list will never be <code>null</code>, but it may be
     *         empty. The returned <code>List</code> is independent from the
     *         underlying data structure; modifying one does not affect the
     *         other.
     */
    List<Entity> getLoadedUnits();

    /**
     * Unload the given unit.
     *
     * @param unit - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contained in this space,
     *         <code>false</code> otherwise.
     */
    boolean unload(Entity unit);

    /**
     * @return the number of unused spaces in this transporter.
     */
    double getUnused();

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    String getUnusedString();

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
    boolean isWeaponBlockedAt(int loc, boolean isRear);

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
    @Nullable Entity getExteriorUnitAt(int loc, boolean isRear);

    /**
     * @return list of all units carried externally by this transporter
     */
    List<Entity> getExternalUnits();

    /**
     * @return the MP reduction due to cargo carried by this transporter
     */
    int getCargoMpReduction(Entity carrier);

    void setGame(Game game) throws Exception;
    
    /**
     * clear out all troops listed in the transporter. Used by MHQ to reset units after game
     */
    void resetTransporter();
    
    /**
     * Returns the number of Docking Collars (hardpoints) this transporter counts as toward
     * the maximum that a JumpShip (or WS, SS) may carry. TO:AUE p.146
     *
     * @return The number of docking hardpoints this transporter counts as toward the limit.
     */
    default int hardpointCost() {
        return 0;
    }
}
