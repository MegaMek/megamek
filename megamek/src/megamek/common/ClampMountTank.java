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
 * Represtents the space on a vehicle used by Battle Armor squads
 * equiped with Magnetic Clamps to attach themselves for transport.
 * This transporter gets assigned to all of a player's vehicles
 * in the Exchange Phase if any Battle Armor squad equipped with a
 * Magnetic Clamp is on that player's side.
 *
 * @see         megamek.server.Server#executePhase(int)
 * @see         megamek.server.Server#checkForMagneticClamp()
 */

/* package */ class ClampMountTank extends ClampMountMech {

    // Private attributes, constants and helper functions.

    /**
     * The set of front locations blocked by loaded troopers.
     */
    private static final int[] BLOCKED_LOCATIONS = { Tank.LOC_FRONT,
                                                     Tank.LOC_RIGHT,
                                                     Tank.LOC_LEFT,
                                                     Tank.LOC_REAR };

    /**
     * The set of front locations that load troopers externally.
     */
    private static final int[] EXTERIOR_LOCATIONS = { Tank.LOC_FRONT,
                                                      Tank.LOC_RIGHT,
                                                      Tank.LOC_LEFT,
                                                      Tank.LOC_REAR };

    // Protected constructors and methods.

    /**
     * Get the locations blocked when a squad is loaded.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @param   isRear - a <code>boolean</code> value stating if the given
     *          location is rear facing; if <code>false</code>, the location
     *          is front facing.
     * @return  an array of <code>int</code> listing the blocked locations.
     */
    protected int[] getBlockedLocs( boolean isRear ) {
        return ClampMountTank.BLOCKED_LOCATIONS;
    }

    /**
     * Get the exterior locations that a loaded squad covers.
     * <p/>
     * Sub-classes are encouraged to override this method.
     *
     * @param   isRear - a <code>boolean</code> value stating if the given
     *          location is rear facing; if <code>false</code>, the location
     *          is front facing.
     * @return  an array of <code>int</code> listing the exterior locations.
     */
    protected int[] getExteriorLocs( boolean isRear ) {
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

} // End package class ClampMountTank extends ClampMountMech
