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

import megamek.common.Entity;
import megamek.common.LandAirMek;

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
        int skillLevel = switch (getLevel()) {
            case ULTRA_GREEN -> 2;
            case GREEN -> 3;
            case VETERAN -> 5;
            case ELITE -> 6;
            case HEROIC -> 7;
            case LEGENDARY -> 8;
            default -> 4; // Regular
        };

        if (entity instanceof LandAirMek) {
            skillLevel += 1;
        }

        skillLevel += determineBonus(entity, clanPilot, forceClan);

        return cleanReturn(entity,
                SKILL_LEVELS[0][Math.min(skillLevel, SKILL_LEVELS[0].length)],
                SKILL_LEVELS[1][Math.min(skillLevel, SKILL_LEVELS[1].length)]);
    }
}
