/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EngineExplosionRatingTest {
    @ParameterizedTest
    @CsvSource({ "19.996, 20, 2, 1, 0", "37.5, 37, 4, 2, 1", "375, 375, 37, 19, 9", "500, 500, 50, 25, 12" })
    void fractionalRatingsSurviveUntilBlastDamageIsRounded(double rating, int center, int adjacent,
          int twoAway, int threeAway) {
        CapturingGameManager manager = new CapturingGameManager();
        manager.doFusionEngineExplosion(rating, BoardLocation.of(new Coords(0, 0), 0), new Vector<>(), null);
        assertArrayEquals(new int[] { center, adjacent, twoAway, threeAway }, manager.damages);
    }

    private static class CapturingGameManager extends TWGameManager {
        private int[] damages;

        @Override
        public void doExplosion(int[] damages, boolean autoDestroyInSameHex, Coords position, int boardId,
              boolean allowShelter, Vector<Report> reports, Vector<Integer> units, int clusterAmount,
              int excludedUnitId, boolean engineExplosion, boolean canDamageVtol) {
            this.damages = damages;
        }
    }
}
