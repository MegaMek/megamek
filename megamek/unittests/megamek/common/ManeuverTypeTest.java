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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.board.Board;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the legality checks for aerospace special maneuvers (TW p.85). A Split-S loses two altitudes, so it is only
 * legal when the unit ends strictly above the terrain ceiling below it (regression test for issue #8699, where the
 * check read {@code (altitude + 2) > ceiling} and offered a Split-S straight into the ground).
 */
class ManeuverTypeTest {

    private static final int GROUND_MAP_CEILING = 0;
    private static final boolean NOT_VTOL = false;
    private static final int MANEUVER_AT_MOVE_START = 0;

    private Board board;
    private MovePath movePath;

    @BeforeEach
    void setUp() {
        board = mock(Board.class);
        movePath = mock(MovePath.class);
        when(movePath.getStepVector()).thenReturn(new Vector<MoveStep>());
    }

    private boolean canPerform(int maneuverType, int velocity, int altitude, int ceiling) {
        return ManeuverType.canPerform(maneuverType, velocity, altitude, ceiling, NOT_VTOL,
              MANEUVER_AT_MOVE_START, board, movePath);
    }

    @Test
    @DisplayName("Split-S from altitude 2 over a ground map would end in the ground and is illegal")
    void splitSAtAltitudeTwoOverGroundMapIsIllegal() {
        assertFalse(canPerform(ManeuverType.MAN_SPLIT_S, 3, 2, GROUND_MAP_CEILING),
              "Split-S from altitude 2 ends at altitude 0 - in the ground");
    }

    @Test
    @DisplayName("Split-S from altitude 1 over a ground map would end below the ground and is illegal")
    void splitSAtAltitudeOneOverGroundMapIsIllegal() {
        assertFalse(canPerform(ManeuverType.MAN_SPLIT_S, 3, 1, GROUND_MAP_CEILING),
              "Split-S from altitude 1 ends below altitude 0");
    }

    @Test
    @DisplayName("Split-S from altitude 3 over a ground map ends at altitude 1 and is legal")
    void splitSAtAltitudeThreeOverGroundMapIsLegal() {
        assertTrue(canPerform(ManeuverType.MAN_SPLIT_S, 3, 3, GROUND_MAP_CEILING),
              "Split-S from altitude 3 ends at altitude 1, the minimum legal flight altitude");
    }

    @Test
    @DisplayName("Split-S from altitude 5 over a ground map is legal")
    void splitSAtAltitudeFiveOverGroundMapIsLegal() {
        assertTrue(canPerform(ManeuverType.MAN_SPLIT_S, 3, 5, GROUND_MAP_CEILING));
    }

    @Test
    @DisplayName("Split-S ending exactly at a low-altitude hex ceiling is illegal")
    void splitSEndingAtTerrainCeilingIsIllegal() {
        // Low-altitude map hex with terrain reaching altitude 3: ending AT the ceiling is a collision,
        // matching the strictly-above convention used when flying into a hex (MoveStep)
        assertFalse(canPerform(ManeuverType.MAN_SPLIT_S, 3, 5, 3),
              "Ending at the hex ceiling altitude is a terrain collision");
    }

    @Test
    @DisplayName("Split-S ending above a low-altitude hex ceiling is legal")
    void splitSEndingAboveTerrainCeilingIsLegal() {
        assertTrue(canPerform(ManeuverType.MAN_SPLIT_S, 3, 6, 3));
    }

    @Test
    @DisplayName("Immelmann still requires velocity 3+ and headroom below altitude 9")
    void immelmannChecksAreUnchanged() {
        assertTrue(canPerform(ManeuverType.MAN_IMMELMAN, 3, 2, GROUND_MAP_CEILING));
        assertFalse(canPerform(ManeuverType.MAN_IMMELMAN, 2, 2, GROUND_MAP_CEILING),
              "Immelmann needs velocity 3 or more");
        assertFalse(canPerform(ManeuverType.MAN_IMMELMAN, 3, 9, GROUND_MAP_CEILING),
              "Immelmann climbs 2 and needs to start below altitude 9");
    }
}
