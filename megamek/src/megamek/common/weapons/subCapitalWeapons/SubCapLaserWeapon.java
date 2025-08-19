/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.subCapitalWeapons;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.Mounted;
import megamek.common.weapons.lasers.EnergyWeapon;

/**
 * @author Jay Lawson
 * @since Sep 2, 2004
 */
public abstract class SubCapLaserWeapon extends EnergyWeapon {
    private static final long serialVersionUID = -4293264735637352953L;

    public SubCapLaserWeapon() {
        super();
        atClass = CLASS_CAPITAL_LASER;
        capital = true;
        subCapital = true;
        flags = flags.or(F_DIRECT_FIRE).or(F_ENERGY).andNot(F_PROTO_WEAPON);
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_SUBCAPITAL;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        int maxRange = shortAV <= 1 ? AlphaStrikeElement.LONG_RANGE : AlphaStrikeElement.MEDIUM_RANGE;
        return (range <= maxRange) ? shortAV : 0;
    }
}
