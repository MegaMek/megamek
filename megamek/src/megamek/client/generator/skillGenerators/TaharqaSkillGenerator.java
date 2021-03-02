/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.generator.enums.SkillGeneratorMethod;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.LandAirMech;
import megamek.common.enums.SkillLevel;

public class TaharqaSkillGenerator extends TotalWarfareSkillGenerator {
    //region Variable Declarations
    private static final long serialVersionUID = -7334417837623003013L;
    //endregion Variable Declarations

    //region Constructors
    public TaharqaSkillGenerator() {
        super(SkillGeneratorMethod.TAHARQA);
    }
    //endregion Constructors

    /**
     * The base skill level for each entity is determined separately in Taharqa's Method
     * @param entity the Entity to generate a random skill array for
     * @param forceClan forces the type to be clan if the entity is a clan unit
     * @return an integer array containing the (Gunnery, Piloting) skill values, or an alternative
     * pairing if applicable [(Gunnery, Anti-'Mech) for infantry]
     */
    @Override
    public int[] generateRandomSkills(final Entity entity, final boolean forceClan) {
        int levelBonus;
        switch (getLevel()) {
            case ULTRA_GREEN:
                levelBonus = -4;
                break;
            case GREEN:
                levelBonus = -2;
                break;
            case VETERAN:
                levelBonus = 2;
                break;
            case ELITE:
                levelBonus = 4;
                break;
            case REGULAR:
            default:
                levelBonus = 0;
                break;
        }

        if (entity instanceof LandAirMech) {
            levelBonus += 3;
        }

        final int levelRoll = Compute.d6(2) + levelBonus;
        final SkillLevel level;

        // restate level based on roll
        if (levelRoll < 1) {
            level = SkillLevel.ULTRA_GREEN;
        } else if (levelRoll < 6) {
            level = SkillLevel.GREEN;
        } else if (levelRoll < 10) {
            level = SkillLevel.REGULAR;
        } else if (levelRoll < 12) {
            level = SkillLevel.VETERAN;
        } else {
            level = SkillLevel.ELITE;
        }

        return generateRandomSkills(level, entity, forceClan);
    }
}
