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
package megamek.common.weapons.capitalweapons;

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.lasers.EnergyWeapon;

/**
 * @author Jay Lawson
 */
public abstract class NLWeapon extends EnergyWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 3039645862661842495L;

    public NLWeapon() {
        super();
        atClass = CLASS_CAPITAL_LASER;
        capital = true;
        flags = flags.andNot(F_PROTO_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON);
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        int maxRange = shortAV < 4 ? AlphaStrikeElement.LONG_RANGE : AlphaStrikeElement.EXTREME_RANGE;
        return (range <= maxRange) ? shortAV : 0;
    }
}
