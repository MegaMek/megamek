/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package megamek.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Represtents a volume of space set aside for carrying troops and their
 * equipment under battle conditions. Typically, a component of an APC.
 */

public final class TroopSpace implements Transporter {

    // Private attributes and helper functions.

    /**
     *
     */
    private static final long serialVersionUID = 7837499891552862932L;

    /**
     * The troops being carried.
     */
    Map<Integer, Float> troops = new HashMap<>();
    
    /**
     * The total amount of space available for troops.
     */
    /* package */double totalSpace;

    /**
     * The current amount of space available for troops.
     */
    /* package */double currentSpace;

    transient IGame game;


    // Protected constructors and methods.
    /**
     * The default constructor is only for serialization.
     */
    protected TroopSpace() {
        totalSpace = 0;
        currentSpace = 0;
    }

    // Public constructors and methods.

    /**
     * Create a space for the given tonnage of troops. For this class, only the
     * weight of the troops (and their equipment) are considered; if you'd like
     * to think that they are stacked like lumber, be my guest.
     *
     * @param space
     *            - The weight of troops (in tons) this space can carry.
     */
    public TroopSpace(double space) {
        totalSpace = space;
        currentSpace = space;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    public boolean canLoad(Entity unit) {
        // Assume that we can carry the unit.
        boolean result = true;

        // Only Infantry and BattleArmor can be carried in TroopSpace.
        if (!(unit instanceof Infantry)) {
            result = false;
        }

        // We must have enough space for the new troops.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        else if (currentSpace < unit.getWeight()) {
            result = false;
        }

        // Return our result.
        return result;
    }

    /**
     * Load the given unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @exception - If the unit can't be loaded, an
     *            <code>IllegalArgumentException</code> exception will be
     *            thrown.
     */
    public void load(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " into this troop space.");
        }

        // Decrement the available space.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        currentSpace -= unit.getWeight();

        // Add the unit to our list of troops.
        troops.put(unit.getId(), unit.getWeight());

    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This
     *         list will never be <code>null</code>, but it may be empty. The
     *         returned <code>List</code> is independant from the under- lying
     *         data structure; modifying one does not affect the other.
     */
    public Vector<Entity> getLoadedUnits() {
        Vector<Entity> loaded = new Vector<Entity>();
        for (Map.Entry<Integer,Float> entry : troops.entrySet()) {
            int key = entry.getKey();
            loaded.add(game.getEntity(key));
        }

        return loaded;
    }

    /**
     * Unload the given unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contained in this space,
     *         <code>false</code> otherwise.
     */
    public boolean unload(Entity unit) {
        // Remove the unit if we are carrying it.
        boolean retval = false;
        float unloadWeight = 0;
        if (unit != null) {
            unloadWeight = troops.get(unit.getId());
        }

        // If we removed it, restore our space.
        if (troops.remove(unit.getId()) != null) {
            retval = true;
            currentSpace += unloadWeight;
        }

        // Return our status
        return retval;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    public String getUnusedString() {
        return "Troops - " + currentSpace + " tons";
    }

    public double getUnused(){
        return currentSpace;
    }

    /**
     * Determine if transported units prevent a weapon in the given location
     * from firing.
     *
     * @param loc
     *            - the <code>int</code> location attempting to fire.
     * @param isRear
     *            - a <code>boolean</code> value stating if the given location
     *            is rear facing; if <code>false</code>, the location is front
     *            facing.
     * @return <code>true</code> if a transported unit is in the way,
     *         <code>false</code> if the weapon can fire.
     */
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return false;
    }

    /**
     * If a unit is being transported on the outside of the transporter, it can
     * suffer damage when the transporter is hit by an attack. Currently, no
     * more than one unit can be at any single location; that same unit can be
     * "spread" over multiple locations.
     *
     * @param loc
     *            - the <code>int</code> location hit by attack.
     * @param isRear
     *            - a <code>boolean</code> value stating if the given location
     *            is rear facing; if <code>false</code>, the location is front
     *            facing.
     * @return The <code>Entity</code> being transported on the outside at that
     *         location. This value will be <code>null</code> if no unit is
     *         transported on the outside at that location.
     */
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    public final List<Entity> getExternalUnits() {
        ArrayList<Entity> rv = new ArrayList<Entity>(1);
        return rv;
    }

    public int getCargoMpReduction() {
        return 0;
    }

    @Override
    public String toString() {
        return "troopspace:" + totalSpace;
    }

    public void setGame(IGame game) {
        this.game = game;
    }
    
    public void resetTransporter() {
        troops = new HashMap<>();
        currentSpace = totalSpace;
    }
    
} // End package class TroopSpace implements Transporter
