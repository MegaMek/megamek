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
