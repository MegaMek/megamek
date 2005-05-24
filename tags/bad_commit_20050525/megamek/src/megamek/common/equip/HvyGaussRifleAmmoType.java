/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.equip;

import megamek.common.*;

public class HvyGaussRifleAmmoType extends GaussRifleAmmoType {

    public HvyGaussRifleAmmoType() {
    super(TechConstants.T_IS_LEVEL_2, GaussRifleType.HEAVY);
    }

    public int getShotDamage(Entity en, Targetable targ) {
    if (en != null && targ != null ) {
        // Get the range
        int fire_range = en.getPosition().distance(targ.getPosition());
        // Use the range to get the range id
        int range_id = range.getRangeID(fire_range);

        switch (range_id) {
        case RangeType.RANGE_MINIMUM:
        case RangeType.RANGE_SHORT:
        return 25;
        case RangeType.RANGE_MEDIUM:
        return 20;
        case RangeType.RANGE_LONG:
            case RangeType.RANGE_EXTREME: // TODO : what's the *real* value?
        return 10;
        }
    }

    // If not explicit, return variable
    return WeaponType.DAMAGE_VARIABLE;
    }

}
