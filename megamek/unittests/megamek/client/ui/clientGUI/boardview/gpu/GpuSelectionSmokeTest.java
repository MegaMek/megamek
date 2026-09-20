/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.loaders.MekFileParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Real framebuffer checks for the filled band, its bob, and its ground/roof/flight plane in both cameras. */
@Tag("on-demand")
class GpuSelectionSmokeTest {
    @ParameterizedTest
    @ValueSource(strings = { "ground", "roof", "airborne" })
    void selectedBandFollowsTheUnitPlaneAndRemainsWideInBothCameras(String scenario) throws Exception {
        Board board = new Board(7, 7);
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setHex(new Coords(x, y), new Hex(0));
            }
        }
        Coords coords = new Coords(3, 3);
        board.setHex(coords, new Hex(1, scenario.equals("roof") ? "building:1;bldg_elev:2;bldg_cf:100" : "", ""));
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        try (var fixture = GpuBoardFixture.create(board)) {
            boolean airborne = scenario.equals("airborne");
            var entity = airborne
                  ? new MekFileParser(new File("testresources/megamek/common/units/Cobra Transport VTOL.blk")).getEntity()
                  : fixture.entity;
            SwingUtilities.invokeAndWait(() -> {
                if (airborne) {
                    fixture.entity.setPosition(new Coords(1, 1));
                    entity.setId(2);
                    entity.setOwner(fixture.player);
                    entity.setDeployed(true);
                    fixture.game.addEntity(entity, false);
                }
                entity.setPosition(coords);
                entity.setElevation(airborne ? 3 : scenario.equals("roof") ? 2 : 0);
                var panel = mock(MovementDisplay.class);
                when(panel.currentEntity()).thenReturn(entity);
                when(panel.getComponents()).thenReturn(new Component[0]);
                when(panel.getActionButtons()).thenReturn(List.of());
                when(panel.getCompletionButtons()).thenReturn(List.of());
                fixture.panel = panel;
                fixture.source.refresh();
            });
            AtomicReference<Throwable> failure = new AtomicReference<>();
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                @Override
                public void render() {
                    try {
                        super.render();
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        if (frames() == 1) {
                            ((UnitPlayback) field(this, "playback")).togglePaused();
                            clock(this, 0);
                            boardCamera.setIsometric(true);
                            boardCamera.center(BoardGeometry.center(coords, 1 + entity.getElevation()));
                            boardCamera.zoom(.35f);
                        } else {
                            var scene = (BoardScene) field(this, "scene");
                            assertEquals(entity.getId(), scene.selectedId());
                            var unit = scene.units().stream().filter(candidate -> candidate.id() == scene.selectedId()).findFirst().orElseThrow();
                            assertEquals(airborne, unit.airborne());
                            assertEquals(1 + entity.getElevation(), unit.location().elevation(), .001f);
                            var pose = (UnitFootprint.Pose) ((Map<?, ?>) field(this, "unitFootprints")).get(unit);
                            var instance = (ModelInstance) ((Map<?, ?>) field(this, "unitInstances")).get(unit.id() + ":" + unit.part());
                            float time = (float) field(this, "hoverClock");
                            float expectedHover = airborne ? hoverOffset(time, unit.id(), unit.part()) : 0;
                            assertEquals(unit.location().elevation() * BoardGeometry.LEVEL + expectedHover, pose.position().z, .001f);
                            assertEquals(instance.transform.getTranslation(new Vector3()).z, pose.outlinePoint(coords, 0).z, .001f,
                                  "The band must use the model's plane, including cosmetic flight hover");
                            checkBandPixels(this, pose, time);
                            String angle = frames() == 4 ? "top" : frames() == 2 ? "isometric-low" : "isometric-high";
                            GpuBoardTestUi.capture(new File(output, "selection-" + scenario + "-" + angle + ".png"));
                            if (frames() == 2) {
                                clock(this, SELECTION_BOB_PERIOD_SECONDS / 2);
                            } else if (frames() == 3) {
                                boardCamera.setIsometric(false);
                            } else {
                                Gdx.app.exit();
                            }
                        }
                    } catch (Throwable error) {
                        failure.compareAndSet(null, error);
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
            assertNull(failure.get(), () -> String.valueOf(failure.get()));
        }
    }

    private static void checkBandPixels(GpuBattleView view, UnitFootprint.Pose pose, float time) throws ReflectiveOperationException {
        var camera = view.boardCamera.camera;
        var ui = (GpuBoardUi) field(view, "ui");
        float scaleX = (float) Gdx.graphics.getBackBufferWidth() / Gdx.graphics.getWidth();
        float scaleY = (float) Gdx.graphics.getBackBufferHeight() / Gdx.graphics.getHeight();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            int wideEdges = 0;
            Coords coords = pose.unit().location().coords();
            for (int edge = 0; edge < 6; edge++) {
                int cyan = 0;
                for (float across : new float[] { .2f, .5f, .8f }) {
                    float inset = BoardGeometry.MARKER_INSET + across * GpuBattleView.SELECTION_BAND_WIDTH;
                    Vector3 point = pose.outlinePoint(coords, edge, inset).lerp(pose.outlinePoint(coords, edge + 1, inset), .5f);
                    point.z += GpuBattleView.selectionBob(time);
                    camera.project(point, 0, ui.bottomPixels(), camera.viewportWidth, camera.viewportHeight);
                    int rgba = pixels.getPixel(Math.round(point.x * scaleX), Math.round(point.y * scaleY));
                    if ((rgba >>> 24) < 32 && ((rgba >>> 16) & 255) > 220 && ((rgba >>> 8) & 255) > 220) {
                        cyan++;
                    }
                }
                if (cyan == 3) { wideEdges++; }
            }
            assertTrue(wideEdges >= 3, "At least three edges must visibly span the filled band's width at its animated height; found " + wideEdges);
        } finally {
            pixels.dispose();
        }
    }

    private static void clock(GpuBattleView view, float time) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField("hoverClock");
        field.setAccessible(true);
        field.setFloat(view, time);
    }

    private static Object field(GpuBattleView view, String name) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(view);
    }
}
