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
import megamek.client.generator.enums.SkillGeneratorType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.enums.SkillLevel;

public class TotalWarfareSkillGenerator extends AbstractSkillGenerator {
    //region Variable Declarations
    private static final long serialVersionUID = 1120383901354362683L;

    private static final int[][] SKILL_LEVELS = new int[][] { { 7, 6, 5, 4, 4, 3, 2, 1, 0 },
            { 7, 7, 6, 6, 5, 4, 3, 2, 1 } };
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
        final SkillGeneratorType type = (forceClan && entity.isClan()) ? SkillGeneratorType.CLAN : getType();

        // First, we determine  the bonus
        int bonus = 0;
        if (type.isClan()) {
            if ((entity instanceof Mech) || (entity instanceof BattleArmor)) {
                bonus += 2;
            } else if ((entity instanceof Tank) || (entity instanceof Infantry)) {
                bonus -= 2;
            }
        } else if (type.isManeiDomini()) {
            bonus++;
        }

        // Demands of dual training
        if (entity instanceof LandAirMech) {
            bonus -= 2;
        }

        final int gunneryRoll = Compute.d6(1) + bonus;
        final int pilotingRoll = Compute.d6(1) + bonus;

        int gunneryLevel;
        int pilotingLevel;

        switch (level) {
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
            default:
                gunneryLevel = (int) Math.ceil((gunneryRoll + 0.5) / 2.0);
                pilotingLevel = (int) Math.ceil((pilotingRoll + 0.5) / 2.0);
                if (gunneryRoll <= 0) {
                    gunneryLevel = 0;
                }
                if (pilotingRoll <= 0) {
                    pilotingLevel = 0;
                }
                break;
        }

        int[] skills = new int[]{ SKILL_LEVELS[0][gunneryLevel], SKILL_LEVELS[1][pilotingLevel] };

        if (isForceClose()) {
            skills[1] = skills[0] + 1;
        }

        return skills;
    }
}
