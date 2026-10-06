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
package megamek.common.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InitiativeRollTest {

    /**
     * The copy constructor must produce an independent instance: appending a tiebreak roll to the copy must not affect
     * the source. Sharing the backing vectors would let two distinct initiative candidates compare equal forever and
     * cause unbounded tie-break recursion (a StackOverflowError).
     */
    @Test
    void copyConstructorProducesIndependentRoll() {
        InitiativeRoll source = new InitiativeRoll();
        source.addRoll(InitiativeBonusBreakdown.fromTotal(2), "");

        InitiativeRoll copy = new InitiativeRoll(source);

        assertNotSame(source, copy);
        assertEquals(source.size(), copy.size());
        assertEquals(source, copy);

        // Mutating the copy must leave the source untouched.
        int sourceSizeBefore = source.size();
        copy.addRoll(InitiativeBonusBreakdown.fromTotal(0), "");

        assertEquals(sourceSizeBefore, source.size());
        assertEquals(sourceSizeBefore + 1, copy.size());
    }

    @Test
    void copyOfEmptyRollIsEmpty() {
        InitiativeRoll copy = new InitiativeRoll(new InitiativeRoll());
        assertEquals(0, copy.size());
    }

    @Test
    void copyPreservesEveryRollAndBonus() {
        InitiativeRoll source = new InitiativeRoll();
        source.addRoll(InitiativeBonusBreakdown.fromTotal(2), "");
        source.addRoll(InitiativeBonusBreakdown.fromTotal(-1), "");

        InitiativeRoll copy = new InitiativeRoll(source);

        assertEquals(source.size(), copy.size());
        for (int i = 0; i < source.size(); i++) {
            // getRoll() folds in the bonus total, so this verifies both the raw roll and the bonus were copied.
            assertEquals(source.getRoll(i), copy.getRoll(i), "roll " + i);
        }
        assertEquals(source, copy);
        assertEquals(source.toString(), copy.toString());
    }

    @Test
    void mutatingSourceAfterCopyLeavesCopyUnchanged() {
        InitiativeRoll source = new InitiativeRoll();
        source.addRoll(InitiativeBonusBreakdown.fromTotal(0), "");

        InitiativeRoll copy = new InitiativeRoll(source);
        String copyBefore = copy.toString();

        source.addRoll(InitiativeBonusBreakdown.fromTotal(0), "");

        assertEquals(1, copy.size());
        assertEquals(copyBefore, copy.toString());
    }

    @Test
    void copyPreservesReplacedRollState() {
        // replaceRoll() flags the roll as replaced (Tactical Genius); the flag must survive the copy.
        InitiativeRoll source = new InitiativeRoll();
        source.addRoll(InitiativeBonusBreakdown.fromTotal(0), "");
        source.replaceRoll(InitiativeBonusBreakdown.fromTotal(0), "");

        InitiativeRoll copy = new InitiativeRoll(source);

        assertEquals(source.toString(), copy.toString());
        assertTrue(copy.toString().contains("Tactical Genius"), copy.toString());
    }
}
