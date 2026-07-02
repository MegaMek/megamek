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

package megamek.server.victory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import megamek.common.game.Game;
import org.junit.jupiter.api.Test;

class ScanTallyTest {

    @Test
    void testRecordScanAndCount() {
        ScanTally tally = new ScanTally();

        assertTrue(tally.recordScan(1, 10));
        assertTrue(tally.recordScan(1, 11));
        assertTrue(tally.recordScan(2, 10));

        assertEquals(2, tally.getScanCount(1));
        assertEquals(1, tally.getScanCount(2));
        assertEquals(0, tally.getScanCount(99));
    }

    @Test
    void testRepeatScanOfSameTargetNotBanked() {
        ScanTally tally = new ScanTally();
        tally.recordScan(1, 10);

        assertFalse(tally.recordScan(1, 10));
        assertEquals(1, tally.getScanCount(1));
        assertTrue(tally.hasScanned(1, 10));
        assertFalse(tally.hasScanned(1, 11));
    }

    @Test
    void testExfiltrationProcessedOnce() {
        ScanTally tally = new ScanTally();

        assertFalse(tally.isExfiltrationProcessed(1));
        tally.markExfiltrationProcessed(1);
        assertTrue(tally.isExfiltrationProcessed(1));
        assertFalse(tally.isExfiltrationProcessed(2));
    }

    @Test
    void testGetTallyCreatesAndReuses() {
        Game game = new Game();
        game.setVictoryContext(new HashMap<>());

        ScanTally firstCall = ScanTally.getTally(game);
        firstCall.recordScan(1, 10);
        ScanTally secondCall = ScanTally.getTally(game);

        assertSame(firstCall, secondCall);
        assertEquals(1, secondCall.getScanCount(1));
        assertSame(firstCall, ScanTally.findTally(game.getVictoryContext()));
    }

    @Test
    void testFindTallyWithoutContextOrTally() {
        assertNull(ScanTally.findTally(null));
        assertNull(ScanTally.findTally(new HashMap<>()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSerializationRoundTrip() throws Exception {
        Game game = new Game();
        game.setVictoryContext(new HashMap<>());
        ScanTally tally = ScanTally.getTally(game);
        tally.recordScan(1, 10);
        tally.recordScan(1, 11);
        tally.markExfiltrationProcessed(2);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(game.getVictoryContext());
        }
        HashMap<String, Object> restoredContext;
        try (ObjectInputStream objectInputStream =
              new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()))) {
            restoredContext = (HashMap<String, Object>) objectInputStream.readObject();
        }

        ScanTally restoredTally = ScanTally.findTally(restoredContext);
        assertNotNull(restoredTally);
        assertEquals(2, restoredTally.getScanCount(1));
        assertTrue(restoredTally.hasScanned(1, 10));
        assertTrue(restoredTally.isExfiltrationProcessed(2));
        assertFalse(restoredTally.isExfiltrationProcessed(1));
    }
}
