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


import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.rules.RulesC3;
import megamek.common.units.Entity;

public class CoreRulesC3 extends RulesC3 {

    /**
     * {@inheritDoc}
     * C3 can be used with ECM affecting it. Core p.198
     */
    @Override
    public int getC3RangeToUse(int range, int c3range, int c3ecmRange) {
        if ((c3range > c3ecmRange) && (range > c3ecmRange)) {
            return c3ecmRange;
        } else if (range > c3range) {
            return c3range;
        }
        return RangeType.RANGE_OUT;
    }
    
    /**
     * {@inheritDoc}
     * C3 and ECM can interact to lower values. Core p.198
     */
    @Override
    public void getC3RangeModifier(ToHitData mods, int range, int usingRange, int c3ecmRange, int c3range,
          boolean ecmAffected, Entity attacker) {
        if (usingRange == c3ecmRange && usingRange != c3range && ecmAffected) {
            // Halve the bonus, so we need to know what the original range was too.
            int rangeModifier = 0;
            if (range == RangeType.RANGE_LONG) {
                rangeModifier = attacker.getLongRangeModifier();
            } else if (range == RangeType.RANGE_MEDIUM) {
                rangeModifier = attacker.getMediumRangeModifier();
            } else if (range == RangeType.RANGE_EXTREME) {
                rangeModifier = attacker.getExtremeRangeModifier();
            }
            if ((c3ecmRange == RangeType.RANGE_SHORT) || (c3ecmRange == RangeType.RANGE_MINIMUM)) {
                rangeModifier = (rangeModifier + attacker.getShortRangeModifier()) / 2;
                mods.addModifier(rangeModifier, "short range due to C3 spotter under ECM");
            } else if (c3ecmRange == RangeType.RANGE_MEDIUM) {
                rangeModifier = (rangeModifier + attacker.getMediumRangeModifier()) / 2;
                mods.addModifier(rangeModifier, "medium range due to C3 spotter under ECM");
            } else if (c3ecmRange == RangeType.RANGE_LONG) {
                rangeModifier = (rangeModifier + attacker.getLongRangeModifier()) / 2;
                mods.addModifier(rangeModifier, "long range due to C3 spotter under ECM");
            }
        } else {
            // Normal C3 operation, no ECM
            if ((c3range == RangeType.RANGE_SHORT) || (c3range == RangeType.RANGE_MINIMUM)) {
                mods.addModifier(attacker.getShortRangeModifier(), "short range due to C3 spotter");
            } else if (c3range == RangeType.RANGE_MEDIUM) {
                mods.addModifier(attacker.getMediumRangeModifier(), "medium range due to C3 spotter");
            } else if (c3range == RangeType.RANGE_LONG) {
                mods.addModifier(attacker.getLongRangeModifier(), "long range due to C3 spotter");
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * C3 spotters require LOS to target. Core p.198
     */
    @Override
    public boolean c3SpotterLOSRequired() { return true; }
    
    /**
     * {@inheritDoc}
     * C3 can work with ECM, it is just reduced. Core p.198
     */
    @Override
    public boolean c3AllowedWithECM() { return true; }
}
