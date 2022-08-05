/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.battlevalue;

import megamek.codeUtilities.MathUtility;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.LAMPilot;
import megamek.common.options.OptionsConstants;

/**
 * Utility class for obtaining BV Skill Multipliers (TM p.315).
 */
public class BvMultiplier {

    private static final double[][] bvMultipliers = new double[][] {
            {2.42, 2.31, 2.21, 2.10, 1.93, 1.75, 1.68, 1.59, 1.50},
            {2.21, 2.11, 2.02, 1.92, 1.76, 1.60, 1.54, 1.46, 1.38},
            {1.93, 1.85, 1.76, 1.68, 1.54, 1.40, 1.35, 1.28, 1.21},
            {1.66, 1.58, 1.51, 1.44, 1.32, 1.20, 1.16, 1.10, 1.04},
            {1.38, 1.32, 1.26, 1.20, 1.10, 1.00, 0.95, 0.90, 0.85},
            {1.31, 1.19, 1.13, 1.08, 0.99, 0.90, 0.86, 0.81, 0.77},
            {1.24, 1.12, 1.07, 1.02, 0.94, 0.85, 0.81, 0.77, 0.72},
            {1.17, 1.06, 1.01, 0.96, 0.88, 0.80, 0.76, 0.72, 0.68},
            {1.10, 0.99, 0.95, 0.90, 0.83, 0.75, 0.71, 0.68, 0.64},
    };

    /**
     * Returns the BV multiplier for the gunnery/piloting of the given entity's pilot (TM p.315) as well as MD
     * implants of the pilot.
     * Returns 1 if the given entity's crew is null. Special treatment is given to infantry units where
     * units unable to make anti-mek attacks use 5 as their anti-mek (piloting) value as well as LAM pilots that
     * use the average of their aero and mek values.
     *
     * @param entity The entity to get the skill modifier for
     * @return The BV multiplier for the given entity's pilot
     */
    public static double bvMultiplier(Entity entity) {
        if (entity.getCrew() == null) {
            return 1;
        }
        int gunnery = entity.getCrew().getGunnery();
        int piloting = entity.getCrew().getPiloting();

        if ((entity instanceof Infantry) && (!((Infantry) entity).canMakeAntiMekAttacks())) {
            piloting = 5;
        } else if (entity.getCrew() instanceof LAMPilot) {
            LAMPilot lamPilot = (LAMPilot) entity.getCrew();
            gunnery = (lamPilot.getGunneryMech() + lamPilot.getGunneryAero()) / 2;
            piloting = (lamPilot.getPilotingMech() + lamPilot.getPilotingAero()) / 2;
        }
        return bvImplantMultiplier(entity) * bvSkillMultiplier(gunnery, piloting);
    }

    /**
     * Returns the BV multiplier for the given gunnery and piloting values. Returns 1 for the neutral
     * values 4/5.
     *
     * @param gunnery the gunnery skill of a pilot
     * @param piloting the piloting skill of a pilot
     * @return a multiplier to the BV of whatever unit the pilot is piloting.
     */
    public static double bvSkillMultiplier(int gunnery, int piloting) {
        return bvMultipliers[MathUtility.clamp(gunnery, 0, 8)][MathUtility.clamp(piloting, 0, 8)];
    }

    /**
     * Returns the BV multiplier for any MD implants that the crew of the given entity has. When the crew
     * doesn't have any relevant MD implants, returns 1.
     *
     * @param entity The entity to get the skill modifier for
     * @return a multiplier to the BV of the given entity
     */
    public static double bvImplantMultiplier(Entity entity) {
        int level = 1;
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_PAIN_SHUNT)) {
            level = 2;
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_VDNI)) {
            level = 3;
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_BVDNI)) {
            level = 5;
        }
        return level / 4.0 + 0.75;
    }

    private BvMultiplier() { }
}
