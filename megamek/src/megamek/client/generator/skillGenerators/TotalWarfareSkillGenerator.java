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
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.enums.SkillLevel;
import org.apache.logging.log4j.LogManager;

public class TotalWarfareSkillGenerator extends AbstractSkillGenerator {
    //region Variable Declarations
    private static final long serialVersionUID = 1120383901354362683L;

    protected static final int[][] SKILL_LEVELS = new int[][] {
            { 7, 6, 5, 4, 4, 3, 2, 1, 0, 0 },
            { 7, 7, 6, 6, 5, 4, 3, 2, 1, 0 } };
    //endregion Variable Declarations

    //region Constructors
    public TotalWarfareSkillGenerator() {
        this(SkillGeneratorMethod.TOTAL_WARFARE);
    }

    protected TotalWarfareSkillGenerator(final SkillGeneratorMethod method) {
        super(method);
    }
    //endregion Constructors

    @Override
    public int[] generateRandomSkills(final Entity entity, final boolean forceClan) {
        return generateRandomSkills(getLevel(), entity, forceClan);
    }

    protected int[] generateRandomSkills(final SkillLevel level, final Entity entity,
                                         final boolean forceClan) {
        final int bonus = determineBonus(entity, forceClan);

        final int gunneryRoll = Compute.d6(1) + bonus;
        final int pilotingRoll = Compute.d6(1) + bonus;

        final int gunneryLevel;
        final int pilotingLevel;
        switch (level) {
            case ULTRA_GREEN:
                gunneryLevel = (int) Math.ceil(gunneryRoll / 2.0);
                pilotingLevel = (int) Math.ceil(pilotingRoll / 2.0);
                break;
            case GREEN:
                gunneryLevel = (int) Math.ceil((gunneryRoll + 0.5) / 2.0);
                pilotingLevel = (int) Math.ceil((pilotingRoll + 0.5) / 2.0);
                break;
            case REGULAR:
                gunneryLevel = (int) Math.ceil(gunneryRoll / 2.0) + 2;
                pilotingLevel = (int) Math.ceil(pilotingRoll / 2.0) + 2;
                break;
            case VETERAN:
                gunneryLevel = (int) Math.ceil(gunneryRoll / 2.0) + 3;
                pilotingLevel = (int) Math.ceil(pilotingRoll / 2.0) + 3;
                break;
            case ELITE:
                gunneryLevel = (int) Math.ceil(gunneryRoll / 2.0) + 4;
                pilotingLevel = (int) Math.ceil(pilotingRoll / 2.0) + 4;
                break;
            case HEROIC:
                gunneryLevel = (int) Math.ceil(gunneryRoll / 2.0) + 5;
                pilotingLevel = (int) Math.ceil(pilotingRoll / 2.0) + 5;
                break;
            case LEGENDARY:
                gunneryLevel = (int) Math.ceil(gunneryRoll / 2.0) + 6;
                pilotingLevel = (int) Math.ceil(pilotingRoll / 2.0) + 6;
                break;
            default:
                LogManager.getLogger().error("Attempting to generate skills for unknown skill level of " + level);
                gunneryLevel = 0;
                pilotingLevel = 0;
                break;
        }

        return cleanReturn(entity, SKILL_LEVELS[0][Math.min(gunneryLevel, SKILL_LEVELS[0].length)],
                SKILL_LEVELS[1][Math.min(pilotingLevel, SKILL_LEVELS[1].length)]);
    }

    /**
     * @param entity the entity whose crew skill is being rolled
     * @param forceClan whether to force clan generation for a clan entity
     * @return the bonus to use on the Random Skills Table (Expanded) roll
     */
    protected int determineBonus(final Entity entity, final boolean forceClan) {
        if (getType().isClan() || (forceClan && entity.isClan())) {
            if (entity instanceof Mech) {
                return 2;
            } else if (entity instanceof Tank) {
                return -2;
            }
        }

        return 0;
    }
}
