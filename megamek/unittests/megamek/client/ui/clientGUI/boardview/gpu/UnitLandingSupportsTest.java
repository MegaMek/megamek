/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class UnitLandingSupportsTest {
    private final Coords center = new Coords(1, 1);

    @Test
    void contactsUseSolidSurfacesAndRejectLiquidOrMissingGround() {
        Vector3 point = BoardGeometry.center(center, 0);
        assertEquals(-2 * BoardGeometry.LEVEL, UnitLandingSupports.ground(scene(-2, -1, false), point.x, point.y));
        assertTrue(Float.isNaN(UnitLandingSupports.ground(scene(0, 2, false), point.x, point.y)));
        assertEquals(BoardGeometry.LEVEL, UnitLandingSupports.ground(scene(1, 2, true), point.x, point.y));
        var scene = scene(0, -1, false);
        assertTrue(Float.isNaN(UnitLandingSupports.ground(scene, -1000, 1000)));
        assertTrue(Float.isNaN(UnitLandingSupports.ground(scene, Float.NaN, 0)));
        assertTrue(Float.isNaN(UnitLandingSupports.ground(scene, Float.MAX_VALUE, 0)));
    }

    @Test
    void padsFollowTheExistingRoadRampInsteadOfTheFlatHexLevelOrAFeatureRoof() {
        var neighbor = center.translated(2);
        var tiles = new ArrayList<BoardScene.Tile>();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                var coords = new Coords(x, y);
                tiles.add(new BoardScene.Tile(coords, coords.equals(center) ? 2 : 0, -1, false,
                      coords.equals(center) ? 1 << 2 : coords.equals(neighbor) ? 1 << 5 : 0, BoardScene.Surface.GRASS,
                      null, null, null, List.of(new BoardScene.Feature("building", 0, 0, 0, 1, 4, 0, BoardScene.FeatureKind.BUILDING)), List.of()));
            }
        }
        var scene = new BoardScene(0, 3, 3, tiles, List.of(), List.of(), -1, "", List.of());
        var point = BoardGeometry.center(center, 0).lerp(BoardGeometry.center(neighbor, 0), .5f);
        assertEquals(BoardGeometry.LEVEL, UnitLandingSupports.ground(scene, point.x, point.y), .002f);
    }

    private BoardScene scene(int level, int waterDepth, boolean frozen) {
        var tiles = new ArrayList<BoardScene.Tile>();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), level, waterDepth, frozen, 0, BoardScene.Surface.GRASS,
                      null, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 3, 3, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
