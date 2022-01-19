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
package megamek.common.weapons.lrms;

import megamek.common.AmmoType;

/**
 * @author Sebastian Brocks
 */
public abstract class ExtendedLRMWeapon extends LRMWeapon {

    private static final long serialVersionUID = -1266251778897684302L;

    public ExtendedLRMWeapon() {
        super();
        ammoType = AmmoType.T_EXLRM;
        flags = flags.andNot(F_ARTEMIS_COMPATIBLE);
        minimumRange = 10;
        shortRange = 12;
        mediumRange = 22;
        longRange = 38;
        extremeRange = 44;
        maxRange = RANGE_EXT;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_STANDARD;
    }

    @Override
    public String getSortingName() {
        return "Extended LRM " + ((rackSize < 10) ? "0" + rackSize : rackSize);
    }
}
