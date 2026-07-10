package megamek.common.rules.totalwarfare;
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


import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.rules.core.CoreRulesC3;
import megamek.common.units.Entity;

public class TWRulesC3 extends CoreRulesC3 {
    // ECM disrupts C3, so c3ecmRange is not used
    @Override
    public int getC3RangeToUse(int range, int c3range, int c3ecmRange) {
        if (range > c3range) {
            return c3range;
        }
        return RangeType.RANGE_OUT;
    }

    // C3 disrupted by ECM is ignored. Return the unaffected ECM range
    @Override
    public void getC3RangeModifier(ToHitData mods, int range, int usingRange, int c3ecmRange, int c3range,
          boolean ecmAffected, Entity attacker) {
        // Normal C3 operation, no ECM
        if ((c3range == RangeType.RANGE_SHORT) || (c3range == RangeType.RANGE_MINIMUM)) {
            mods.addModifier(attacker.getShortRangeModifier(), "short range due to C3 spotter");
        } else if (c3range == RangeType.RANGE_MEDIUM) {
            mods.addModifier(attacker.getMediumRangeModifier(), "medium range due to C3 spotter");
        } else if (c3range == RangeType.RANGE_LONG) {
            mods.addModifier(attacker.getLongRangeModifier(), "long range due to C3 spotter");
        }
    }

    // C3 spotters don't require LOS to target
    @Override
    public boolean c3SpotterLOSRequired() { return false; }

    // ECM disrupts C3.
    @Override
    public boolean c3AllowedWithECM() { return false; }
}
