/*
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
package megamek.client.generator.skillGenerators;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.LandAirMek;
import megamek.common.enums.SkillLevel;

public class StratConSkillGenerator extends TotalWarfareSkillGenerator {
    /**
     * Generates random skills based on the given entity and parameters.
     *
     * @param entity The entity for which skills need to be generated.
     * @param clanPilot Determines if the entity is a clan pilot.
     * @param forceClan Determines if the force is clan.
     * @return An array of randomly generated skills based on the entity and parameters.
     */
    @Override
    public int[] generateRandomSkills(final Entity entity, final boolean clanPilot, final boolean forceClan) {
        int bonus = switch (getLevel()) {
            case ULTRA_GREEN -> 0;
            case GREEN -> 1;
            case VETERAN -> 3;
            case ELITE -> 4;
            case HEROIC -> 5;
            case LEGENDARY -> 6;
            default -> 2;
        };

        if (entity instanceof LandAirMek) {
            bonus += 1;
        }

        // this will give us a SkillLevel 1 above or below the default rate, or at the default rate
        final int roll = MathUtility.clamp(Compute.randomInt(3) - 1 + bonus, 0, 6);

        SkillLevel skillLevel = switch (roll) {
            case 0 -> SkillLevel.ULTRA_GREEN;
            case 1 -> SkillLevel.GREEN;
            case 2 -> SkillLevel.REGULAR;
            case 3 -> SkillLevel.VETERAN;
            case 4 -> SkillLevel.ELITE;
            case 5 -> SkillLevel.HEROIC;
            case 6 -> SkillLevel.LEGENDARY;
            default -> throw new IllegalStateException("Unexpected value in megamek/client/generator/skillGenerators/StratConSkillGenerator.java/generateRandomSkills: "
                    + roll);
        };

        return generateRandomSkills(skillLevel, entity, clanPilot, forceClan);
    }
}
