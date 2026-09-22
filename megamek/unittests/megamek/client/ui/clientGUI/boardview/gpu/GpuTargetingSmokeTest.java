/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.panels.phaseDisplay.ActionPhaseDisplay;
import megamek.common.RangeType;
import megamek.common.ResolvedAttack;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real assignment capture, corner-band pixels and independent arrow/marker visibility through combat playback. */
@Tag("on-demand")
class GpuTargetingSmokeTest {
    @Test
    void splitTargetsFollowSelectionAndStayVisibleWhenAllArrowsHide() throws Exception {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Entity> selected = new AtomicReference<>();
        var board = GpuFiringCaptureTest.board();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.getHex(x, y).addTerrain(new Terrain(Terrains.PAVEMENT, 1));
            }
        }
        try (var fixture = GpuBoardFixture.create(board)) {
            SwingUtilities.invokeAndWait(() -> {
                GpuFiringCaptureTest.attacks(fixture);
                try {
                    GpuFiringCaptureTest.addTarget(fixture, 43, new Coords(6, 6));
                    var attacker = GpuFiringCaptureTest.addTarget(fixture, 44, new Coords(2, 6));
                    GpuFiringCaptureTest.addTarget(fixture, 45, new Coords(6, 2));
                    int weapon = attacker.getEquipmentNum(attacker.getWeaponList().getFirst());
                    fixture.view.addAttack(new WeaponAttackAction(attacker.getId(), 43, weapon));
                    fixture.view.addAttack(new WeaponAttackAction(attacker.getId(), 45, weapon));
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
                int weapon = fixture.entity.getEquipmentNum(fixture.entity.getWeaponList().getFirst());
                fixture.view.addAttack(new WeaponAttackAction(fixture.entity.getId(), 43, weapon));
                fixture.view.addSprites(List.of(new FieldOfFireSprite(fixture.view, RangeType.RANGE_SHORT,
                      fixture.entity.getPosition(), 63)));
                selected.set(fixture.entity);
                // Exercise the shared selected-unit source without a weapon panel covering the marker pixels.
                var panel = mock(ActionPhaseDisplay.class);
                when(panel.currentEntity()).thenAnswer(ignored -> selected.get());
                when(panel.getComponents()).thenReturn(new Component[0]);
                when(panel.getActionButtons()).thenReturn(List.of());
                when(panel.getCompletionButtons()).thenReturn(List.of());
                fixture.panel = panel;
                fixture.source.refresh();
            });
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                @Override
                void updateCameraFocus(BoardScene scene, BoardView.CenterRequest request) {
                    // Keep every target in view when switching actors; camera following is tested separately.
                    boardCamera.fit(scene);
                }

                @Override
                public void render() {
                    try {
                        super.render();
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        var playback = (UnitPlayback) field(this, "playback");
                        var scene = (BoardScene) field(this, "scene");
                        if (frames() == 1) {
                            playback.togglePaused();
                            clock(this, 0);
                            boardCamera.setIsometric(true);
                            boardCamera.fit(scene);
                        } else if (frames() <= 4) {
                            assertEquals(1, scene.selectedId());
                            checkOverlays(this, true, Set.of(42, 43));
                            String angle = frames() == 4 ? "top" : frames() == 2 ? "isometric-low" : "isometric-high";
                            GpuBoardTestUi.capture(new File(output, "targeting-" + angle + ".png"));
                            if (frames() == 2) {
                                assertEquals(TARGET_BOB_HEIGHT_OFFSET, targetBob(0), .001f);
                                clock(this, TARGET_BOB_PERIOD_SECONDS / 2);
                            } else if (frames() == 3) {
                                assertEquals(TARGET_BOB_HEIGHT_OFFSET + TARGET_BOB_HEIGHT_LEVELS * BoardGeometry.LEVEL,
                                      targetBob((float) field(this, "hoverClock")), .001f);
                                boardCamera.setIsometric(false);
                            } else {
                                var attacker = scene.units().stream().filter(unit -> unit.id() == 1).findFirst().orElseThrow();
                                var target = scene.units().stream().filter(unit -> unit.id() == 42).findFirst().orElseThrow();
                                playback.accept(List.of(UnitPlaybackTest.attack(attacker, target, ResolvedAttack.Kind.SHOT, true)),
                                      scene, ignored -> false);
                                playback.togglePaused();
                                playback.advance(.01, UnitMotion.Speed.NORMAL);
                                playback.togglePaused();
                            }
                        } else if (frames() == 5) {
                            assertTrue(playback.attack() != null);
                            // The playing attack is unit 1 at unit 42, so only its target carries bands.
                            checkOverlays(this, false, Set.of(42));
                            GpuBoardTestUi.capture(new File(output, "targeting-hidden-during-fire.png"));
                            // Disabling arrow hiding restores every attacker's arrows without changing the marker targets.
                            var control = (GpuFireControl) field(this, "fireControl");
                            control.update(scene, false);
                            checkArrows(control, scene, true);
                            assertTrue(control.targets(42));
                            assertTrue(control.targets(43));
                            assertFalse(control.targets(45));
                            select(fixture, selected, 44);
                        } else if (frames() == 6) {
                            assertTrue(playback.attack() != null);
                            assertEquals(44, scene.selectedId());
                            // Selecting another unit mid-playback must not move the bands to its assignments.
                            checkOverlays(this, false, Set.of(42));
                            GpuBoardTestUi.capture(new File(output, "targeting-markers-during-fire-selected-other-unit.png"));
                            playback.finish();
                        } else if (frames() == 7) {
                            assertEquals(44, scene.selectedId());
                            checkOverlays(this, true, Set.of(43, 45));
                            GpuBoardTestUi.capture(new File(output, "targeting-second-attacker.png"));
                            select(fixture, selected, 45);
                        } else if (frames() == 8) {
                            assertEquals(45, scene.selectedId());
                            checkOverlays(this, true, Set.of());
                            select(fixture, selected, Entity.NONE);
                        } else if (frames() == 9) {
                            assertEquals(Entity.NONE, scene.selectedId());
                            checkOverlays(this, true, Set.of());
                            GpuBoardTestUi.capture(new File(output, "targeting-no-selection.png"));
                            select(fixture, selected, 1);
                        } else if (frames() == 10) {
                            checkOverlays(this, true, Set.of(42, 43));
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.view.clearAllAttacks();
                                fixture.source.refresh();
                            });
                        } else {
                            checkOverlays(this, false, Set.of());
                            assertTrue(scene.firingLines().isEmpty());
                            Gdx.app.exit();
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

    private static void select(GpuBoardFixture fixture, AtomicReference<Entity> selected, int id) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            selected.set(fixture.game.getEntity(id));
            fixture.source.refresh();
        });
    }

    private static void checkOverlays(GpuBattleView view, boolean visible, Set<Integer> targets) throws ReflectiveOperationException {
        var scene = (BoardScene) field(view, "scene");
        var control = (GpuFireControl) field(view, "fireControl");
        var playback = (UnitPlayback) field(view, "playback");
        checkArrows(control, scene, visible);
        var poses = (Map<?, ?>) field(view, "unitFootprints");
        var camera = view.boardCamera.camera;
        var ui = (GpuBoardUi) field(view, "ui");
        float time = (float) field(view, "hoverClock");
        float scaleX = (float) Gdx.graphics.getBackBufferWidth() / Gdx.graphics.getWidth();
        float scaleY = (float) Gdx.graphics.getBackBufferHeight() / Gdx.graphics.getHeight();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            for (int id : List.of(42, 43, 45)) {
                if (playback.attacks().isEmpty()) {
                    assertEquals(targets.contains(id), control.targets(id),
                          "Idle bands must mark the selected unit's assigned targets");
                }
                var unit = scene.units().stream().filter(candidate -> candidate.id() == id).findFirst().orElseThrow();
                var pose = (UnitFootprint.Pose) poses.get(unit);
                int corners = 0, gaps = 0;
                for (int edge = 0; edge < 6; edge++) {
                    for (float along : new float[] { .125f, .5f, .875f }) {
                        float inset = BoardGeometry.MARKER_INSET + GpuBattleView.TARGET_BAND_WIDTH / 2;
                        Vector3 point = pose.outlinePoint(unit.location().coords(), edge, inset)
                              .lerp(pose.outlinePoint(unit.location().coords(), edge + 1, inset), along);
                        point.z += GpuBattleView.targetBob(time);
                        camera.project(point, 0, ui.bottomPixels(), camera.viewportWidth, camera.viewportHeight);
                        int rgba = pixels.getPixel(Math.round(point.x * scaleX), Math.round(point.y * scaleY));
                        if ((rgba >>> 24) > 220 && ((rgba >>> 16) & 255) < 32 && ((rgba >>> 8) & 255) < 32) {
                            if (along == .5f) { gaps++; } else { corners++; }
                        }
                    }
                }
                assertEquals(0, gaps, "The middle of each target edge must remain open");
                if (targets.contains(id) && GpuBattleView.SHOW_TARGET_MARKERS) {
                    assertTrue(corners >= 4, "Target " + id + " must have visible red corners at its bob height; found " + corners);
                } else {
                    assertEquals(0, corners, "Hidden or disabled target markers must not be drawn");
                }
            }
        } finally {
            pixels.dispose();
        }
    }

    private static void checkArrows(GpuFireControl control, BoardScene scene, boolean visible) throws ReflectiveOperationException {
        assertFalse(control.targets(1), "The attacker is not a target");
        var instance = (ModelInstance) field(control, "instance");
        int arrows = 0, ranges = 0;
        for (var part : instance.model.meshParts) {
            if (part.id.startsWith("attack-")) { arrows++; }
            if (part.id.startsWith("range-frame-")) { ranges++; }
        }
        assertEquals(visible ? scene.firingLines().size() : 0, arrows);
        assertTrue(ranges > 0, "Hiding targeting must leave weapon ranges visible");
    }

    private static void clock(GpuBattleView view, float time) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField("hoverClock");
        field.setAccessible(true);
        field.setFloat(view, time);
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        var owner = object instanceof GpuBattleView ? GpuBattleView.class : object.getClass();
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }
}
