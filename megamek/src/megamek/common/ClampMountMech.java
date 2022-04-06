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

/**
 * Represents the space on an standard Mech (i.e. one that is not an OmniMech)
 * used by Battle Armor squads equipped with Magnetic Clamps to attach themselves
 * for transport. This transporter gets assigned to all of a player's standard
 * Mechs in the Exchange Phase if any Battle Armor squad equipped with a
 * Magnetic Clamp is on that player's side.
 */
public class ClampMountMech extends BattleArmorHandles {
    private static final long serialVersionUID = -5687854937528642266L;

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
     * Get the <code>String</code> to report the presence (or lack thereof) of
     * a loaded squad of Battle Armor troopers. <p> Sub-classes are encouraged
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
            return ClampMountMech.NO_VACANCY_STRING;
        }
        return ClampMountMech.HAVE_VACANCY_STRING;
    }

    // Public constructors and methods.

    /**
     * Create a space to mount clamp-equipped troopers on a Mech.
     */
    public ClampMountMech() {
        // Initialize our super-class.
        super();
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return getLoadedUnits().size();
    }

    @Override
    public boolean canLoad(Entity unit) {
        // Assume that we can carry the unit.
        boolean result = true;

        // Only BattleArmor can be carried in BattleArmorHandles.
        if (!(unit instanceof BattleArmor)) {
            result = false;
        }

        // We must have enough space for the new troopers.
        else if (-1 != troopers) {
            result = false;
        }

        // The unit must be capable of doing mechanized BA
        else {
            result = ((BattleArmor) unit).hasMagneticClamps();
        }

        // Return our result.
        return result;
    }

    @Override
    public String toString() {
        return "ClampMountMech - troopers:" + troopers;
    }
} // End package class ClampMountMech implements Transporter
