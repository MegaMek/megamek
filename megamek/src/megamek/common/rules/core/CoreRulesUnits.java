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

import megamek.common.rules.RulesUnits;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;

public class CoreRulesUnits extends RulesUnits {
    // Mule kicks have no additional modifier Core p.238
    public int getMuleKickModifier() { return 0; }

    // Is it immobile due to leg destruction? Core p.237 (tripod), p.239 (quad), p.90
    public boolean getDoesLegDestructionCauseImmobile(Mek mek) {
        int legsDestroyed = 0;
        for (int i = 0; i < mek.locations(); i++) {
            if (mek.locationIsLeg(i)) {
                if (mek.isLocationBad(i)) {
                    legsDestroyed++;
                }
            }
        }
        if (legsDestroyed == 2 && !(mek instanceof QuadMek)) {
            return true;
        } else if (legsDestroyed == 4 && (mek instanceof QuadMek)) {
            return true;
        }
        return false;
    }
}
