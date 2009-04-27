/*
 * MegaMek -
 * Copyright (C) 2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Represtents a set of handles on an OmniMech used by Battle Armor units
 * equiped with Boarding Claws to attach themselves for transport. This is
 * standard equipment on OmniMechs.
 * 
 * @see megamek.common.MechFileParser#postLoadInit
 */

/* package */class BattleArmorHandles implements Transporter {

    // Private attributes, constants and helper functions.

    /**
     * 
     */
    private static final long serialVersionUID = -7149931565043762975L;

    /**
     * The troopers being carried.
     */
    protected Entity troopers;

    /**
     * The set of front locations that load troopers externally.
     */
    private static final int[] EXTERIOR_LOCATIONS_FRONT = { Mech.LOC_RT,
            Mech.LOC_LT };

    /**
     * The set of rear locations that load troopers externally.
     */
    private static final int[] EXTERIOR_LOCATIONS_REAR = { Mech.LOC_CT,
            Mech.LOC_RT, Mech.LOC_LT };

    /**
     * The <code>String</code> reported when the handles are in use.
     */
    private static final String NO_VACANCY_STRING = "A squad is loaded";

    /**
     * The <code>String</code> reported when the handles are available.
     */
    private static final String HAVE_VACANCY_STRING = "One battle armor squad";

    // Protected constructors and methods.

    /**
     * Get the exterior locations that a loaded squad covers. <p/> Sub-classes
     * are encouraged to override this method.
     * 
     * @param isRear - a <code>boolean</code> value stating if the given
     *            location is rear facing; if <code>false</code>, the
     *            location is front facing.
     * @return an array of <code>int</code> listing the exterior locations.
     */
    protected int[] getExteriorLocs(boolean isRear) {
        if (isRear)
            return BattleArmorHandles.EXTERIOR_LOCATIONS_REAR;
        return BattleArmorHandles.EXTERIOR_LOCATIONS_FRONT;
    }

    /**
     * Get the internal name of the equipment needed to board this transporter.
     * <p/> Sub-classes are encouraged to override this method.
     * 
     * @return a <code>String</code> containing the internal name of the
     *         <code>EquipmentType</code> needed to board this transporter.
     */
    protected String getBoardingEquipment() {
        return BattleArmor.BOARDING_CLAW;
    }

    /**
     * Get the <code>String</code> to report the presence (or lack thereof) of
     * a loaded squad of Battle Armor troopers. <p/> Sub-classes are encouraged
     * to override this method.
     * 
     * @param isLoaded - a <code>boolean</code> that indicates that troopers
     *            are currently loaded (if the value is <code>true</code>) or
     *            not (if the value is <code>false</code>).
     * @return a <code>String</code> describing the occupancy state of this
     *         transporter.
     */
    protected String getVacancyString(boolean isLoaded) {
        if (isLoaded)
            return BattleArmorHandles.NO_VACANCY_STRING;
        return BattleArmorHandles.HAVE_VACANCY_STRING;
    }

    // Public constructors and methods.

    /**
     * Create a set of handles.
     */
    public BattleArmorHandles() {
        this.troopers = null;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     * <p>
     * Sub-classes should override the <code>getBoardingEquipment</code>
     * method.
     * 
     * @param unit - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     * @see megamek.common.BattleArmorHandles#getBoardingEquipment()
     */
    public final boolean canLoad(Entity unit) {
        // Assume that we can carry the unit.
        boolean result = true;

        // Only BattleArmor can be carried in BattleArmorHandles.
        if (!(unit instanceof BattleArmor)) {
            result = false;
        }

        // We must have enough space for the new troopers.
        else if (null != this.troopers) {
            result = false;
        }

        // The unit must have a Boarding Claw.
        else {

            // Walk through the unit's miscellaneous equipment.
            // Assume we don't find it.
            // Stop looking if we do find it.
            Iterator<Mounted> equipment = unit.getMisc().iterator();
            result = false;
            while (!result && equipment.hasNext()) {
                Mounted mount = equipment.next();
                EquipmentType equip = mount.getType();
                result = equip.getInternalName().equals(
                        this.getBoardingEquipment())
                        && (!equip.hasModes() || mount.curMode().equals("On")); //$NON-NLS-1$
            }

        }

        // Return our result.
        return result;
    }

    /**
     * Load the given unit.
     * 
     * @param unit - the <code>Entity</code> to be loaded.
     * @exception IllegalArgumentException - If the unit can't be loaded, an
     *                <code>IllegalArgumentException</code> exception will be
     *                thrown.
     */
    public final void load(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!this.canLoad(unit)) {
            throw new IllegalArgumentException("Can not load "
                    + unit.getShortName() + " onto this OmniMech.");
        }

        // Assign the unit as our carried troopers.
        this.troopers = unit;
    }

    /**
     * Get a <code>List</code> of the units currently loaded into this
     * payload.
     * 
     * @return A <code>List</code> of loaded <code>Entity</code> units. This
     *         list will never be <code>null</code>, but it may be empty. The
     *         returned <code>List</code> is independant from the under- lying
     *         data structure; modifying one does not affect the other.
     */
    public final Vector<Entity> getLoadedUnits() {
        // Return a list of our carried troopers.
        Vector<Entity> units = new Vector<Entity>(1);
        if (null != this.troopers) {
            units.addElement(this.troopers);
        }
        return units;
    }

    /**
     * Unload the given unit.
     * 
     * @param unit - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contain is loadeded in this
     *         space, <code>false</code> otherwise.
     */
    public final boolean unload(Entity unit) {
        // Are we carrying the unit?
        if (this.troopers == null || !this.troopers.equals(unit)) {
            // Nope.
            return false;
        }

        // Remove the troopers.
        this.troopers = null;
        return true;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     * <p>
     * Sub-classes should override the <code>getVacancyString</code> method.
     * 
     * @return A <code>String</code> meant for a human.
     * @see megamek.common.BattleArmorHandles#getUnusedString()
     */
    public final String getUnusedString() {
        return this.getVacancyString(null != this.troopers);
    }

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
     * @see megamek.common.BattleArmorHandles#getBlockedLocs(boolean)
     */
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        // Assume that the weapon is not blocked.
        boolean result = false;

        // The weapon can only be blocked if we are carrying troopers.
        if (null != this.troopers) {

            // Is the relevant trooper alive?
            int tloc = BattleArmor.LOC_SQUAD;
            switch (loc) {
                case Mech.LOC_CT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_5
                            : BattleArmor.LOC_TROOPER_6;
                    break;
                case Mech.LOC_LT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_4
                            : BattleArmor.LOC_TROOPER_2;
                    break;
                case Mech.LOC_RT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_3
                            : BattleArmor.LOC_TROOPER_1;
                    break;
            }
            if (troopers.locations() > tloc && troopers.getInternal(tloc) > 0)
                result = true;
        } // End carrying-troopers

        // Return our result.
        return result;
    }

    /**
     * If a unit is being transported on the outside of the transporter, it can
     * suffer damage when the transporter is hit by an attack. Currently, no
     * more than one unit can be at any single location; that same unit can be
     * "spread" over multiple locations.
     * <p>
     * Sub-classes should override the <code>getExteriorLocs</code> method.
     * 
     * @param loc - the <code>int</code> location hit by attack.
     * @param isRear - a <code>boolean</code> value stating if the given
     *            location is rear facing; if <code>false</code>, the
     *            location is front facing.
     * @return The <code>Entity</code> being transported on the outside at
     *         that location. This value will be <code>null</code> if no unit
     *         is transported on the outside at that location.
     * @see megamek.common.BattleArmorHandles#getExteriorLocs(boolean)
     */
    public final Entity getExteriorUnitAt(int loc, boolean isRear) {

        // Only check if we are carrying troopers.
        if (null != this.troopers) {

            // See if troopers cover that location.
            // Stop after the first match.
            int[] locs = this.getExteriorLocs(isRear);
            for (int loop = 0; loop < locs.length; loop++) {
                if (loc == locs[loop]) {
                    return this.troopers;
                }
            }

        } // End carrying-troopers

        // No troopers at that location.
        return null;
    }

    public final List<Entity> getExternalUnits() {
        ArrayList<Entity> rv = new ArrayList<Entity>(1);
        rv.add(troopers);
        return rv;
    }

    public int getCargoMpReduction() {
        return 0;
    }
} // End package class BattleArmorHandles implements Transporter
