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

import static megamek.client.ratgenerator.CrewDescriptor.clanSkillBonus;
import static megamek.client.ratgenerator.CrewDescriptor.innerSphereSkillBonus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Covers the modifier added to a generated crew's skill roll, and with it the promise that asking for
 * a skill level gets you that skill level.
 *
 * <p>The modifier shifts the roll along the row of whichever experience level was asked for, and the
 * good end of every row holds the next level's numbers. So a modifier allowed to grow without bound
 * does not merely improve a crew, it moves it out of the band that was requested: a Clan force
 * generated as Regular came out Veteran and Elite, and at the extreme could not produce a single
 * Regular crew.</p>
 */
class CrewSkillBonusTest {

    /** The Clan skill tables, as {@code CrewDescriptor} holds them, for reading results back. */
    private static final int[][] GUNNERY = {
          { 7, 6, 5, 5, 4, 4, 4, 4, 3 },
          { 5, 4, 4, 4, 4, 3, 3, 2, 2 },
          { 4, 4, 4, 3, 3, 2, 2, 1, 1 },
          { 4, 3, 3, 2, 2, 1, 1, 0, 0 }
    };
    private static final int[][] PILOTING = {
          { 7, 7, 6, 6, 6, 6, 5, 5, 4 },
          { 6, 6, 6, 5, 5, 4, 4, 3, 3 },
          { 6, 5, 5, 4, 4, 3, 3, 2, 2 },
          { 5, 4, 4, 3, 3, 2, 2, 1, 1 }
    };

    /** A Clan faction's ratings: Keshik, front line, second line, Solahma, provisional garrison. */
    private static final int CLAN_RATING_LEVELS = 5;
    private static final int KESHIK = 4;
    private static final int FRONT_LINE = 3;
    private static final int SECOND_LINE = 2;
    private static final int SOLAHMA = 0;

    /** The gunnery/piloting a crew rolls, for each face of the die, at this modifier. */
    private static String[] rolledSkills(int experienceRow, int bonus) {
        String[] results = new String[6];
        for (int roll = 1; roll <= 6; roll++) {
            int column = Math.clamp(roll + bonus, 0, GUNNERY[experienceRow].length - 1);
            results[roll - 1] = GUNNERY[experienceRow][column] + "/" + PILOTING[experienceRow][column];
        }
        return results;
    }

    private static long countOf(String[] results, String skills) {
        long count = 0;
        for (String result : results) {
            if (result.equals(skills)) {
                count++;
            }
        }
        return count;
    }

    /**
     * The calibration this exists to hold: a Clan Regular Mek crew is commonly 3/4, against the Inner
     * Sphere's 4/5.
     */
    @Test
    void aClanRegularMekCrewIsCommonly3And4() {
        for (int rating : new int[] { SECOND_LINE, FRONT_LINE, KESHIK }) {
            int bonus = clanSkillBonus(rating, CLAN_RATING_LEVELS, UnitType.MEK, false);
            String[] rolled = rolledSkills(CrewDescriptor.SKILL_REGULAR, bonus);

            assertEquals(2, countOf(rolled, "3/4"),
                  "rating " + rating + " should centre on 3/4, rolled " + String.join(" ", rolled));
            assertTrue(countOf(rolled, "4/5") > 0,
                  "rating " + rating + " must still be able to roll an ordinary 4/5");
        }
    }

    /**
     * The regression. The rating scaling used to stack on top of the caste advantage, so a front-line
     * or Keshik formation reached +3 or +4 and left the requested band behind entirely.
     */
    @Test
    void aTopRatedClanFormationStaysInTheBandItWasAskedFor() {
        int bonus = clanSkillBonus(KESHIK, CLAN_RATING_LEVELS, UnitType.MEK, false);

        assertEquals(2, bonus, "the caste advantage is the ceiling, not a step to climb on top of");
        assertTrue(countOf(rolledSkills(CrewDescriptor.SKILL_REGULAR, bonus), "4/5") > 0,
              "a force asked for Regular must be able to produce a Regular crew");
    }

    /** A Keshik crew is no worse than a second-line one; the cap lowers nothing that was correct. */
    @Test
    void theCapNeverMakesABetterRatedFormationWorse() {
        int secondLine = clanSkillBonus(SECOND_LINE, CLAN_RATING_LEVELS, UnitType.MEK, false);
        int frontLine = clanSkillBonus(FRONT_LINE, CLAN_RATING_LEVELS, UnitType.MEK, false);
        int keshik = clanSkillBonus(KESHIK, CLAN_RATING_LEVELS, UnitType.MEK, false);

        assertTrue(frontLine >= secondLine, "a front-line crew is no worse than a second-line one");
        assertTrue(keshik >= frontLine, "a Keshik crew is no worse than a front-line one");
    }

    /** The half of the rating scaling that was doing real work still does it. */
    @Test
    void aPoorlyRatedClanFormationIsStillPulledBelowTheCap() {
        int solahma = clanSkillBonus(SOLAHMA, CLAN_RATING_LEVELS, UnitType.MEK, false);

        assertTrue(solahma < 2, "Solahma crews are worse than the warrior caste, got " + solahma);
    }

    /** Clan vehicle and infantry crews are drawn from the lower castes and stay below their Meks. */
    @Test
    void clanVehicleCrewsRankBelowTheirMekWarriors() {
        int mek = clanSkillBonus(FRONT_LINE, CLAN_RATING_LEVELS, UnitType.MEK, false);
        int tank = clanSkillBonus(FRONT_LINE, CLAN_RATING_LEVELS, UnitType.TANK, false);

        assertTrue(tank < mek, "a Clan vehicle crew is not a Mek warrior");
    }

    /** A support formation is a step down whichever side of the Inner Sphere line it is on. */
    @Test
    void supportFormationsTakeAPenaltyOnBothSides() {
        assertTrue(clanSkillBonus(FRONT_LINE, CLAN_RATING_LEVELS, UnitType.MEK, true)
                    < clanSkillBonus(FRONT_LINE, CLAN_RATING_LEVELS, UnitType.MEK, false));
        assertTrue(innerSphereSkillBonus(2, 5, true, "FS")
                    < innerSphereSkillBonus(2, 5, false, "FS"));
    }

    /** The Inner Sphere side is untouched: StratOps gives +1 to the best rating and -1 to the worst. */
    @Test
    void theInnerSphereModifierIsUnchanged() {
        assertEquals(1, innerSphereSkillBonus(4, 5, false, "FS"), "an A rating");
        assertEquals(-1, innerSphereSkillBonus(0, 5, false, "FS"), "an F rating");
        assertEquals(0, innerSphereSkillBonus(2, 5, false, "FS"), "a middling rating");
        assertEquals(0, innerSphereSkillBonus(0, 1, false, "FS"), "a faction with one rating");
    }

    /** Shadow Division crews keep their edge over the rest of the Word of Blake. */
    @Test
    void shadowDivisionCrewsKeepTheirEdge() {
        assertTrue(innerSphereSkillBonus(2, 5, false, "WOB.SD")
                    > innerSphereSkillBonus(2, 5, false, "WOB"));
    }

    /** A force that never said what it fields takes the rating scaling alone. */
    @Test
    void anUnknownUnitTypeTakesTheRatingScalingAlone() {
        assertEquals(clanSkillBonus(SECOND_LINE, CLAN_RATING_LEVELS, null, false),
              SECOND_LINE - (CLAN_RATING_LEVELS / 2));
    }
}
