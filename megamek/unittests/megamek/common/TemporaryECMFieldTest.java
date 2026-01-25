/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TemporaryECMField} expiration and coverage logic.
 */
class TemporaryECMFieldTest {

    @Test
    void testFromEMPMineCreation() {
        Coords position = new Coords(5, 5);
        int currentRound = 3;
        int playerId = 1;

        TemporaryECMField field = TemporaryECMField.fromEMPMine(position, currentRound, playerId);

        assertEquals(position, field.getPosition());
        assertEquals(0, field.getRange()); // Single hex
        assertEquals(currentRound, field.getCreationRound());
        assertEquals(currentRound + 1, field.getExpirationRound());
        assertEquals(GamePhase.END, field.getExpirationPhase());
        assertEquals(playerId, field.getPlayerId());
        assertEquals("EMP Mine", field.getSource());
    }

    @Test
    void testIsExpiredBeforeExpirationRound() {
        TemporaryECMField field = TemporaryECMField.fromEMPMine(new Coords(0, 0), 3, 1);

        // During round 3 (creation round), not expired
        assertFalse(field.isExpired(3, GamePhase.MOVEMENT));
        assertFalse(field.isExpired(3, GamePhase.FIRING));
        assertFalse(field.isExpired(3, GamePhase.END));

        // During round 4, before END phase, not expired
        assertFalse(field.isExpired(4, GamePhase.MOVEMENT));
        assertFalse(field.isExpired(4, GamePhase.FIRING));
    }

    @Test
    void testIsExpiredAtExpirationPhase() {
        TemporaryECMField field = TemporaryECMField.fromEMPMine(new Coords(0, 0), 3, 1);

        // Expires at END phase of round 4
        assertTrue(field.isExpired(4, GamePhase.END));
    }

    @Test
    void testIsExpiredAfterExpirationRound() {
        TemporaryECMField field = TemporaryECMField.fromEMPMine(new Coords(0, 0), 3, 1);

        // Round 5 is after expiration
        assertTrue(field.isExpired(5, GamePhase.MOVEMENT));
        assertTrue(field.isExpired(5, GamePhase.END));
    }

    @Test
    void testAffectsHexAtPosition() {
        Coords position = new Coords(5, 5);
        TemporaryECMField field = TemporaryECMField.fromEMPMine(position, 1, 1);

        // EMP mine ECM is range 0 (single hex)
        assertTrue(field.affectsHex(position));
        assertFalse(field.affectsHex(new Coords(5, 6)));
        assertFalse(field.affectsHex(new Coords(4, 5)));
    }

    @Test
    void testAffectsHexWithRange() {
        Coords position = new Coords(5, 5);
        // Create a field with range 2
        TemporaryECMField field = new TemporaryECMField(
              position, 2, 1, 2, GamePhase.END, 1, "Test"
        );

        assertTrue(field.affectsHex(position));
        assertTrue(field.affectsHex(new Coords(5, 6))); // Distance 1
        assertTrue(field.affectsHex(new Coords(5, 7))); // Distance 2
        assertFalse(field.affectsHex(new Coords(5, 8))); // Distance 3
    }

    @Test
    void testAffectsHexNullCoords() {
        TemporaryECMField field = TemporaryECMField.fromEMPMine(new Coords(5, 5), 1, 1);

        assertFalse(field.affectsHex(null));
    }

    @Test
    void testEquality() {
        TemporaryECMField field1 = TemporaryECMField.fromEMPMine(new Coords(5, 5), 3, 1);
        TemporaryECMField field2 = TemporaryECMField.fromEMPMine(new Coords(5, 5), 3, 1);
        TemporaryECMField field3 = TemporaryECMField.fromEMPMine(new Coords(6, 6), 3, 1);

        assertEquals(field1, field2);
        assertFalse(field1.equals(field3));
    }
}
