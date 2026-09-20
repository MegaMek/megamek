/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import megamek.common.Hex;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Checks the complete renderer's scene/terrain boundary and its cached footprint pose in both cameras. */
@Tag("on-demand")
class GpuScenePlaybackSmokeTest {
    @Test
    void queuedTerrainAndMovingFootprintsUseThePresentedSceneInBothCameras() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        try (var fixture = GpuBoardFixture.create()) {
            var start = new Coords(4, 4);
            var middle = new Coords(4, 6);
            var end = new Coords(4, 8);
            var changed = new Coords(5, 5);
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.setPosition(start);
                fixture.game.getBoard().setHex(changed, new Hex(0));
                fixture.source.refresh();
            });
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private UnitPlayback playback;

                @Override
                public void render() {
                    try {
                        super.render();
                        if (frames() == 1) {
                            playback = (UnitPlayback) field(this, "playback");
                            playback.togglePaused();
                            boardCamera.setIsometric(false);
                            boardCamera.center(BoardGeometry.center(changed, 0));
                            boardCamera.zoom(.35f);
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.game.getBoard().setHex(changed, new Hex(1));
                                GpuSceneSourceTest.move(fixture, start, middle);
                                fixture.game.getBoard().setHex(changed, new Hex(2));
                                GpuSceneSourceTest.move(fixture, middle, end);
                            });
                        } else if (frames() == 2) {
                            playback.togglePaused();
                            playback.advance(0, UnitMotion.Speed.NORMAL);
                            playback.advance(playback.motions.get(1).remainingSeconds() / (2 * UnitMotion.Speed.NORMAL.rate),
                                  UnitMotion.Speed.NORMAL);
                            playback.togglePaused();
                        } else if (frames() == 3 || frames() == 4) {
                            var scene = (BoardScene) field(this, "scene");
                            assertEquals(0, scene.tile(changed).elevation());
                            var poses = (Map<?, ?>) field(this, "unitFootprints");
                            var pose = (UnitFootprint.Pose) poses.values().iterator().next();
                            assertTrue(pose.position().epsilonEquals(playback.motions.get(1).surfacePosition(scene), .001f));
                            assertTrue(pose.position().dst(playback.motions.get(1).destination()) > BoardGeometry.HEIGHT / 2);
                            GpuBoardTestUi.capture(new File(output, frames() == 3 ? "scene-playback-top.png" : "scene-playback-isometric.png"));
                            if (frames() == 3) {
                                boardCamera.setIsometric(true);
                            } else {
                                playback.togglePaused();
                                playback.advance(playback.motions.get(1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate,
                                      UnitMotion.Speed.NORMAL);
                                playback.togglePaused();
                            }
                        } else if (frames() == 5) {
                            assertEquals(1, ((BoardScene) field(this, "scene")).tile(changed).elevation());
                            assertSettledPose(this, middle);
                            GpuBoardTestUi.capture(new File(output, "scene-playback-first-arrival.png"));
                            playback.finish();
                        } else if (frames() == 6) {
                            assertEquals(2, ((BoardScene) field(this, "scene")).tile(changed).elevation());
                            assertSettledPose(this, end);
                            Gdx.app.exit();
                        }
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static void assertSettledPose(GpuBattleView view, Coords coords) throws ReflectiveOperationException {
        var poses = (Map<?, ?>) field(view, "unitFootprints");
        var pose = (UnitFootprint.Pose) poses.values().iterator().next();
        assertEquals(coords, pose.unit().location().coords());
        assertTrue(pose.position().epsilonEquals(BoardGeometry.center(coords, pose.unit().location().elevation()), .001f));
    }

    private static Object field(GpuBattleView view, String name) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(view);
    }
}
