/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess;

/**
 * This class stores all the calculations of probabilities given the rule set
 */
public class ProbabilityCalculator {
    //How likely am I to hit a certain location with weapons fire, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CT,LOC_RT,LOC_LT,LOC_RARM,LOC_LARM,LOC_RLEG,LOC_LLEG Defined in megamek.common.Mek
    public static final double[] hitProbabilitiesFront = { 1d / 36, 7d / 36, 5d / 36, 5d / 36, 5d / 36, 5d / 36, 4d / 36, 4d / 36 };
    public static final double[] hitProbabilitiesRightSide = { 1d / 36, 5d / 36, 7d / 36, 4d / 36, 7d / 36, 3d / 36, 7d / 36, 3d / 36 };
    public static final double[] hitProbabilitiesLeftSide = { 1d / 36, 5d / 36, 4d / 36, 7d / 36, 3d / 36, 7d / 36, 3d / 36, 7d / 36 };

    //How likely am I to hit a certain location with a punch, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CT,LOC_RT,LOC_LT,LOC_RARM,LOC_LARM,LOC_RLEG,LOC_LLEG Defined in megamek.common.Mek
    public static final double[] hitProbabilitiesPunchFront = { 1d / 6, 1d / 6, 1d /6, 1d / 6, 1d / 6, 1d / 6, 0, 0 };
    public static final double[] hitProbabilitiesPunchRightSide = { 1d / 6, 1d / 6, 2d /6, 0, 2d / 6,  0, 0, 0 };
    public static final double[] hitProbabilitiesPunchLeftSide = { 1d / 6, 1d / 6, 0, 2d / 6, 0, 2d / 6, 0, 0 };

    //How likely am I to hit a certain location with a punch, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CT,LOC_RT,LOC_LT,LOC_RARM,LOC_LARM,LOC_RLEG,LOC_LLEG Defined in megamek.common.Mek
    public static final double[] hitProbabilitiesKickFront = { 0, 0, 0, 0, 0, 0, 3d / 6, 3d / 6 };
    public static final double[] hitProbabilitiesKickRightSide = { 0, 0, 0, 0, 0, 0, 1d, 0 };
    public static final double[] hitProbabilitiesKickLeftSide = { 0, 0, 0, 0, 0, 0, 0, 1d };

    /**
     * returns the probability that hit_location (from class Mek) is hit when the
     * Mek is attacked with weapons fire from facing attackedFromFacing, with 0
     * defined as forward
     */
    static double getHitProbability(int attackedFromFacing, int hit_location) {
        if ((attackedFromFacing == 5) || (attackedFromFacing == 0) || (attackedFromFacing == 1) || (attackedFromFacing == 3)) {
            return hitProbabilitiesFront[hit_location];
        }
        if (attackedFromFacing == 2) {
            return hitProbabilitiesRightSide[hit_location];
        }
        // assume attackedFromFacing==4
        return hitProbabilitiesLeftSide[hit_location];
    }

    /**
     * returns the probability that hit_location (from class Mek) is hit when the
     * Mek is attacked with a punch from facing attackedFromFacing, with 0 defined
     * as forward
     */
    static double getHitProbability_Punch(int attackedFromFacing, int hit_location) {
        if ((attackedFromFacing == 5) || (attackedFromFacing == 0) || (attackedFromFacing == 1) || (attackedFromFacing == 3)) {
            return hitProbabilitiesPunchFront[hit_location];
        }
        if (attackedFromFacing == 2) {
            return hitProbabilitiesPunchRightSide[hit_location];
        }
        // assume attackedFromFacing==4
        return hitProbabilitiesPunchLeftSide[hit_location];
    }

    /**
     * returns the probability that hit_location (from class Mek) is hit when the
     * Mek is attacked with a kick from facing attackedFromFacing, with 0 defined as
     * forward
     */
    static double getHitProbability_Kick(int attackedFromFacing, int hit_location) {
        if ((attackedFromFacing == 5) || (attackedFromFacing == 0) || (attackedFromFacing == 1) || (attackedFromFacing == 3)) {
            return hitProbabilitiesKickFront[hit_location];
        }
        if (attackedFromFacing == 2) {
            return hitProbabilitiesKickRightSide[hit_location];
        }
        // assume attackedFromFacing==4
        return hitProbabilitiesKickLeftSide[hit_location];
    }

    /**
     * If we roll on the critical hit table, how many criticals do we expect to cause
     */
    static double getExpectedCriticalHitCount() {
        return 0.611; // (9+2*5+3)/36
    }
}
