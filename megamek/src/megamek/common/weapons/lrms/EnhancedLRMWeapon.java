/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.lrms;

import megamek.common.AmmoType;

/**
 * @author Sebastian Brocks
 */
public abstract class EnhancedLRMWeapon extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 8755275511561446251L;

    public EnhancedLRMWeapon() {
        super();
        flags = flags.andNot(F_PROTO_WEAPON);
        ammoType = AmmoType.T_NLRM;
    }

    @Override
    public String getSortingName() {
        return "Enhanced LRM " + ((rackSize < 10) ? "0" + rackSize : rackSize);
    }
}
