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
import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;

public class TotalWarfareSkillGenerator extends AbstractSkillGenerator {
    private final static MMLogger logger = MMLogger.create(TotalWarfareSkillGenerator.class);

    // region Variable Declarations
    protected static final int[][] SKILL_LEVELS = new int[][] {
          { 7, 6, 5, 4, 4, 3, 2, 1, 0, 0 },
          { 7, 7, 6, 6, 5, 4, 3, 2, 1, 0 } };
    // endregion Variable Declarations

    // region Constructors
    public TotalWarfareSkillGenerator() {
        this(SkillGeneratorMethod.TOTAL_WARFARE);
    }

    protected TotalWarfareSkillGenerator(final SkillGeneratorMethod method) {
        super(method);
    }
    // endregion Constructors

    @Override
    public int[] generateRandomSkills(final Entity entity, final boolean clanPilot, final boolean forceClan) {
        return generateRandomSkills(getLevel(), entity, clanPilot, forceClan);
    }

    protected int[] generateRandomSkills(final SkillLevel level, final Entity entity, final boolean clanPilot,
          final boolean forceClan) {
        final int bonus = determineBonus(entity, clanPilot, forceClan);

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
                logger.error("Attempting to generate skills for unknown skill level of {}", level);
                gunneryLevel = 0;
                pilotingLevel = 0;
                break;
        }

        return cleanReturn(entity, SKILL_LEVELS[0][Math.min(gunneryLevel, SKILL_LEVELS[0].length)],
              SKILL_LEVELS[1][Math.min(pilotingLevel, SKILL_LEVELS[1].length)]);
    }

    /**
     * @param entity    the entity whose crew skill is being rolled
     * @param clanPilot if the crew is led by a clan pilot
     * @param forceClan forces the type to be clan if the crew are led by a clanPilot
     *
     * @return the bonus to use on the Random Skills Table (Expanded) roll
     */
    protected int determineBonus(final Entity entity, final boolean clanPilot,
          final boolean forceClan) {
        if (getType().isClan() || (forceClan && clanPilot)) {
            if (entity instanceof Mek) {
                return 2;
            } else if (entity instanceof Tank) {
                return -2;
            }
        }

        return 0;
    }
}
