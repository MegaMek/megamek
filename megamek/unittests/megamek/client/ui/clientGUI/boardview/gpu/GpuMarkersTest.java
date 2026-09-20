/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.client.ui.clientGUI.boardview.BoardMarker;
import megamek.client.ui.clientGUI.boardview.sprite.CollapseWarningSprite;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GpuMarkersTest {
    @BeforeAll
    static void loadMathNatives() {
        GdxNativesLoader.load();
    }

    @Test
    void overheadMarkersStayUprightAndStillAtEveryCameraBearing() {
        BoardCamera camera = new BoardCamera();
        for (float tilt : new float[] { 0, GpuMarkers.FLAT_TILT_DEGREES - 0.1f }) {
            for (int bearing = 0; bearing < 360; bearing += 30) {
                camera.setIsometric(false);
                camera.orbit(bearing, tilt);
                assertTrue(GpuMarkers.flat(camera.camera));
                Matrix4 start = new Matrix4();
                GpuMarkers.transform(start, camera.camera, Vector3.Zero, 0);
                Matrix4 later = new Matrix4();
                GpuMarkers.transform(later, camera.camera, Vector3.Zero, 3);
                assertArrayEquals(start.val, later.val, 0.0001f, "Overhead markers must not spin");
                assertEquals(1, new Vector3(Vector3.Y).rot(start).nor().dot(camera.camera.up), 0.0001f);
                assertEquals(-1, new Vector3(Vector3.Z).rot(start).nor().dot(camera.camera.direction), 0.0001f,
                      "The face must remain perpendicular to the viewing direction, not just the board");
            }
        }
    }

    @Test
    void obliqueMarkersSpinAboutWorldVerticalAndRepeatWithoutChangingTheirPosition() {
        BoardCamera camera = new BoardCamera();
        camera.setIsometric(false);
        camera.orbit(125, GpuMarkers.FLAT_TILT_DEGREES + 0.1f);
        assertFalse(GpuMarkers.flat(camera.camera));
        Vector3 previousNormal = null;
        Matrix4 first = new Matrix4();
        for (int quarter = 0; quarter <= 4; quarter++) {
            float time = quarter * GpuMarkers.SPIN_PERIOD_SECONDS / 4;
            Matrix4 transform = new Matrix4();
            GpuMarkers.transform(transform, camera.camera, new Vector3(12, 23, 144), time);
            if (quarter == 0) {
                first.set(transform);
            } else if (quarter == 4) {
                assertArrayEquals(first.val, transform.val, 0.0001f, "A full period must return to the initial pose");
            }
            assertEquals(1, new Vector3(Vector3.Y).rot(transform).nor().z, 0.0001f);
            Vector3 center = new Vector3(0, 0, 0.5f).mul(transform);
            assertEquals(12, center.x, 0.0001f);
            assertEquals(23, center.y, 0.0001f);
            Vector3 normal = new Vector3(Vector3.Z).rot(transform).nor();
            if (previousNormal != null) {
                assertEquals(0, normal.dot(previousNormal), 0.0001f, "Quarter turns must be visible");
            }
            previousNormal = normal;
        }
    }

    @Test
    void markerVolumeAlwaysClearsItsSupportAcrossCameraAnglesAndBoardScales() {
        BoardGeometry.Tuning original = BoardGeometry.tuning();
        try {
            for (float scale : new float[] { 0.5f, 1, 2 }) {
                BoardGeometry.tune(new BoardGeometry.Tuning(scale, 0.6f, 0.87f, 30, 0.8f));
                BoardCamera camera = new BoardCamera();
                for (int tilt = 0; tilt <= 80; tilt += 10) {
                    camera.setIsometric(false);
                    camera.orbit(73, tilt);
                    Vector3 support = new Vector3(0, 0, 7 * BoardGeometry.LEVEL);
                    Matrix4 transform = new Matrix4();
                    GpuMarkers.transform(transform, camera.camera, support, 1.3f);
                    for (float x : new float[] { -48, 48 }) {
                        for (float y : new float[] { -48, 48 }) {
                            for (float z : new float[] { 0, 1 }) {
                                assertTrue(new Vector3(x, y, z).mul(transform).z > support.z,
                                      "Even the lowest edge must clear the roof or deck");
                            }
                        }
                    }
                }
            }
        } finally {
            BoardGeometry.tune(original);
        }
    }

    @Test
    void locationMarkersClearRenderedUnitsWithoutLeavingTheirHex() {
        Coords coords = new Coords(3, 3);
        Vector3 ground = BoardGeometry.center(coords, 0);
        BoundingBox standing = new BoundingBox(new Vector3(ground).add(-20, -25, 0),
              new Vector3(ground).add(20, 25, 2 * BoardGeometry.LEVEL));
        // A second, raised model overlaps this hex even though its origin lies outside it.
        BoundingBox raised = new BoundingBox(new Vector3(ground).add(15, -10, 3 * BoardGeometry.LEVEL),
              new Vector3(ground).add(100, 10, 6 * BoardGeometry.LEVEL));
        BoundingBox distant = new BoundingBox(new Vector3(ground).add(200, 200, 0),
              new Vector3(ground).add(240, 240, 40 * BoardGeometry.LEVEL));
        BoardCamera camera = new BoardCamera();
        for (int bearing = 0; bearing < 360; bearing += 60) {
            for (float tilt : new float[] { 0, 29.9f, 30.1f, 50, 80 }) {
                camera.setIsometric(false);
                camera.orbit(bearing, tilt);
                for (float roof : new float[] { 0, 8 }) {
                    Vector3 support = GpuMarkers.locationSupport(coords, roof,
                          GpuMarkers.occupiedBounds(coords, List.of(standing, raised, distant)));
                    assertEquals(ground.x, support.x);
                    assertEquals(ground.y, support.y);
                    Matrix4 transform = new Matrix4();
                    GpuMarkers.transform(transform, camera.camera, support, 1.3f);
                    BoundingBox marker = new BoundingBox(new Vector3(-48, -48, 0), new Vector3(48, 48, 1))
                          .mul(transform);
                    assertTrue(marker.min.z > Math.max(raised.max.z, roof * BoardGeometry.LEVEL),
                          "The entire symbol must clear both the roof and the highest rendered unit");
                    assertTrue(marker.max.z < distant.max.z, "A different hex's unit must not lift this marker");
                }
            }
        }
        Vector3 cleared = GpuMarkers.locationSupport(coords, 0, GpuMarkers.occupiedBounds(coords, List.of(distant)));
        assertEquals(ground, cleared, "A unit leaving the hex must immediately release its reserved space");
    }

    @Test
    void groupedMarkerVolumesStayInsideTheirHexAtEveryBearingAndCameraMode() {
        BoardGeometry.Tuning original = BoardGeometry.tuning();
        try {
            for (float hexScale : new float[] { 0.5f, 1, 2 }) {
                BoardGeometry.tune(new BoardGeometry.Tuning(hexScale, 0.6f, 0.87f, 18, 0.8f));
                Coords coords = new Coords(3, 3);
                Vector3 base = BoardGeometry.center(coords, 7);
                BoardCamera camera = new BoardCamera();
                for (int bearing = 0; bearing < 360; bearing += 30) {
                    for (float tilt : new float[] { 0, 29.9f, 30.1f, 50, 80 }) {
                        camera.setIsometric(false);
                        camera.orbit(bearing, tilt);
                        Vector3 right = new Vector3(camera.camera.direction).crs(camera.camera.up).nor();
                        for (boolean occupied : new boolean[] { false, true }) {
                            for (int count = 1; count <= BoardMarker.Kind.values().length; count++) {
                                for (int index = 0; index < count; index++) {
                                    Vector3 support = new Vector3(base)
                                          .add(GpuMarkers.groupOffset(camera.camera, index, count, occupied));
                                    Matrix4 transform = new Matrix4();
                                    GpuMarkers.transform(transform, camera.camera, support, 1.3f,
                                          GpuMarkers.markerSize(camera.camera, count, occupied),
                                          BoardGeometry.LEVEL * GpuMarkers.CLEARANCE_LEVELS);
                                    for (float x : new float[] { -48, 48 }) {
                                        for (float y : new float[] { -48, 48 }) {
                                            for (float z : new float[] { 0, 1 }) {
                                                Vector3 point = new Vector3(x, y, z).mul(transform);
                                                assertTrue(BoardGeometry.contains(coords, point.x, point.y),
                                                      "Every marker must remain inside the hex it describes");
                                                assertTrue(point.z > base.z, "Every row must clear its unit/roof support");
                                                if (occupied && GpuMarkers.flat(camera.camera)) {
                                                    assertTrue(Math.abs(point.sub(base).dot(right)) > BoardGeometry.HEIGHT * 0.22f,
                                                          "Side markers must leave the centre, hook and dot of the contact clear");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            BoardGeometry.tune(original);
        }
    }

    @Test
    void capturesExistingWarningsAboveBuildingsAndBridgesWithoutBakingTheClassicGlyph() throws Exception {
        Board board = new Board(7, 6);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 6; y++) {
                board.setHex(new Coords(x, y), new Hex(0));
            }
        }
        Coords building = new Coords(2, 2), bridge = new Coords(4, 3);
        board.setHex(building, new Hex(2, "building:1;bldg_elev:4;bldg_cf:10", ""));
        board.setHex(bridge, new Hex(-1, "bridge:1:9;bridge_elev:3;bridge_cf:10", ""));
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board)) {
            var warnings = List.of(new CollapseWarningSprite(fixture.view, building),
                  new CollapseWarningSprite(fixture.view, bridge));
            BoardScene before = fixture.source.takeFrame().scene();
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.addSprites(warnings);
                fixture.source.refresh();
            });
            BoardScene scene = fixture.source.takeFrame().scene();
            assertEquals(2, scene.markers().size());
            assertTrue(scene.markers().contains(new BoardMarker(BoardMarker.Kind.COLLAPSE_WARNING, building, 6)));
            assertTrue(scene.markers().contains(new BoardMarker(BoardMarker.Kind.COLLAPSE_WARNING, bridge, 2)));
            assertEquals(scene.markers(), scene.withUnits(List.of()).markers(), "Playback must retain location warnings");
            assertEquals(before.tile(building).tactical(), scene.tile(building).tactical());
            assertEquals(before.tile(bridge).tactical(), scene.tile(bridge).tactical());
            SwingUtilities.invokeAndWait(() -> {
                warnings.getFirst().setHidden(true);
                fixture.source.refresh();
            });
            assertEquals(1, fixture.source.takeFrame().scene().markers().size());
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.removeSprites(warnings);
                fixture.source.refresh();
            });
            assertTrue(fixture.source.takeFrame().scene().markers().isEmpty());
            assertEquals(before.tile(building).tactical(), fixture.source.takeFrame().scene().tile(building).tactical());
        }
    }
}
