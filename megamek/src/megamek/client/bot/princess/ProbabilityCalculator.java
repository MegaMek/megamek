/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

/**
 * This class stores all the calculations of probabilities given the rule set
 */
public class ProbabilityCalculator {
    //How likely am I to hit a certain location with weapons fire, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CENTER_TORSO,LOC_RIGHT_TORSO,LOC_LEFT_TORSO,LOC_RIGHT_ARM,LOC_LEFT_ARM,LOC_RIGHT_LEG,LOC_LEFT_LEG Defined in megamek.common.units.Mek
    public static final double[] hitProbabilitiesFront = { 1d / 36, 7d / 36, 5d / 36, 5d / 36, 5d / 36, 5d / 36,
                                                           4d / 36, 4d / 36 };
    public static final double[] hitProbabilitiesRightSide = { 1d / 36, 5d / 36, 7d / 36, 4d / 36, 7d / 36, 3d / 36,
                                                               7d / 36, 3d / 36 };
    public static final double[] hitProbabilitiesLeftSide = { 1d / 36, 5d / 36, 4d / 36, 7d / 36, 3d / 36, 7d / 36,
                                                              3d / 36, 7d / 36 };

    //How likely am I to hit a certain location with a punch, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CENTER_TORSO,LOC_RIGHT_TORSO,LOC_LEFT_TORSO,LOC_RIGHT_ARM,LOC_LEFT_ARM,LOC_RIGHT_LEG,LOC_LEFT_LEG Defined in megamek.common.units.Mek
    public static final double[] hitProbabilitiesPunchFront = { 1d / 6, 1d / 6, 1d / 6, 1d / 6, 1d / 6, 1d / 6, 0, 0 };
    public static final double[] hitProbabilitiesPunchRightSide = { 1d / 6, 1d / 6, 2d / 6, 0, 2d / 6, 0, 0, 0 };
    public static final double[] hitProbabilitiesPunchLeftSide = { 1d / 6, 1d / 6, 0, 2d / 6, 0, 2d / 6, 0, 0 };

    //How likely am I to hit a certain location with a punch, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CENTER_TORSO,LOC_RIGHT_TORSO,LOC_LEFT_TORSO,LOC_RIGHT_ARM,LOC_LEFT_ARM,LOC_RIGHT_LEG,LOC_LEFT_LEG Defined in megamek.common.units.Mek
    public static final double[] hitProbabilitiesKickFront = { 0, 0, 0, 0, 0, 0, 3d / 6, 3d / 6 };
    public static final double[] hitProbabilitiesKickRightSide = { 0, 0, 0, 0, 0, 0, 1d, 0 };
    public static final double[] hitProbabilitiesKickLeftSide = { 0, 0, 0, 0, 0, 0, 0, 1d };

    /**
     * returns the probability that hit_location (from class Mek) is hit when the Mek is attacked with weapons fire from
     * facing attackedFromFacing, with 0 defined as forward
     */
    static double getHitProbability(int attackedFromFacing, int hit_location) {
        if ((attackedFromFacing == 5) || (attackedFromFacing == 0) || (attackedFromFacing == 1) || (attackedFromFacing
              == 3)) {
            return hitProbabilitiesFront[hit_location];
        }
        if (attackedFromFacing == 2) {
            return hitProbabilitiesRightSide[hit_location];
        }
        // assume attackedFromFacing==4
        return hitProbabilitiesLeftSide[hit_location];
    }

    /**
     * returns the probability that hit_location (from class Mek) is hit when the Mek is attacked with a punch from
     * facing attackedFromFacing, with 0 defined as forward
     */
    static double getHitProbability_Punch(int attackedFromFacing, int hit_location) {
        if ((attackedFromFacing == 5) || (attackedFromFacing == 0) || (attackedFromFacing == 1) || (attackedFromFacing
              == 3)) {
            return hitProbabilitiesPunchFront[hit_location];
        }
        if (attackedFromFacing == 2) {
            return hitProbabilitiesPunchRightSide[hit_location];
        }
        // assume attackedFromFacing==4
        return hitProbabilitiesPunchLeftSide[hit_location];
    }

    /**
     * returns the probability that hit_location (from class Mek) is hit when the Mek is attacked with a kick from
     * facing attackedFromFacing, with 0 defined as forward
     */
    static double getHitProbability_Kick(int attackedFromFacing, int hit_location) {
        if ((attackedFromFacing == 5) || (attackedFromFacing == 0) || (attackedFromFacing == 1) || (attackedFromFacing
              == 3)) {
            return hitProbabilitiesKickFront[hit_location];
        }
        if (attackedFromFacing == 2) {
            return hitProbabilitiesKickRightSide[hit_location];
        }
        // assume attackedFromFacing==4
        return hitProbabilitiesKickLeftSide[hit_location];
    }

    /**
     * If we roll on the critical hit table, how many criticalSlots do we expect to cause
     */
    static double getExpectedCriticalHitCount() {
        return 0.611; // (9+2*5+3)/36
    }
}
