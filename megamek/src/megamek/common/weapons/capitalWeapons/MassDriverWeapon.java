/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

/*
 * Created on Jan 25, 2014
 *
 */
package megamek.common.weapons.capitalWeapons;

import java.io.Serial;

import megamek.common.weapons.gaussRifles.GaussWeapon;

/**
 * @author Dave Nawton
 */
public abstract class MassDriverWeapon extends GaussWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -2800123131421584210L;

    public MassDriverWeapon() {
        super();
        this.atClass = CLASS_CAPITAL_MD;
        this.capital = true;
        flags = flags.or(F_MASS_DRIVER).andNot(F_PROTO_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON);
        ;
        this.maxRange = RANGE_LONG;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_CAPITAL;
    }
}
