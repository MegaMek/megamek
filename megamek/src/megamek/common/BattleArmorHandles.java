/*
 * MegaMek - Copyright (C) 2002-2005 Ben Mazur (bmazur@sev.org)
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

import java.util.List;
import java.util.Vector;

import megamek.common.annotations.Nullable;

/**
 * Represents a set of handles on an OmniMech used by Battle Armor units
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
    protected int troopers = Entity.NONE;
    transient Game game;

    /**
     * The set of front locations that load troopers externally.
     */
    private static final int[] EXTERIOR_LOCATIONS_FRONT =
        { Mech.LOC_RT, Mech.LOC_LT };

    /**
     * The set of rear locations that load troopers externally.
     */
    private static final int[] EXTERIOR_LOCATIONS_REAR =
        { Mech.LOC_CT, Mech.LOC_RT, Mech.LOC_LT };

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
     * Get the exterior locations that a loaded squad covers.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @param isRear
     *            - a <code>boolean</code> value stating if the given location
     *            is rear facing; if <code>false</code>, the location is front
     *            facing.
     * @return an array of <code>int</code> listing the exterior locations.
     */
    protected int[] getExteriorLocs(boolean isRear) {
        if (isRear) {
            return BattleArmorHandles.EXTERIOR_LOCATIONS_REAR;
        }
        return BattleArmorHandles.EXTERIOR_LOCATIONS_FRONT;
    }

    /**
     * Get the <code>String</code> to report the presence (or lack thereof) of a
     * loaded squad of Battle Armor troopers.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @param isLoaded
     *            - a <code>boolean</code> that indicates that troopers are
     *            currently loaded (if the value is <code>true</code>) or not
     *            (if the value is <code>false</code>).
     * @return a <code>String</code> describing the occupancy state of this
     *         transporter.
     */
    protected String getVacancyString(boolean isLoaded) {
        if (isLoaded) {
            return BattleArmorHandles.NO_VACANCY_STRING;
        }
        return BattleArmorHandles.HAVE_VACANCY_STRING;
    }

    // Public constructors and methods.

    /**
     * Create a set of handles.
     */
    public BattleArmorHandles() {
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     * <p>
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        // Assume that we can carry the unit.
        boolean result = true;

        // Only BattleArmor can be carried in BattleArmorHandles.
        if (!(unit instanceof BattleArmor)) {
            result = false;
        }

        // We must have enough space for the new troopers.
        else if (troopers != Entity.NONE) {
            result = false;
        }

        // The unit must be capable of doing mechanized BA
        else {
            result = ((BattleArmor) unit).canDoMechanizedBA();
        }

        // Return our result.
        return result;
    }

    /**
     * Load the given unit.
     *
     * @param unit the <code>Entity</code> to be loaded.
     * @throws IllegalArgumentException If the unit can't be loaded
     */
    @Override
    public final void load(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " onto this OmniMech.");
        }

        // Assign the unit as our carried troopers.
        troopers = unit.getId();
    }

    /**
     * Get a <code>List</code> of the units currently loaded into this payload.
     *
     * @return A <code>List</code> of loaded <code>Entity</code> units. This
     *         list will never be <code>null</code>, but it may be empty. The
     *         returned <code>List</code> is independant from the under- lying
     *         data structure; modifying one does not affect the other.
     */
    @Override
    public final Vector<Entity> getLoadedUnits() {
        // Return a list of our carried troopers.
        Vector<Entity> units = new Vector<>(1);
        if (troopers != Entity.NONE) {
            Entity entity = game.getEntity(troopers);
            
            if (entity != null) {
                units.addElement(entity);
            }
        }
        return units;
    }

    /**
     * Unload the given unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be unloaded.
     * @return <code>true</code> if the unit was contain is loadeded in this
     *         space, <code>false</code> otherwise.
     */
    @Override
    public final boolean unload(Entity unit) {
        // Are we carrying the unit?
        Entity trooper = game.getEntity(troopers);
        if ((trooper == null) || !trooper.equals(unit)) {
            // Nope.
            return false;
        }

        // Remove the troopers.
        troopers = Entity.NONE;
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
    @Override
    public final String getUnusedString() {
        return getVacancyString(troopers != Entity.NONE);
    }

    @Override
    public double getUnused() {
        if (troopers == Entity.NONE) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void resetTransporter() {
        troopers = Entity.NONE;
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
    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        // Assume that the weapon is not blocked.
        boolean result = false;

        // The weapon can only be blocked if we are carrying troopers.
        Entity trooper = game.getEntity(troopers);
        if (null != trooper) {

            // Is the relevant trooper alive?
            int tloc = BattleArmor.LOC_SQUAD;
            switch (loc) {
                case Mech.LOC_CT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_5 : BattleArmor.LOC_TROOPER_6;
                    break;
                case Mech.LOC_LT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_4 : BattleArmor.LOC_TROOPER_2;
                    break;
                case Mech.LOC_RT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_3 : BattleArmor.LOC_TROOPER_1;
                    break;
            }
            if ((trooper.locations() > tloc) && (trooper.getInternal(tloc) > 0)) {
                result = true;
            }
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
     * @param loc
     *            - the <code>int</code> location hit by attack.
     * @param isRear
     *            - a <code>boolean</code> value stating if the given location
     *            is rear facing; if <code>false</code>, the location is front
     *            facing.
     * @return The <code>Entity</code> being transported on the outside at that
     *         location. This value will be <code>null</code> if no unit is
     *         transported on the outside at that location.
     * @see megamek.common.BattleArmorHandles#getExteriorLocs(boolean)
     */
    @Override
    public final @Nullable Entity getExteriorUnitAt(int loc, boolean isRear) {
        return isWeaponBlockedAt(loc, isRear) ? game.getEntity(troopers) : null;
    }

    @Override
    public final List<Entity> getExternalUnits() {
        return getLoadedUnits();
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return 0;
    }
    
    @Override
    public String toString() {
        return "BattleArmorHandles - troopers:" + troopers;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }
}
