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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The infantry vs. infantry casualty arithmetic checked against the worked example on TO:AR pp. 172-173 (attacker
 * 229.25, defender 76, roll of 3 on the 3 to 1 column: 55%/30%) and the rounding rules on p. 174.
 */
class InfantryCombatCasualtiesTest {

    @Test
    void attackerLossIsAPercentageOfTheDefendersStrengthRoundedUp() {
        // book: 76 x .55 = 41.8, rounded up
        assertEquals(42, InfantryCombatCasualties.marinePointsLost(76, 55, false, false));
    }

    @Test
    void defenderInFullControlLosesHalfRoundedUpOnce() {
        // book: 229.25 x .3 = 68.78, halved = 34.39, rounded up; 229 gives the same 35
        assertEquals(35, InfantryCombatCasualties.marinePointsLost(229, 30, false, true));
    }

    @Test
    void repulsedAttackerLosesDouble() {
        // 1 to 1 column, roll of 2: 75%/25% (R) against a 28-point defender
        assertEquals(42, InfantryCombatCasualties.marinePointsLost(28, 75, true, false));
    }

    @Test
    void withdrawingAttackerLosesHalf() {
        assertEquals(6, InfantryCombatCasualties.marinePointsLost(28, 40, false, true));
    }

    @Test
    void nothingIsLostFromNothing() {
        assertEquals(0, InfantryCombatCasualties.marinePointsLost(0, 55, false, false));
        assertEquals(0, InfantryCombatCasualties.marinePointsLost(76, 0, false, false));
    }

    @Test
    void casualtyFractionIsTheShareOfOwnStrength() {
        assertEquals(12.0 / 56.0, InfantryCombatCasualties.casualtyFraction(12, 56), 1e-9);
        assertEquals(0.0, InfantryCombatCasualties.casualtyFraction(0, 56));
        assertEquals(1.0, InfantryCombatCasualties.casualtyFraction(56, 56));
        assertEquals(1.0, InfantryCombatCasualties.casualtyFraction(60, 56), "a loss above own strength is total");
        assertEquals(1.0, InfantryCombatCasualties.casualtyFraction(1, 0), "a side with no strength is gone");
    }

    @Test
    void personnelLostRoundsDown() {
        // a 28-man platoon on a side that lost 12 of 56 points loses 6, not 6.0000001 rounded up
        assertEquals(6, InfantryCombatCasualties.personnelLost(28, 12.0 / 56.0, false));
    }

    @Test
    void armouredTroopersLoseHalfAsManyRoundedDown() {
        // p. 174: armour with a damage divisor of 2 or more halves the final casualty number, rounded down
        assertEquals(3, InfantryCombatCasualties.personnelLost(28, 12.0 / 56.0, true));
        assertEquals(0, InfantryCombatCasualties.personnelLost(28, 1.0 / 56.0, true));
    }

    @Test
    void aTotalLossTakesEveryone() {
        assertEquals(28, InfantryCombatCasualties.personnelLost(28, 1.0, false));
        assertEquals(28, InfantryCombatCasualties.personnelLost(28, 1.0, true), "armour does not save a wiped side");
    }

    @Test
    void noLossTakesNobody() {
        assertEquals(0, InfantryCombatCasualties.personnelLost(28, 0.0, false));
        assertEquals(0, InfantryCombatCasualties.personnelLost(0, 0.5, false));
    }
}
