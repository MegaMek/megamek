/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 2 && y == 2 ? raisedLevel : 0, null));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "Movement", List.of());
    }

    /** A 5x5 scene with (2,2) and (3,2) at the given levels and every other tile at zero. */
    private BoardScene pairScene(int first, int second) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                Coords coords = new Coords(x, y);
                int elevation = coords.equals(new Coords(2, 2)) ? first
                      : coords.equals(new Coords(3, 2)) ? second : 0;
                tiles.add(new BoardScene.Tile(coords, elevation, null));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "Movement", List.of());
    }

    /** The edge of {@code from} whose neighbor is {@code to}. */
    private static int edgeBetween(Coords from, Coords to) {
        for (int edge = 0; edge < 6; edge++) {
            if (from.translated(BoardGeometry.edgeDirection(edge)).equals(to)) {
                return edge;
            }
        }
        throw new IllegalArgumentException("Not neighbors: " + from + " and " + to);
    }

    @Test
    void paddingLeavesTheConfiguredGapBetweenFlatHexSurfaces() {
        assertEquals(BoardGeometry.PADDING * BoardGeometry.WIDTH, BoardGeometry.GAP, 0.001f,
              "The padding is a fraction of the flat hex width");
        float measured = BoardGeometry.corner(new Coords(0, 0), 0, 4).y
              - BoardGeometry.corner(new Coords(0, 1), 0, 1).y;
        assertEquals(BoardGeometry.GAP, measured, 0.001f, "Facing hex edges must keep the padding gap");
        assertEquals(BoardGeometry.GAP, BoardGeometry.CELL_HEIGHT - BoardGeometry.HEIGHT, 0.001f,
              "The lattice spacing adds the gap to the flat hex surface");
        // The uniform lattice keeps about the same gap between diagonal neighbors.
        float diagonal = (float) Math.hypot(
              BoardGeometry.centerX(new Coords(1, 0)) - BoardGeometry.centerX(new Coords(0, 1)),
              BoardGeometry.centerY(new Coords(1, 0)) - BoardGeometry.centerY(new Coords(0, 1)));
        float tight = (float) Math.hypot(BoardGeometry.TILE_WIDTH * 0.75f, BoardGeometry.TILE_HEIGHT / 2)
              * BoardGeometry.HEX_SCALE;
        assertEquals(BoardGeometry.GAP, diagonal - tight, BoardGeometry.GAP * 0.05f,
              "Diagonal neighbors must keep about the same gap");
    }

    @Test
    void slopesAndCliffsFollowTheThreeLevelRule() {
        assertFalse(BoardGeometry.isCliffPair(3, 5));
        assertFalse(BoardGeometry.isCliffPair(3, 3));
        assertTrue(BoardGeometry.isCliffPair(3, 6));
        assertTrue(BoardGeometry.hasCliff(6, 3));
        assertFalse(BoardGeometry.hasCliff(3, 6), "Only the raised hex draws the face");
        assertEquals(2f, BoardGeometry.padHeight(2, 2), 0.001f,
              "Same-level neighbors keep the padding at their shared level");
        assertEquals(1f, BoardGeometry.padHeight(0, 2), 0.001f, "Slope halves meet at one midline level");
        assertEquals(1f, BoardGeometry.padHeight(2, 0), 0.001f);
        assertEquals(4f, BoardGeometry.padHeight(4, 0), 0.001f, "A cliff protrudes flat at its own level");
        assertEquals(0f, BoardGeometry.padHeight(0, 4), 0.001f);
    }

    @Test
    void sameLevelNeighborsKeepThePaddingFlat() {
        Coords first = new Coords(2, 2);
        Coords second = new Coords(3, 2);
        int edge = edgeBetween(first, second);
        int reverse = edgeBetween(second, first);
        // The third tile at each corner of the pair is lower, which must not dent a flat join.
        BoardScene flat = pairScene(2, 2);
        assertEquals(2 * BoardGeometry.LEVEL,
              BoardGeometry.padPoint(new Vector3(), flat, first, 2, edge, 0.5f, 1).z, 0.001f,
              "Same-level neighbors keep the padding at that level");
        BoardScene slope = pairScene(2, 0);
        assertEquals(BoardGeometry.padHeight(2, 0) * BoardGeometry.LEVEL,
              BoardGeometry.padPoint(new Vector3(), slope, first, 2, edge, 0.5f, 1).z, 0.001f,
              "A slope reaches the average level at the midline");
        assertEquals(BoardGeometry.padHeight(0, 2) * BoardGeometry.LEVEL,
              BoardGeometry.padPoint(new Vector3(), slope, second, 0, reverse, 0.5f, 1).z, 0.001f,
              "and the neighbor's half reaches the same line");
        BoardScene cliff = pairScene(4, 0);
        assertEquals(4 * BoardGeometry.LEVEL,
              BoardGeometry.padPoint(new Vector3(), cliff, first, 4, edge, 0.5f, 1).z, 0.001f,
              "A cliff protrudes flat at the raised level");
        assertEquals(0f, BoardGeometry.padPoint(new Vector3(), cliff, second, 0, reverse, 0.5f, 1).z, 0.001f,
              "and the low side stays flat at its own level");
    }

    @Test
    void cornerCapsCloseRampCornersAndCliffsKeepTheirFaces() {
        Coords first = new Coords(2, 2);
        Coords second = new Coords(3, 2);
        int edge = edgeBetween(first, second);
        // (2,2) beside 0-level tiles: every corner is a plain ramp corner and gets a slanted cap.
        BoardScene ramp = pairScene(2, 2);
        assertEquals(BoardGeometry.CORNER_BEVEL, BoardGeometry.cornerBevel(ramp, first, 2, edge), 0.001f,
              "Ramp corners are closed by a slanted cap");
        assertEquals(4f / 3f, BoardGeometry.cornerHeight(ramp, first, 2, edge), 0.001f,
              "The cap meets at the average of the three tiles");
        // The band stops short of the lattice corner so the cap can take over the last part of the edge.
        Vector3 end = BoardGeometry.padPoint(new Vector3(), ramp, first, 2, edge, 0, 1);
        Vector3 corner = BoardGeometry.cellCorner(first, 2, edge);
        Vector3 other = BoardGeometry.cellCorner(first, 2, edge + 1);
        float edgeLength = (float) Math.hypot(corner.x - other.x, corner.y - other.y);
        float cut = (float) Math.hypot(end.x - corner.x, end.y - corner.y);
        assertEquals(BoardGeometry.CORNER_BEVEL * edgeLength, cut, edgeLength * 0.01f,
              "The cap starts at the configured fraction of the cell edge");
        // A cliff corner keeps the full edge so the raised face still drops at the lattice corner.
        BoardScene cliff = pairScene(4, 0);
        assertEquals(0f, BoardGeometry.cornerBevel(cliff, first, 4, edge), 0.001f,
              "Cliff corners keep their vertical faces");
    }

    @Test
    void paddingBandsStartAtTheHexEdgeAndMeetTheSharedMidline() {
        BoardScene scene = scene(2);
        Coords raised = new Coords(2, 2);
        Vector3 start = BoardGeometry.padPoint(new Vector3(), scene, raised, 2, 1, 0.5f, 0);
        assertEquals(2 * BoardGeometry.LEVEL, start.z, 0.001f, "Interpolation starts flat at the hex edge");
        Vector3 midline = BoardGeometry.padPoint(new Vector3(), scene, raised, 2, 1, 0.5f, 1);
        assertEquals(BoardGeometry.padHeight(2, 0) * BoardGeometry.LEVEL, midline.z, 0.001f,
              "A band meets its neighbor at the shared midline level");
        assertTrue(midline.z < start.z, "A ramp toward lower neighbors descends");
    }

    @Test
    void bothHalvesOfABandMeetOnTheSameMidlinePoint() {
        BoardScene scene = scene(2);
        Coords raised = new Coords(2, 2);
        Coords low = raised.translated(0);
        Vector3 first = BoardGeometry.padPoint(new Vector3(), scene, raised, 2, edgeBetween(raised, low), 0.5f, 1);
        Vector3 second = BoardGeometry.padPoint(new Vector3(), scene, low, 0, edgeBetween(low, raised), 0.5f, 1);
        assertEquals(first.x, second.x, 0.001f, "The shared midline is one line");
        assertEquals(first.y, second.y, 0.001f, "The shared midline is one line");
        assertEquals(first.z, second.z, 0.001f, "and both halves reach the same height on it");
    }

    @Test
    void paddingBandsAndSlopesStayPickable() {
        // Halfway up the raised hex's ramp toward a lower neighbor, on its center line.
        Coords raised = new Coords(2, 2);
        BoardScene sloped = scene(2);
        Vector3 point = BoardGeometry.padPoint(new Vector3(), sloped, raised, 2, 1, 0.5f, 0.5f);
        assertEquals(raised, BoardGeometry.pick(sloped,
              new Ray(new Vector3(point).add(0, 1000, 0), new Vector3(0, -1, 0))));
        // A flat band still belongs to the cell whose half was clicked.
        BoardScene flat = scene(0);
        Vector3 flatPad = BoardGeometry.padPoint(new Vector3(), flat, raised, 0, 1, 0.5f, 0.5f);
        assertEquals(raised, BoardGeometry.pick(flat,
              new Ray(flatPad.add(0, 0, 500), new Vector3(0, 0, -1))));
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
        // The raise must stand above the sight line over the padding band that now separates the hexes.
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
    void tuningRecomputesEveryDerivedValueAndAnnouncesItself() {
        BoardGeometry.Tuning original = new BoardGeometry.Tuning(BoardGeometry.PADDING, BoardGeometry.HEX_SCALE,
              BoardGeometry.INTERPOLATION_INSET, BoardGeometry.CORNER_BEVEL, BoardGeometry.BAND_NORMAL_FLATNESS,
              BoardGeometry.UNIT_SCALE, BoardGeometry.UNIT_HEIGHT_SCALE, BoardGeometry.BASE_LEVEL_HEIGHT,
              BoardGeometry.HEX_FRAME_SHADE);
        int revision = BoardGeometry.revision();
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(0f, 2f, 0f, 0.2f, 0.25f, 2f, 1.5f, 20, 0.5f));
            assertEquals(BoardGeometry.TILE_WIDTH * 2f, BoardGeometry.WIDTH, 0.001f);
            assertEquals(BoardGeometry.TILE_HEIGHT * 2f, BoardGeometry.HEIGHT, 0.001f);
            assertEquals(20 * 2f, BoardGeometry.LEVEL, 0.001f);
            assertEquals(0.2f, BoardGeometry.CORNER_BEVEL, 0.001f);
            assertEquals(2f, BoardGeometry.UNIT_SCALE, 0.001f);
            assertEquals(0.5f, BoardGeometry.HEX_FRAME_SHADE, 0.001f);
            assertEquals(0, BoardGeometry.GAP, 0.001f);
            assertFalse(BoardGeometry.HAS_PADDING, "A tight tiling has no padding");
            assertEquals(revision + 1, BoardGeometry.revision(), "The renderers must see the change");

            BoardGeometry.tune(new BoardGeometry.Tuning(0.2f, 2f, 0.05f, 0.1f, 0.5f, 1f, 0.9f, 14, 1f));
            assertTrue(BoardGeometry.HAS_PADDING);
            assertEquals(1f, BoardGeometry.HEX_FRAME_SHADE, 0.001f, "Full transparency must reach the renderer");
            assertEquals(BoardGeometry.PADDING * BoardGeometry.WIDTH, BoardGeometry.GAP, 0.001f);
            assertEquals(BoardGeometry.GAP, BoardGeometry.CELL_HEIGHT - BoardGeometry.HEIGHT, 0.001f,
                  "The gap stays the difference between the lattice cell and the hex surface");
        } finally {
            BoardGeometry.tune(original);
        }
        assertEquals(revision + 3, BoardGeometry.revision(), "Restoring the original tuning is a tuning too");
    }
}
