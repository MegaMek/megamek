/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM4Prototype extends CLPrototypeStreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7175957508769188051L;

    /**
     *
     */
    public CLStreakSRM4Prototype() {
        super();
        name = "Streak SRM 4 (CP)";
        setInternalName("CLStreakSRM4Prototype");
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 3.0f;
        criticals = 2;
        bv = 39;
        cost = 60000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
    }
}
