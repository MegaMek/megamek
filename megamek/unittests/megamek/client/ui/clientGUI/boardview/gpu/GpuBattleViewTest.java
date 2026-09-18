/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class GpuBattleViewTest {
    private static final float SAMPLES_PER_SECOND = 60;

    @Test
    void hoverWaveStaysWithinItsAmplitudeAndRepeatsItsPeriod() {
        float amplitude = GpuBattleView.HOVER_LEVELS * BoardGeometry.LEVEL;
        float reached = 0;
        for (int sample = 0; sample < 2000; sample++) {
            float offset = GpuBattleView.hoverOffset(sample / SAMPLES_PER_SECOND, 7, -1);
            assertTrue(Math.abs(offset) <= amplitude + 0.001f, "A hover offset stays within its amplitude");
            reached = Math.max(reached, Math.abs(offset));
        }
        assertTrue(reached > amplitude * 0.98f, "The wave must reach its amplitude");
        assertEquals(GpuBattleView.hoverOffset(1.25f, 7, -1),
              GpuBattleView.hoverOffset(1.25f + GpuBattleView.HOVER_PERIOD_SECONDS, 7, -1), 0.001f,
              "One period must return to the same height");
    }

    @Test
    void hoverWaveStartsAtDifferentPhasesPerUnitAndPart() {
        assertNotEquals(GpuBattleView.hoverOffset(0, 1, -1), GpuBattleView.hoverOffset(0, 2, -1), 0.01f,
              "Neighboring units must not rise and fall together");
        assertNotEquals(GpuBattleView.hoverOffset(0.7f, 4, -1), GpuBattleView.hoverOffset(0.7f, 4, 1), 0.01f,
              "Multiple parts of one unit must not rise and fall together");
    }

    @Test
    void floatingMeeplesAreTiedToTheirHexCenter() {
        BoardScene.Unit floating = unit(true);
        Vector3 center = new Vector3(10, 20, 5 * BoardGeometry.LEVEL);
        assertEquals(3 * BoardGeometry.LEVEL, GpuBattleView.tetherGround(floating, 3, center, false), 0.001f,
              "The stem ends at the center of the hex under the token");
        assertTrue(Float.isNaN(GpuBattleView.tetherGround(floating, 3, center, true)),
              "A moving meeple is between hexes and gets no stem");
        assertTrue(Float.isNaN(GpuBattleView.tetherGround(unit(false), 3, center, false)),
              "A grounded meeple covers its hex and gets no stem");
        assertTrue(Float.isNaN(GpuBattleView.tetherGround(floating, 3,
              new Vector3(10, 20, 3 * BoardGeometry.LEVEL), false)),
              "A token at its tile surface has nothing to point at");
    }

    private static BoardScene.Unit unit(boolean airborne) {
        return new BoardScene.Unit(1, -1, "test", new BoardScene.Waypoint(new Coords(0, 0), 0, 0), null, false, null, 1,
              airborne);
    }
}
