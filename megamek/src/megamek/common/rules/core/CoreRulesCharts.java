package megamek.common.rules.core;

/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.rules.RulesCharts;
import megamek.common.units.Mek;

public class CoreRulesCharts extends RulesCharts {

    /**
     * {@inheritDoc}
     * Falls are always forwards unless you land on your back. Core p.115
     */
    @Override
    public int getFacingForFall() {
        if (Compute.d6(1) == 1) {
            return 3;
        }
        return 0;
    }

     /**
     * {@inheritDoc}
     * Core p.81
     */
     @Override
     public int getPunchHitLocation(int roll, int side, boolean quad) {
        // front punch hits
        boolean rear = (side == ToHitData.SIDE_REAR);
        if (side == ToHitData.SIDE_FRONT || side == ToHitData.SIDE_REAR) {
            switch (roll) {
                case 1:
                    return (quad && rear) ? Mek.LOC_RIGHT_LEG : Mek.LOC_RIGHT_ARM;
                case 2:
                    return Mek.LOC_RIGHT_TORSO;
                case 3:
                    return Mek.LOC_CENTER_TORSO;
                case 4:
                    return Mek.LOC_LEFT_TORSO;
                case 5:
                    return (quad && rear) ? Mek.LOC_LEFT_LEG : Mek.LOC_LEFT_ARM;
                case 6:
                    return Mek.LOC_HEAD;
            }
        }
        return getPunchHitLocationSide(roll, side, quad);
    }
}
