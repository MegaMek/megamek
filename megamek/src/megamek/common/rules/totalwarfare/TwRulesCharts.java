package megamek.common.rules.totalwarfare;

/*
 * Copyright (C) 2026 James Magnan (bmazur@sev.org)
 * Copyright (C) 2004-2026 The MegaMek Team. All Rights Reserved.
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


import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.rules.core.CoreRulesCharts;
import megamek.common.units.Mek;

public class TwRulesCharts extends CoreRulesCharts {

    // When falling, roll to see the new facing
    @Override
    public int getFacingForFall() {
        return Compute.d6(1) - 1;
    }

    // Punch hit location chart
    @Override
    public int getPunchHitLocation(int roll, int side, boolean quad) {
        // front punch hits
        if (side == ToHitData.SIDE_FRONT) {
            switch (roll) {
                case 1:
                    return Mek.LOC_LEFT_ARM;
                case 2:
                    return Mek.LOC_LEFT_TORSO;
                case 3:
                    return Mek.LOC_CENTER_TORSO;
                case 4:
                    return Mek.LOC_RIGHT_TORSO;
                case 5:
                    return Mek.LOC_RIGHT_ARM;
                case 6:
                    return Mek.LOC_HEAD;
            }
        }
        return getPunchHitLocationSide(roll, side, quad);
    }
}
