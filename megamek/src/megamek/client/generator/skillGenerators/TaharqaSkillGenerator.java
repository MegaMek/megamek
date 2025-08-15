/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.generator.skillGenerators;

import megamek.client.generator.enums.SkillGeneratorMethod;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.LandAirMek;
import megamek.common.enums.SkillLevel;

public class TaharqaSkillGenerator extends TotalWarfareSkillGenerator {
    //region Constructors
    public TaharqaSkillGenerator() {
        super(SkillGeneratorMethod.TAHARQA);
    }
    //endregion Constructors

    /**
     * The base skill level for each entity is determined separately in Taharqa's Method
     *
     * @param entity    the Entity to generate a random skill array for
     * @param clanPilot if the crew to generate a random skills array for are a clan crew
     * @param forceClan forces the type to be clan if the crew are a clan crew
     *
     * @return an integer array containing the (Gunnery, Piloting) skill values, or an alternative pairing if applicable
     *       [(Gunnery, Anti-'Mek) for infantry]
     */
    @Override
    public int[] generateRandomSkills(final Entity entity, final boolean clanPilot, final boolean forceClan) {
        int bonus;
        switch (getLevel()) {
            case ULTRA_GREEN:
                bonus = -4;
                break;
            case GREEN:
                bonus = -2;
                break;
            case VETERAN:
                bonus = 2;
                break;
            case ELITE:
                bonus = 4;
                break;
            case HEROIC:
                bonus = 6;
                break;
            case LEGENDARY:
                bonus = 8;
                break;
            case REGULAR:
            default:
                bonus = 0;
                break;
        }

        if (entity instanceof LandAirMek) {
            bonus += 3;
        }

        final int roll = Compute.d6(2) + bonus;

        // Calculate the level to generate at based on the roll
        final SkillLevel level;
        if (roll < 2) {
            level = SkillLevel.ULTRA_GREEN;
        } else if (roll < 6) {
            level = SkillLevel.GREEN;
        } else if (roll < 10) {
            level = SkillLevel.REGULAR;
        } else if (roll < 12) {
            level = SkillLevel.VETERAN;
        } else if (roll < 15) {
            level = SkillLevel.ELITE;
        } else if (roll < 18) {
            level = SkillLevel.HEROIC;
        } else {
            level = SkillLevel.LEGENDARY;
        }

        return generateRandomSkills(level, entity, clanPilot, forceClan);
    }
}
