/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Exercises shared tactical painters over elevated terrain in the native renderer. */
@Tag("on-demand")
class GpuTacticalSmokeTest {
    @Test
    void deploymentEcmAndMeasurementCursorFollowTheHexSurface() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            fixture.addEcm();
            Coords marked = new Coords(4, 4);
            Player enemy = new Player(1, "Enemy deployment");
            enemy.setTeam(2);
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.addPlayer(enemy.getId(), enemy);
                fixture.player.setStartingPos(Board.START_W);
                enemy.setStartingPos(Board.START_W);
                fixture.game.setPhase(GamePhase.DEPLOYMENT);
                fixture.view.markDeploymentHexesFor(fixture.entity);
                fixture.view.checkLOS(marked);
                fixture.source.refresh();
            });
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                @Override
                public void render() {
                    try {
                        super.render();
                        File output = new File(System.getProperty("megamek.gpu.screenshots"));
                        if (frames() == 1) {
                            boardCamera.setIsometric(true);
                            boardCamera.center(BoardGeometry.center(marked, 2));
                            boardCamera.zoom(0.26f);
                        } else if (frames() == 4) {
                            GpuBoardTestUi.capture(new File(output, "deployment-ecm-friendly.png"));
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.entity.setOwner(enemy);
                                fixture.view.updateEcmList();
                                fixture.source.refresh();
                            });
                        } else if (frames() == 8) {
                            GpuBoardTestUi.capture(new File(output, "deployment-ecm-enemy.png"));
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    @Test
    void rendersTerrainFollowingShapesInBothCamerasAndReusesGeometry() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                GpuTacticalTest.overlays(fixture);
                fixture.source.refresh();
            });
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;
                private GpuTactical probe;
                private long builds;

                @Override
                public void render() {
                    try {
                        super.render();
                        tick++;
                        if (probe == null) {
                            probe = new GpuTactical();
                            probe.update(fixture.source.takeFrame().scene());
                            builds = probe.builds();
                        }
                        probe.update(fixture.source.takeFrame().scene());
                        assertEquals(builds, probe.builds(), "Unchanged geometry must survive new frames and cameras");
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        if (tick == 15) {
                            boardCamera.setIsometric(false);
                            boardCamera.zoom(0.6f);
                        } else if (tick == 35) {
                            GpuBoardTestUi.capture(new File(output, "native-tactical-top.png"));
                            boardCamera.setIsometric(true);
                        } else if (tick == 60) {
                            GpuBoardTestUi.capture(new File(output, "native-tactical-isometric.png"));
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void dispose() {
                    if (probe != null) {
                        probe.dispose();
                    }
                    super.dispose();
                }
            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) {
            throw new AssertionError("Native tactical rendering failed", failure.get());
        }
    }
}
