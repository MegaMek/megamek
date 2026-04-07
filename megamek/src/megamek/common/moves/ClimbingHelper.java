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
package megamek.common.moves;

import megamek.common.equipment.MiscMounted;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

/**
 * Utility class for Mek climbing rules (TO:AR p.20).
 *
 * <p>Climbing allows a Mek to enter a hex that is 3 or more levels higher or lower
 * than the hex it occupies, provided it has at least one arm with all four actuators
 * functional and the hand free. Only walking MP can be used when climbing.</p>
 */
public final class ClimbingHelper {

    /** MP cost per level when climbing with two functional hands. */
    public static final int MP_COST_TWO_HANDS = 2;

    /** MP cost per level when climbing with one functional hand. */
    public static final int MP_COST_ONE_HAND = 3;

    /** PSR modifier applied to all piloting rolls while climbing. */
    public static final int CLIMBING_PSR_MODIFIER = 1;

    /** Additional PSR modifier when climbing with only one functional arm. */
    public static final int ONE_ARM_PSR_MODIFIER = 2;

    /** To-hit modifier applied against a climbing target (easier to hit). */
    public static final int TARGET_CLIMBING_MODIFIER = -2;

    /** Number of levels lowered per Dangle-and-Drop turn. */
    public static final int DANGLE_LEVELS_PER_TURN = 2;

    /** MP cost for dropping from a dangle position. */
    public static final int DROP_MP_COST = 4;

    private ClimbingHelper() {
        // Utility class - no instantiation
    }

    /**
     * Checks whether a specific arm on a Mek has all four actuators functional and
     * the hand is free (not holding a physical weapon or carried object).
     *
     * @param mek the Mek to check
     * @param location the arm location ({@link Mek#LOC_LEFT_ARM} or {@link Mek#LOC_RIGHT_ARM})
     * @return true if the arm is fully functional and hand is free
     */
    public static boolean isArmClimbCapable(Mek mek, int location) {
        if ((location != Mek.LOC_LEFT_ARM) && (location != Mek.LOC_RIGHT_ARM)) {
            return false;
        }

        boolean allActuatorsFunctional = mek.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, location)
              && mek.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, location)
              && mek.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, location)
              && mek.hasWorkingSystem(Mek.ACTUATOR_HAND, location);

        if (!allActuatorsFunctional) {
            return false;
        }

        // Check for carried objects in this arm
        if (mek.getCarriedObjects().containsKey(location)) {
            return false;
        }

        // Check for physical weapons (clubs/hatchets/swords) mounted in this arm
        for (MiscMounted club : mek.getClubs()) {
            if ((club.getLocation() == location) && !club.isDestroyed()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the number of arms (0, 1, or 2) eligible for climbing on the given Mek.
     *
     * @param mek the Mek to check
     * @return the number of climbable arms
     */
    public static int countClimbableArms(Mek mek) {
        int count = 0;
        if (isArmClimbCapable(mek, Mek.LOC_LEFT_ARM)) {
            count++;
        }
        if (isArmClimbCapable(mek, Mek.LOC_RIGHT_ARM)) {
            count++;
        }
        return count;
    }

    /**
     * Returns true if the given entity is a Mek capable of climbing (TO:AR p.20).
     * Requires at least one arm with all four actuators functional and hand free,
     * and the unit must not be prone or shut down.
     *
     * @param entity the entity to check
     * @return true if this entity can climb
     */
    public static boolean canClimb(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return false;
        }
        return (countClimbableArms(mek) >= 1) && !mek.isProne() && !mek.isShutDown();
    }

    /**
     * Returns a human-readable reason why the entity cannot climb, or null if climbing is possible.
     *
     * @param entity the entity to check
     * @return the reason climbing is impossible, or null if climbing is allowed
     */
    public static String getClimbingImpossibleReason(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return "Only Meks can use climbing rules.";
        }
        if (mek.isProne()) {
            return mek.getDisplayName() + " is prone and cannot climb.";
        }
        if (mek.isShutDown()) {
            return mek.getDisplayName() + " is shut down and cannot climb.";
        }
        int climbableArms = countClimbableArms(mek);
        if (climbableArms == 0) {
            return mek.getDisplayName() + " does not have a functional arm to climb with."
                  + "\n\nClimbing requires at least one arm with all four actuators"
                  + " (shoulder, upper arm, lower arm, hand) intact and the hand"
                  + " free of weapons or carried objects.";
        }
        return null;
    }

    /**
     * Returns true if the given entity can perform a Dangle-and-Drop maneuver (TO:AR p.20). Requires two arms with all
     * four actuators functional and hands free.
     *
     * @param entity the entity to check
     *
     * @return true if this entity can dangle
     */
    public static boolean canDangle(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return false;
        }
        return (countClimbableArms(mek) >= 2) && !mek.isProne() && !mek.isShutDown();
    }

    /**
     * Returns a human-readable reason why the entity cannot dangle, or null if dangling is possible.
     *
     * @param entity the entity to check
     *
     * @return the reason dangling is impossible, or null if dangling is allowed
     */
    public static String getDangleImpossibleReason(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return "Only Meks can use dangle-and-drop rules.";
        }
        if (mek.isProne()) {
            return mek.getDisplayName() + " is prone and cannot dangle.";
        }
        if (mek.isShutDown()) {
            return mek.getDisplayName() + " is shut down and cannot dangle.";
        }
        int climbableArms = countClimbableArms(mek);
        if (climbableArms < 2) {
            return mek.getDisplayName() + " needs two functional arms to dangle."
                  + "\n\nDangle-and-Drop requires both arms with all four actuators"
                  + " (shoulder, upper arm, lower arm, hand) intact and hands"
                  + " free of weapons or carried objects.";
        }
        return null;
    }

    /**
     * Returns the MP cost per level climbed for the given Mek (TO:AR p.20).
     * 2 MP per level with two functional hands, 3 MP per level with one.
     *
     * @param mek the Mek to check
     * @return the MP cost per level of climbing
     */
    public static int getClimbingMPCostPerLevel(Mek mek) {
        return (countClimbableArms(mek) >= 2) ? MP_COST_TWO_HANDS : MP_COST_ONE_HAND;
    }
}
