/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.common.board.Coords;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BoardGeometryTest {
    @BeforeAll
    static void loadMathNatives() {
        GdxNativesLoader.load();
    }

    private BoardScene scene(int raisedLevel) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 2 && y == 2 ? raisedLevel : 0, -1, false, 0, BoardScene.Surface.GRASS,
                      null, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "Movement", List.of());
    }

    @Test
    void everyHexCenterPicksCorrectlyInBothViews() {
        BoardScene scene = scene(0);
        BoardCamera view = new BoardCamera();
        view.resize(1100, 750);
        for (boolean isometric : List.of(false, true)) {
            view.setIsometric(isometric);
            view.fit(scene);
            for (BoardScene.Tile tile : scene.tiles()) {
                Vector3 center = BoardGeometry.center(tile.coords(), tile.elevation());
                Ray ray = new Ray(new Vector3(center).mulAdd(view.camera.direction, -1000), view.camera.direction);
                assertEquals(tile.coords(), BoardGeometry.pick(scene, ray));
            }
        }
    }

    @Test
    void cliffFaceBelongsToTheRaisedHexAndOutsideMisses() {
        BoardScene scene = scene(4);
        Coords raised = new Coords(2, 2);
        Vector3 center = BoardGeometry.center(raised, 2);
        Ray ray = new Ray(new Vector3(center).add(200, 0, 0), new Vector3(-1, 0, 0));
        assertEquals(raised, BoardGeometry.pick(scene, ray));
        assertNull(BoardGeometry.pick(scene, new Ray(new Vector3(-500, 500, 100), new Vector3(0, 0, -1))));
    }

    @Test
    void switchingPreservesFocusAndZoomAndFitIncludesEveryCorner() {
        BoardScene scene = scene(4);
        BoardCamera view = new BoardCamera();
        view.resize(500, 740);
        view.fit(scene);
        view.pan(60, -20);
        view.zoom(0.8f);
        Vector3 focus = new Vector3(view.focus);
        float zoom = view.camera.zoom;
        view.setIsometric(true);
        assertTrue(focus.epsilonEquals(view.focus, 0.001f));
        assertEquals(zoom, view.camera.zoom);
        view.fit(scene);
        for (BoardScene.Tile tile : scene.tiles()) {
            for (int corner = 0; corner < 6; corner++) {
                Vector3 screen = view.camera.project(BoardGeometry.corner(tile.coords(), tile.elevation(), corner),
                      0, 0, 500, 740);
                assertTrue(screen.x >= 0 && screen.x <= 500 && screen.y >= 0 && screen.y <= 740);
            }
        }
    }

    @Test
    void raisedTerrainOccludesPickingTheHexBehindIt() {
        BoardScene scene = scene(6);
        BoardCamera view = new BoardCamera();
        view.resize(1100, 750);
        view.setIsometric(true);
        Vector3 hidden = BoardGeometry.center(new Coords(1, 1), 0);
        Ray ray = new Ray(hidden.mulAdd(view.camera.direction, -1000), view.camera.direction);
        assertEquals(new Coords(2, 2), BoardGeometry.pick(scene, ray));
    }

    @Test
    void resizingRefitsTheBoardUntilThePlayerMovesTheCamera() {
        BoardScene scene = scene(4);
        BoardCamera view = new BoardCamera();
        view.resize(1280, 800);
        view.setIsometric(true);
        view.fit(scene);
        view.resize(900, 500, scene, 1);
        for (BoardScene.Tile tile : scene.tiles()) {
            for (int corner = 0; corner < 6; corner++) {
                Vector3 screen = view.camera.project(BoardGeometry.corner(tile.coords(), tile.elevation(), corner),
                      0, 0, 900, 500);
                assertTrue(screen.x >= 0 && screen.x <= 900 && screen.y >= 0 && screen.y <= 500);
            }
        }
        view.pan(40, 20);
        Vector3 focus = new Vector3(view.focus);
        float zoom = view.camera.zoom;
        view.resize(1800, 1000, scene, 2);
        assertTrue(focus.epsilonEquals(view.focus, 0.001f));
        assertEquals(zoom / 2, view.camera.zoom, 0.0001f);
        view.toggleOverview(scene);
        view.resize(2700, 1500, scene, 3);
        view.toggleOverview(scene);
        assertTrue(focus.epsilonEquals(view.focus, 0.001f));
        assertEquals(zoom / 3, view.camera.zoom, 0.0001f);
    }

    @Test
    void orbitKeepsPickingAndTheCameraBasisValidAroundTheFullBoard() {
        BoardScene scene = scene(0);
        BoardCamera view = new BoardCamera();
        view.resize(1100, 750);
        for (float tilt : new float[] { 0, 35, BoardCamera.MAX_TILT }) {
            for (float rotation : new float[] { -450, 0, 45, 90, 180, 270, 359, 1080 }) {
                view.setIsometric(false);
                view.orbit(rotation, tilt);
                view.fit(scene);
                assertEquals(1, view.camera.direction.len(), 0.0001f);
                assertEquals(1, view.camera.up.len(), 0.0001f);
                assertEquals(0, view.camera.direction.dot(view.camera.up), 0.0001f);
                assertTrue(view.camera.direction.z < -0.25f, "Orbit must stay above the horizon");
                for (BoardScene.Tile tile : scene.tiles()) {
                    Vector3 center = BoardGeometry.center(tile.coords(), tile.elevation());
                    Ray ray = new Ray(new Vector3(center).mulAdd(view.camera.direction, -1000), view.camera.direction);
                    assertEquals(tile.coords(), BoardGeometry.pick(scene, ray));
                    assertTrue(view.visibleArea(scene).contains(tile.coords().getX(), tile.coords().getY()));
                }
            }
        }
        view.orbit(0, 10000);
        assertEquals(BoardCamera.MAX_TILT, view.tilt());
        view.orbit(0, -10000);
        assertTrue(view.isTopDown());
    }

    @Test
    void panFollowsThePointerAndZoomRetainsItsAnchorWithoutRaisingThePivot() {
        BoardCamera view = new BoardCamera();
        view.resize(1100, 750);
        for (float tilt : new float[] { 0, 35, BoardCamera.MAX_TILT }) {
            view.setIsometric(false);
            view.orbit(135, tilt);
            view.center(BoardGeometry.center(new Coords(2, 2), 0));
            Vector3 anchor = BoardGeometry.center(new Coords(3, 3), 0);
            Vector3 before = view.camera.project(new Vector3(anchor), 0, 0, 1100, 750);
            view.pan(75, -25);
            Vector3 panned = view.camera.project(new Vector3(anchor), 0, 0, 1100, 750);
            assertEquals(before.x + 75, panned.x, 0.1f);
            assertEquals(before.y + 25, panned.y, 0.1f);
            assertEquals(0, view.focus.z, 0.001f);
            view.zoomAt(0.6f, panned.x, panned.y);
            Vector3 zoomed = view.camera.project(new Vector3(anchor), 0, 0, 1100, 750);
            assertEquals(panned.x, zoomed.x, 0.1f);
            assertEquals(panned.y, zoomed.y, 0.1f);
            assertEquals(0, view.focus.z, 0.001f);
        }
    }

    @Test
    void fittingAtLowAnglesIncludesCliffsAndResetRestoresTheIsometricPreset() {
        BoardScene scene = scene(8);
        BoardCamera view = new BoardCamera();
        view.resize(700, 450);
        view.orbit(217, BoardCamera.MAX_TILT);
        view.fit(scene);
        float floor = BoardGeometry.floor(scene) / BoardGeometry.LEVEL;
        for (BoardScene.Tile tile : scene.tiles()) {
            for (float elevation : new float[] { floor, tile.elevation() }) {
                for (int corner = 0; corner < 6; corner++) {
                    Vector3 point = view.camera.project(BoardGeometry.corner(tile.coords(), elevation, corner),
                          0, 0, 700, 450);
                    assertTrue(point.x >= 0 && point.x <= 700 && point.y >= 0 && point.y <= 450);
                }
            }
        }
        view.pan(200, 100);
        view.zoom(0.5f);
        view.reset(scene);
        assertTrue(view.isIsometric());
        Vector3 focus = new Vector3(view.focus);
        float zoom = view.camera.zoom;
        view.orbit(85, -20);
        assertTrue(focus.epsilonEquals(view.focus, 0.001f));
        assertEquals(zoom, view.camera.zoom);
    }


    @Test
    void neighborsShareExactlyTheSameEdgeAtEveryScale() {
        BoardGeometry.Tuning original = BoardGeometry.tuning();
        try {
            for (float scale : new float[] { 0.5f, 1, 3 }) {
                BoardGeometry.tune(new BoardGeometry.Tuning(scale, 0.6f, 0.87f, 18, 0.8f));
                for (int column = 0; column < 2; column++) {
                    Coords here = new Coords(column, 1);
                    for (int edge = 0; edge < 6; edge++) {
                        Coords neighbor = here.translated(BoardGeometry.edgeDirection(edge));
                        Vector3 a = BoardGeometry.corner(here, 0, edge);
                        Vector3 b = BoardGeometry.corner(neighbor, 0, edge + 4);
                        assertEquals(0, a.dst(b), 0.001f);
                    }
                }
            }
        } finally {
            BoardGeometry.tune(original);
        }
    }

    @Test
    void recessedWaterUsesRealDepthAndStillPicksItsHex() {
        for (int depth : new int[] { 0, 1, 2, 7 }) {
            BoardScene.Tile wet = new BoardScene.Tile(new Coords(0, 0), 2, depth, false, 0, BoardScene.Surface.GRASS,
                  null, null, null, List.of(), List.of());
            BoardScene scene = new BoardScene(0, 1, 1, List.of(wet), List.of(), List.of(), -1, "", List.of());
            assertTrue(BoardGeometry.groundZ(wet) < BoardGeometry.waterZ(wet));
            assertEquals(2 * BoardGeometry.LEVEL - (depth == 0 ? 2 * BoardGeometry.HEX_SCALE : depth * BoardGeometry.LEVEL),
                  BoardGeometry.groundZ(wet), 0.001f);
            Vector3 above = BoardGeometry.center(wet.coords(), 20);
            assertEquals(wet.coords(), BoardGeometry.pick(scene, new Ray(above, new Vector3(0, 0, -1))));
            assertTrue(BoardGeometry.floor(scene) < BoardGeometry.groundZ(wet));
        }
    }

    @Test
    void unitScaleIsIndependentOfHexScaleAndInvalidDimensionsAreRejected() {
        BoardGeometry.Tuning original = BoardGeometry.tuning();
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(2, 0.75f, 1.2f, 20, 1));
            assertEquals(40, BoardGeometry.LEVEL);
            assertEquals(0.75f, BoardGeometry.UNIT_SCALE);
            assertEquals(1.2f, BoardGeometry.UNIT_HEIGHT_SCALE);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                  () -> BoardGeometry.tune(new BoardGeometry.Tuning(Float.NaN, 1, 1, 18, 1)));
        } finally {
            BoardGeometry.tune(original);
        }
    }
}
