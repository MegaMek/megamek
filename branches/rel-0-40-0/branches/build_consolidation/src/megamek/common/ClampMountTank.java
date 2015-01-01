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

/**
 * Represtents the space on a vehicle used by Battle Armor squads equiped with
 * Magnetic Clamps to attach themselves for transport. This transporter gets
 * assigned to all of a player's vehicles in the Exchange Phase if any Battle
 * Armor squad equipped with a Magnetic Clamp is on that player's side.
 *
 * @see megamek.server.Server#executePhase(int)
 * @see megamek.server.Server#checkForMagneticClamp()
 */

/* package */class ClampMountTank extends BattleArmorHandlesTank {

    // Private attributes, constants and helper functions.

    /**
     *
     */
    private static final long serialVersionUID = 593951005031815098L;
    /**
     * The set of front locations that load troopers externally.
     */
    private static final int[] EXTERIOR_LOCATIONS = { Tank.LOC_RIGHT,
            Tank.LOC_LEFT, Tank.LOC_REAR };

    /**
     * The <code>String</code> reported when the handles are in use.
     */
    private static final String NO_VACANCY_STRING = "A BA squad with magnetic clamps is loaded";

    /**
     * The <code>String</code> reported when the handles are available.
     */
    private static final String HAVE_VACANCY_STRING = "One BA-magclamp squad";

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
    @Override
    protected int[] getExteriorLocs(boolean isRear) {
        return ClampMountTank.EXTERIOR_LOCATIONS;
    }

    // Public constructors and methods.

    /**
     * Create a space to mount clamp-equipped troopers on a Mech.
     */
    public ClampMountTank() {
        // Initialize our super-class.
        super();
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
    @Override
    protected String getVacancyString(boolean isLoaded) {
        if (isLoaded) {
            return ClampMountTank.NO_VACANCY_STRING;
        }
        return ClampMountTank.HAVE_VACANCY_STRING;
    }

    @Override
    public int getCargoMpReduction() {
        return getLoadedUnits().size();
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     * <p>
     *
     * @param unit - the <code>Entity</code> to be loaded.
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
        else if (null != troopers) {
            result = false;
        }

        // The unit must be capable of doing mechanized BA
        else {
            result = ((BattleArmor)unit).countWorkingMisc(MiscType.F_MAGNETIC_CLAMP) > 0;
        }

        // Return our result.
        return result;
    }

} // End package class ClampMountTank extends BattleArmorHandlesTank
