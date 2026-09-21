/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.overlay.UnitOverviewOverlay;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.ResolvedAttack;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real camera/playback integration and posed model coverage beside the native, resizable side panels. */
@Tag("on-demand")
class GpuCombatCameraSmokeTest {
    @Test
    void splitFireStaysVisibleBesideFiringAndResizedReportPanelsInObliqueAndTopViews() throws Exception {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<List<BoardScene.Animation>> pending = new AtomicReference<>(List.of());
        var preferences = GUIPreferences.getInstance();
        boolean overviewVisible = preferences.getShowUnitOverview();
        AtomicReference<UnitOverviewOverlay> overview = new AtomicReference<>();
        AtomicReference<UnitOverviewOverlay> enemies = new AtomicReference<>();
        Hex[] hexes = new Hex[20 * 20];
        for (int i = 0; i < hexes.length; i++) { hexes[i] = new Hex(i % 20 >= 14 ? 2 : 0); }
        try (var fixture = GpuBoardFixture.create(new Board(20, 20, hexes))) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    preferences.setShowUnitOverview(true);
                    overview.set(new UnitOverviewOverlay(GpuUnitHudTest.gui(fixture)));
                    fixture.view.addOverlay(overview.get());
                    enemies.set(new UnitOverviewOverlay(GpuUnitHudTest.gui(fixture), true));
                    fixture.view.addOverlay(enemies.get());
                    fixture.game.setPhase(GamePhase.FIRING);
                    fixture.entity.setPosition(new Coords(2, 3));
                    Player enemy = new Player(2, "Opponent");
                    enemy.setTeam(2);
                    fixture.game.addPlayer(enemy.getId(), enemy);
                    GpuFiringCaptureTest.addTarget(fixture, 42, new Coords(17, 5)).setOwner(enemy);
                    GpuFiringCaptureTest.addTarget(fixture, 43, new Coords(13, 17)).setOwner(enemy);
                    fixture.source.refresh();
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
            });
            var source = spy(fixture.source);
            doAnswer(invocation -> {
                fixture.source.setViewport(invocation.getArgument(0), invocation.getArgument(1),
                      invocation.getArgument(2), invocation.getArgument(3));
                SwingUtilities.invokeAndWait(fixture.source::refresh);
                return null;
            }).when(source).setViewport(anyInt(), anyInt(), anyInt(), anyInt());
            // Supply the existing firing-panel snapshot; this test exercises its actual native layout and viewport.
            var attackPanel = new BoardScene.Attack("Split fire: two targets", "Medium Laser", "", 0,
                  List.of("Fire at both distant targets"));
            doAnswer(ignored -> {
                var frame = fixture.source.takeFrame();
                List<BoardScene.Animation> timeline = new ArrayList<>(frame.timeline());
                timeline.addAll(pending.getAndSet(List.of()));
                return new GpuBoardSource.Frame(frame.scene(), timeline, frame.context(), frame.globalCommands(),
                      frame.hud(), frame.tooltip(), frame.centerRequest(), frame.boardGeneration(), frame.actorName(),
                      frame.scenarioAtmosphere(), attackPanel, frame.reports());
            }).when(source).takeFrame();
            new Lwjgl3Application(new GpuBattleView(source) {
                private int step;
                private int ticks;
                private int resizedAt;
                private boolean checkedTuning;
                private List<BoardScene.Animation> shots;

                @Override
                public void render() {
                    try {
                        super.render();
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        assertTrue(++ticks < 500, "Combat camera review must finish");
                        var playback = (UnitPlayback) field(this, "playback");
                        var ui = (GpuBoardUi) field(this, "ui");
                        if (boardCamera.isFraming()) {
                            playback.attacks().forEach(shot -> assertTrue(shot.seconds <= 0,
                                  "No weapon animation may advance before the camera is ready"));
                        }
                        if (step == 0) {
                            var scene = (BoardScene) field(this, "scene");
                            assertTrue(ui.cameraWidth() > boardCamera.camera.viewportWidth / 3,
                                  "The initial fit must use the laid-out board area, not a placeholder HUD viewport");
                            assertTrue(boardCamera.camera.zoom < 10, "Opening the window must not zoom far beyond the board");
                            var attacker = scene.units().stream().filter(unit -> unit.id() == 1).findFirst().orElseThrow();
                            shots = scene.units().stream().filter(unit -> unit.id() != 1)
                                  .<BoardScene.Animation>map(target -> UnitPlaybackTest.attack(attacker, target,
                                        ResolvedAttack.Kind.SHOT, true)).toList();
                            assertEquals(2, shots.size());
                            boardCamera.setIsometric(true);
                            boardCamera.orbit(25, 25);
                            boardCamera.center(BoardGeometry.center(attacker.location().coords(), attacker.location().elevation()));
                            boardCamera.zoom(.12f);
                            set(this, "playbackSpeed", UnitMotion.Speed.QUADRUPLE);
                            pending.set(shots);
                            step++;
                        } else if (step == 1 && firing(playback)) {
                            playback.togglePaused();
                            assertCoverage(ui, "attack-panel");
                            GpuBoardTestUi.capture(new File(output, "combat-camera-firing-oblique.png"));
                            GpuBoardTestUi.click("battle-report-toggle");
                            step++;
                        } else if (step == 2) {
                            assertTrue(panel("battle-report").isVisible());
                            assertFalse(panel("attack-panel").isVisible());
                            dragReportWidth(620);
                            step++;
                        } else if (step == 3) {
                            assertEquals(620, panel("battle-report").getWidth(), 1);
                            if (!checkedTuning) {
                                assertCoverage(ui, "battle-report");
                                GpuBoardTestUi.capture(new File(output, "combat-camera-report-oblique.png"));
                                GpuBoardTestUi.click("tuning");
                                checkedTuning = true;
                                return;
                            }
                            assertTrue(panel("board-tuning").isVisible());
                            assertFalse(panel("battle-report").isVisible());
                            assertFalse(panel("attack-panel").isVisible());
                            assertEquals(GpuPanelDock.WIDTH, panel("board-tuning").getWidth(), 1);
                            assertNull(panel("board-tuning").findActor("dock-resize"));
                            assertCoverage(ui, "board-tuning");
                            GpuBoardTestUi.capture(new File(output, "shared-dock-tuning.png"));
                            GpuBoardTestUi.click("tuning");
                            assertTrue(panel("attack-panel").isVisible(), "Closing a utility panel restores attack controls");
                            assertEquals(GpuPanelDock.WIDTH, panel("attack-panel").getWidth(), 1,
                                  "Resizing Report must not change firing declarations");
                            assertNull(panel("attack-panel").findActor("dock-resize"));
                            GpuBoardTestUi.click("battle-report-toggle");
                            resizedAt = ticks;
                            step++;
                            // A native resize may reenter render before setWindowedMode returns.
                            Gdx.graphics.setWindowedMode(900, 680);
                        } else if (step == 4 && ticks > resizedAt + 2) {
                            assertEquals(900, Gdx.graphics.getWidth());
                            assertCoverage(ui, "battle-report");
                            GpuBoardTestUi.capture(new File(output, "combat-camera-report-small.png"));
                            GpuBoardTestUi.click("report-close");
                            nextVolley(playback, BoardCamera.ATTACK_TOP_VIEW_TILT_DEGREES, 73);
                            step++;
                        } else if (step == 5) {
                            assertEquals(BoardCamera.ATTACK_TOP_VIEW_TILT_DEGREES, boardCamera.tilt(), .001f);
                            assertEquals(73, boardCamera.azimuth(), .001f);
                            if (firing(playback)) {
                                playback.togglePaused();
                                assertCoverage(ui, "attack-panel");
                                GpuBoardTestUi.capture(new File(output, "combat-camera-firing-threshold.png"));
                                GpuBoardTestUi.click("battle-report-toggle");
                                nextVolley(playback, 0, 0);
                                step++;
                            }
                        } else if (step == 6) {
                            assertEquals(0, boardCamera.tilt());
                            assertEquals(0, boardCamera.azimuth());
                            if (firing(playback)) {
                                playback.togglePaused();
                                assertCoverage(ui, "battle-report");
                                GpuBoardTestUi.capture(new File(output, "combat-camera-report-top.png"));
                                float scale = Gdx.graphics.getWidth() / ui.stage.getWidth();
                                var hud = fixture.source.takeFrame().hud();
                                for (float density : new float[] { 1, 1.5f, 2 }) {
                                    ui.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), density);
                                    ui.updateHud(hud, System.nanoTime());
                                    assertSidebarGap(ui, panel("battle-report"));
                                    assertEquals(panel("battle-report").getRight(), panel("board-tuning").getRight());
                                    assertEquals(Math.min(GpuPanelDock.WIDTH, panel("battle-report").getWidth()),
                                          panel("attack-panel").getWidth());
                                }
                                ui.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), scale);
                                ui.updateHud(hud, System.nanoTime());
                                dragReportWidth(5000);
                                assertEquals(ui.cameraLeft() / scale, panel("battle-report").getX(), 1,
                                      "The dock cannot be dragged across the enemy strip");
                                assertSidebarGap(ui, panel("battle-report"));
                                SwingUtilities.invokeAndWait(() -> {
                                    preferences.setShowUnitOverview(false);
                                    fixture.source.refresh();
                                });
                                step++;
                            }
                        } else if (step == 7) {
                            assertEquals(GpuPanelDock.MARGIN,
                                  ui.stage.getWidth() - panel("battle-report").getRight(), .01f);
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                private void nextVolley(UnitPlayback playback, float tilt, float orbit) {
                    playback.finish();
                    if (playback.paused()) { playback.togglePaused(); }
                    boardCamera.setIsometric(false);
                    boardCamera.orbit(orbit, tilt);
                    boardCamera.zoom(.2f);
                    pending.set(shots);
                }

                @SuppressWarnings("unchecked")
                private void assertCoverage(GpuBoardUi ui, String panelName) throws Exception {
                    var panel = panel(panelName);
                    assertTrue(panel.isVisible());
                    assertSidebarGap(ui, panel);
                    float scale = Gdx.graphics.getWidth() / ui.stage.getWidth();
                    assertEquals((panel.getX() - GpuPanelDock.CAMERA_GAP) * scale - ui.cameraLeft(), ui.cameraWidth(), .01f);
                    assertTrue(ui.cameraWidth() < boardCamera.camera.viewportWidth - 200);
                    var instances = (Map<String, ModelInstance>) field(this, "unitInstances");
                    for (int id : new int[] { 1, 42, 43 }) {
                        var instance = instances.entrySet().stream().filter(entry -> entry.getKey().startsWith(id + ":"))
                              .findFirst().orElseThrow().getValue();
                        var bounds = UnitBounds.world(instance);
                        for (float x : new float[] { bounds.min.x, bounds.max.x }) {
                            for (float y : new float[] { bounds.min.y, bounds.max.y }) {
                                for (float z : new float[] { bounds.min.z, bounds.max.z }) {
                                    var point = boardCamera.camera.project(new Vector3(x, y, z), 0, 0,
                                          boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight);
                                    assertTrue(point.x > ui.cameraLeft() && point.x < ui.cameraLeft() + ui.cameraWidth() && point.y > 0
                                          && point.y < boardCamera.camera.viewportHeight,
                                          () -> "Unit " + id + " clipped or behind " + panelName + ": " + point);
                                }
                            }
                        }
                    }
                }
            }, GpuBoardWindow.configuration(false));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (overview.get() != null) { preferences.removePreferenceChangeListener(overview.get()); }
                if (enemies.get() != null) { preferences.removePreferenceChangeListener(enemies.get()); }
                preferences.setShowUnitOverview(overviewVisible);
            });
        }
        if (failure.get() != null) { throw new AssertionError("Combat camera framing failed", failure.get()); }
    }

    private static boolean firing(UnitPlayback playback) {
        return playback.attack() != null && playback.attack().seconds > UnitAttack.ANTICIPATION_SECONDS;
    }

    private static void assertSidebarGap(GpuBoardUi ui, Table panel) {
        Group hud = (Group) ui.stage.getRoot().getChildren().first();
        Actor card = hud.getChildren().first();
        // Native artwork has symmetric transparent padding around each card, so its two gutters stay equal too.
        float outside = ui.stage.getWidth() - card.getRight();
        assertTrue(outside > 0);
        assertEquals(outside, card.getX() - panel.getRight(), 1,
              "The unit strip must have equal gaps to the panel and the window edge");
    }

    private static Table panel(String name) { return GpuBoardTestUi.stage().getRoot().findActor(name); }

    private static void dragReportWidth(float width) {
        Table panel = panel("battle-report");
        Actor edge = panel.findActor("dock-resize");
        Vector2 from = edge.localToStageCoordinates(new Vector2(edge.getWidth() / 2, edge.getHeight() / 2));
        Vector2 to = from.cpy().add(panel.getWidth() - width, 0);
        GpuBoardTestUi.stage().stageToScreenCoordinates(from);
        GpuBoardTestUi.stage().stageToScreenCoordinates(to);
        var input = Gdx.input.getInputProcessor();
        input.touchDown(Math.round(from.x), Math.round(from.y), 0, Input.Buttons.LEFT);
        input.touchDragged(Math.round(to.x), Math.round(to.y), 0);
        input.touchUp(Math.round(to.x), Math.round(to.y), 0, Input.Buttons.LEFT);
    }

    private static Object field(GpuBattleView view, String name) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(view);
    }

    private static void set(GpuBattleView view, String name, Object value) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(view, value);
    }
}
