package megamek.common.rules.core;

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

import megamek.common.Messages;
import megamek.common.compute.Compute;
import megamek.common.rules.RulesCharts;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;

public class CoreRulesCharts extends RulesCharts {
    // Escalating failure rules. Same as pilot numbers. Core p.111, 180
    public int escalatingFailure(int count) {
        return switch (count) {
                case 0 -> 2;
                case 1 -> 3;
                case 2 -> 5;
                case 3 -> 7;
                case 4 -> 10;
                case 5 -> 11;
                default -> Integer.MAX_VALUE;
        };
    }

    public int getFacingForFall() {
        if (Compute.d6(1) == 1) {
            return 3;
        }
        return 0;
    }
    
    // Return the names for locations
    public String getLocationName(int loc, boolean quad) {        
        switch (loc) {
            case Mek.LOC_LEFT_LEG:
                if (quad) {
                    return Messages.getString("Locations.RearLeftLeg");
                }
                return Messages.getString("Locations.LeftLeg");
            case Mek.LOC_RIGHT_LEG:
                if (quad) {
                    return Messages.getString("Locations.RearRightLeg");
                }
                return Messages.getString("Locations.RightLeg");
            case Mek.LOC_CENTER_LEG:
                return Messages.getString("Locations.CenterLeg");
            case Mek.LOC_LEFT_ARM:
                if (quad) {
                    return Messages.getString("Locations.FrontLeftLeg");
                }
                return Messages.getString("Locations.LeftArm");
            case Mek.LOC_RIGHT_ARM:
                if (quad) {
                    return Messages.getString("Locations.FrontRightLeg");
                }
                return Messages.getString("Locations.RightArm");
            case Mek.LOC_RIGHT_TORSO:
                return Messages.getString("Locations.RightTorso");
            case Mek.LOC_CENTER_TORSO:
                return Messages.getString("Locations.CenterTorso");
            case Mek.LOC_LEFT_TORSO:
                return Messages.getString("Locations.LeftTorso");
        }
        
        return "";
    }
}
