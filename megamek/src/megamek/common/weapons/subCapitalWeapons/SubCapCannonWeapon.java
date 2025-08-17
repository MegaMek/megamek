/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 * Created on Sep 2, 2004
 *
 */
package megamek.common.weapons.subCapitalWeapons;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.AmmoWeapon;

/**
 * @author Jay Lawson
 */
public abstract class SubCapCannonWeapon extends AmmoWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -4293264735637352953L;

    public SubCapCannonWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.SCC;
        atClass = CLASS_CAPITAL_AC;
        flags = flags.or(F_DIRECT_FIRE)
              .or(F_BALLISTIC)
              .andNot(F_PROTO_WEAPON)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON);
        capital = true;
        subCapital = true;
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_SUBCAPITAL;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        int maxRange = shortAV < 4 ? AlphaStrikeElement.LONG_RANGE : AlphaStrikeElement.MEDIUM_RANGE;
        return (range <= maxRange) ? shortAV : 0;
    }
}
