/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Represtents a volume of space set aside for carrying ASFs and Small Craft
 * aboard DropShips
 */

public class DockingCollar implements Transporter {

    // Private attributes and helper functions.

    /**
     *
     */
    private static final long serialVersionUID = -4699786673513410716L;

    /**
     * The troops being carried.
     */
    /* package */Vector<Integer> troops = new Vector<Integer>();

    private boolean damaged = false;

    transient IGame game;

    /**
     * The total amount of space available for troops.
     */
    /* package */int totalSpace;

    /**
     * The current amount of space available for troops.
     */
    /* package */int currentSpace;

    // Protected constructors and methods.

    /**
     * The default constructor is only for serialization.
     */

    protected DockingCollar() {
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
    public DockingCollar(int docks) {
        totalSpace = 1;
        currentSpace = 1;
    }

    // Type is Docking Collar
    public String getType() {
        return "Docking Collar";
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
        // Assume that we cannot carry the unit.
        boolean result = false;

        /*
         * For now disable everything until I get docking worked out
         */
        if (unit instanceof Dropship) {
            // Dropships are all that collars can carry
            Dropship ds = (Dropship) unit;
            result = true;

            // If the dropship's collar is damaged, we can't mate with it.
            if (ds.isDockCollarDamaged()) {
                result = false;
            }

            // If this collar is in use, or if it's damaged, then we can't
            if ((currentSpace < 1) || isDamaged()) {
                result = false;
            }
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
            throw new IllegalArgumentException("Can not load "
                    + unit.getShortName() + " into this bay.");
        }

        // Decrement the available space.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        currentSpace -= 1;

        // Add the unit to our list of troops.
        troops.addElement(unit.getId());
    }

    // Recovery is different from loading in that it uses up a recovery slot
    // load is only used in deployment phase
    public void recover(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not recover "
                    + unit.getShortName() + " into this bay. " + currentSpace);
        }

        currentSpace -= 1;

        // Add the unit to our list of troops.
        troops.addElement(unit.getId());
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
        // Return a copy of our list of troops.
        Vector<Entity> loaded = new Vector<Entity>();
        for (int id : troops) {
            loaded.add(game.getEntity(id));
        }
        return loaded;
    }

    /**
     * get a vector of launchable units. This is different from loaded in that
     * units in recovery cannot launch
     */
    public Vector<Entity> getLaunchableUnits() {

        Vector<Entity> launchable = new Vector<Entity>();

        for (int i = 0; i < troops.size(); i++) {
            Entity nextUnit = game.getEntity(troops.elementAt(i));
            if ((nextUnit.getRecoveryTurn() == 0) && !damaged) {
                if (nextUnit instanceof Dropship) {
                    Dropship ds = (Dropship) nextUnit;
                    if (!ds.isDockCollarDamaged()) {
                        launchable.add(nextUnit);
                    }
                }
            }
        }

        return launchable;
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

        // can we unload?
        if (isDamaged()) {
            return false;
        }

        // Remove the unit if we are carrying it.
        boolean retval = troops.removeElement(unit.getId());

        // If we removed it, restore our space.
        if (retval) {
            currentSpace += 1;
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
        return "Dropship - " + currentSpace + " units";
    }

    public double getUnused() {
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

    public boolean isDamaged() {
        return damaged;
    }

    public void setDamaged(boolean b) {
        damaged = b;
    }

    public void setGame(IGame game) {
        this.game = game;
    }
    
    public void resetTransporter() {
        troops = new Vector<Integer>();
        currentSpace = totalSpace;
    }
    
} // End package class TroopSpace implements Transporter
