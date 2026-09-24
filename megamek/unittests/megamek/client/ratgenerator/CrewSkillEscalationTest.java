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
package megamek.client.ratgenerator;

import static megamek.client.ratgenerator.CrewDescriptor.escalateExceptionalCrew;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.enums.SkillLevel;
import org.junit.jupiter.api.Test;

/**
 * Verifies the elite crew escalation that lets the Force Generator reach Heroic and Legendary crews.
 *
 * <p>The skill tables stop at the elite row, and a 1d6 into that row reaches Heroic only at its top
 * columns - which needs the equipment-rating bonus. Legendary was therefore unreachable for most
 * commands and near-guaranteed for a few, rather than rare everywhere.</p>
 */
class CrewSkillEscalationTest {

    private static final int ELITE = CrewDescriptor.SKILL_ELITE;
    private static final int VETERAN = CrewDescriptor.SKILL_VETERAN;

    /** Elite gunnery/piloting as the tables produce it, a rung below Heroic. */
    private static final int ELITE_GUNNERY = 2;
    private static final int ELITE_PILOTING = 3;

    /** Foot infantry carry this fixed piloting value, which must never be improved. */
    private static final int INFANTRY_PILOTING = 8;

    @Test
    void escalationTargetsMatchTheSkillLevelThresholds() {
        // The escalation writes these exact pairs. If SkillLevel's definition moves and the constants
        // in CrewDescriptor do not, this is what catches it.
        assertEquals(1, SkillLevel.HEROIC.getDefaultSkillValues()[0], "Heroic gunnery");
        assertEquals(2, SkillLevel.HEROIC.getDefaultSkillValues()[1], "Heroic piloting");
        assertEquals(0, SkillLevel.LEGENDARY.getDefaultSkillValues()[0], "Legendary gunnery");
        assertEquals(1, SkillLevel.LEGENDARY.getDefaultSkillValues()[1], "Legendary piloting");
    }

    @Test
    void belowEliteNeverEscalates() {
        // Only elite crews are candidates, so a veteran can never become legendary however it rolls.
        for (int attempt = 0; attempt < 200; attempt++) {
            int[] result = escalateExceptionalCrew(VETERAN, 3, 4, true);
            assertEquals(3, result[0], "veteran gunnery must be untouched");
            assertEquals(4, result[1], "veteran piloting must be untouched");
        }
    }

    @Test
    void eliteCrewsReachHeroicAndLegendaryAcrossManyRolls() {
        int heroic = 0;
        int legendary = 0;
        int unchanged = 0;
        for (int attempt = 0; attempt < 20000; attempt++) {
            int[] result = escalateExceptionalCrew(ELITE, ELITE_GUNNERY, ELITE_PILOTING, true);
            if (result[0] == 0) {
                legendary++;
            } else if (result[0] == 1) {
                heroic++;
            } else {
                unchanged++;
            }
        }
        // Roughly 1 in 6 escalate and 1 in 36 reach legendary. Bounds are deliberately loose - this
        // asserts that both outcomes are reachable and rare, not an exact distribution.
        assertTrue(legendary > 0, "legendary crews should be reachable");
        assertTrue(heroic > 0, "heroic crews should be reachable");
        assertTrue(unchanged > heroic, "most elite crews should stay elite");
        assertTrue(heroic > legendary, "heroic should be commoner than legendary");
    }

    @Test
    void escalationOnlyEverImprovesSkills() {
        // A crew that already rolled into the top columns keeps what it earned; escalation must not
        // hand it a worse number.
        for (int attempt = 0; attempt < 500; attempt++) {
            int[] result = escalateExceptionalCrew(ELITE, 0, 1, true);
            assertTrue(result[0] <= 0, "gunnery should never worsen, got " + result[0]);
            assertTrue(result[1] <= 1, "piloting should never worsen, got " + result[1]);
        }
    }

    @Test
    void footInfantryKeepTheirFixedPilotingValue() {
        // Non-anti-Mek infantry have no real piloting skill; improving it would be meaningless and
        // would make them look like exceptional pilots.
        boolean sawEscalatedGunnery = false;
        for (int attempt = 0; attempt < 2000; attempt++) {
            int[] result = escalateExceptionalCrew(ELITE, ELITE_GUNNERY, INFANTRY_PILOTING, false);
            assertEquals(INFANTRY_PILOTING, result[1], "foot infantry piloting must stay fixed");
            if (result[0] < ELITE_GUNNERY) {
                sawEscalatedGunnery = true;
            }
        }
        assertTrue(sawEscalatedGunnery, "infantry gunnery should still be able to escalate");
    }
}
