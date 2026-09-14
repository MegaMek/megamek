/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.sprite.MovementEnvelopeSprite;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Opt-in native OpenGL test. Runs the actual view, saves both projections, and closes the context. */
@Tag("on-demand")
class GpuBoardSmokeTest {
    @Test
    void rendersExistingArtworkInBothViewsWithoutGlErrors() throws Exception {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            AtomicInteger boardClicks = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> fixture.view.addBoardViewListener(new BoardViewListenerAdapter() {
                @Override
                public void hexMoused(BoardViewEvent event) {
                    boardClicks.incrementAndGet();
                }
            }));
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private long topHash;
                private long movingHash;
                private long previousFrame;
                private Coords orbitTarget;
                private final List<Double> frameMillis = new ArrayList<>();

                @Override
                public void render() {
                    try {
                        super.render();
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        long now = System.nanoTime();
                        if (frames() > 260 && frames() <= 350 || frames() > 430) {
                            frameMillis.add((now - previousFrame) / 1_000_000.0);
                        }
                        previousFrame = now;
                        if (frames() == 90) {
                            topHash = capture("top.png");
                            // Exercise Scene2D input, not just the camera setter. The click must not reach the board.
                            click("iso");
                            assertTrue(boardCamera.isIsometric());
                            SwingUtilities.invokeAndWait(() -> { });
                            assertEquals(0, boardClicks.get());
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        } else if (frames() == 105) {
                            click("all-actions");
                            for (char character : "zzz".toCharArray()) {
                                Gdx.input.getInputProcessor().keyTyped(character);
                            }
                        } else if (frames() == 108) {
                            Stage stage = (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().first();
                            assertEquals(null, stage.getRoot().findActor("Hold position"));
                            for (int i = 0; i < 3; i++) {
                                Gdx.input.getInputProcessor().keyTyped('\b');
                            }
                        } else if (frames() == 110) {
                            // Invoke the actual Swing button through its Scene2D counterpart.
                            capture("context-actions.png");
                            Gdx.input.getInputProcessor().keyDown(Input.Keys.DOWN);
                            Gdx.input.getInputProcessor().keyUp(Input.Keys.DOWN);
                            Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
                            Gdx.input.getInputProcessor().keyUp(Input.Keys.ENTER);
                            SwingUtilities.invokeAndWait(() -> { });
                            assertEquals(1, fixture.clicks.get());
                            assertEquals(0, boardClicks.get());
                            Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                        } else if (frames() == 120) {
                            Vector<UnitLocation> path = new Vector<>();
                            path.add(new UnitLocation(1, new Coords(5, 5), 0, 0, 0));
                            path.add(new UnitLocation(1, new Coords(6, 5), 1, 0, 0));
                            path.add(new UnitLocation(1, new Coords(7, 5), 2, 0, 0));
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.entity.setPosition(new Coords(7, 5));
                                fixture.entity.setFacing(2);
                                fixture.entity.setSecondaryFacing(2);
                                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, fixture.entity, path));
                            });
                            SwingUtilities.invokeAndWait(() -> { });
                        } else if (frames() == 126) {
                            movingHash = capture("movement.png");
                            assertEquals(new Coords(7, 5), fixture.entity.getPosition());
                        } else if (frames() == 160) {
                            assertNotEquals(topHash, capture("isometric.png"));
                            assertNotEquals(movingHash, capture("movement-finished.png"));
                        } else if (frames() == 165) {
                            clickHex(new Coords(4, 4));
                        } else if (frames() == 175) {
                            assertEquals(new Coords(4, 4), fixture.source.takeFrame().context().coords());
                            assertEquals(0, boardClicks.get(), "Opening a context must not edit orders");
                            capture("target-context.png");
                            click("board.useHex");
                            SwingUtilities.invokeAndWait(() -> { });
                            assertEquals(2, boardClicks.get());
                            Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.view.markDeploymentHexesFor(fixture.entity);
                                fixture.view.addSprites(List.of(new MovementEnvelopeSprite(fixture.view, Color.MAGENTA,
                                      new Coords(4, 4), 63)));
                                fixture.source.refresh();
                            });
                        } else if (frames() == 205) {
                            capture("deployment-ranges.png");
                        } else if (frames() == 210) {
                            Vector3 direction = new Vector3(boardCamera.camera.direction);
                            Vector3 focus = new Vector3(boardCamera.focus);
                            cameraDrag(true, 120, 40);
                            assertFalse(direction.epsilonEquals(boardCamera.camera.direction, 0.001f));
                            assertTrue(focus.epsilonEquals(boardCamera.focus, 0.001f));
                            assertEquals(2, boardClicks.get(), "Orbit gestures must not issue game commands");
                        } else if (frames() == 216) {
                            capture("orbit.png");
                            orbitTarget = fixture.source.takeFrame().scene().tiles().stream()
                                  .max(java.util.Comparator.comparingInt(BoardScene.Tile::elevation)).orElseThrow().coords();
                            clickHex(orbitTarget);
                        } else if (frames() == 226) {
                            assertEquals(orbitTarget, fixture.source.takeFrame().context().coords());
                            assertEquals(2, boardClicks.get(), "Inspection after orbit must not issue orders");
                            capture("orbit-context.png");
                            Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                            click("camera");
                        } else if (frames() == 230) {
                            Vector3 direction = new Vector3(boardCamera.camera.direction);
                            GpuBoardTestUi.clickText(Messages.getString("GpuBoard.rotateLeft"));
                            GpuBoardTestUi.clickText(Messages.getString("GpuBoard.tiltUp"));
                            assertFalse(direction.epsilonEquals(boardCamera.camera.direction, 0.001f));
                            GpuBoardTestUi.clickText(Messages.getString("GpuBoard.resetCamera"));
                            assertTrue(boardCamera.isIsometric());
                        } else if (frames() == 234) {
                            capture("camera-menu.png");
                            Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                            Vector3 focus = new Vector3(boardCamera.focus);
                            cameraDrag(false, 80, 30);
                            assertFalse(focus.epsilonEquals(boardCamera.focus, 0.001f));
                            assertEquals(focus.z, boardCamera.focus.z, 0.001f);
                            // A drag returning to its starting point must not be mistaken for a context click.
                            Gdx.input.getInputProcessor().touchDown(500, 300, 0, Input.Buttons.RIGHT);
                            Gdx.input.getInputProcessor().touchDragged(550, 340, 0);
                            Gdx.input.getInputProcessor().touchDragged(500, 300, 0);
                            Gdx.input.getInputProcessor().touchUp(500, 300, 0, Input.Buttons.RIGHT);
                            SwingUtilities.invokeAndWait(() -> { });
                            assertNull(fixture.source.takeFrame().context());
                            focus.set(boardCamera.focus);
                            Gdx.input.getInputProcessor().touchDown(500, 300, 0, Input.Buttons.RIGHT);
                            pause();
                            Gdx.input.getInputProcessor().touchDragged(600, 350, 0);
                            Gdx.input.getInputProcessor().touchUp(600, 350, 0, Input.Buttons.RIGHT);
                            assertTrue(focus.epsilonEquals(boardCamera.focus, 0.001f), "Focus loss must cancel dragging");
                            assertEquals(2, boardClicks.get());
                            Input original = Gdx.input;
                            Input mouse = spy(original);
                            doReturn(500).when(mouse).getX();
                            doReturn(300).when(mouse).getY();
                            Gdx.input = mouse;
                            try {
                                float zoom = boardCamera.camera.zoom;
                                mouse.getInputProcessor().scrolled(0, -1);
                                assertTrue(boardCamera.camera.zoom < zoom, "Wheel input over the board must zoom");
                            } finally {
                                Gdx.input = original;
                            }
                            boardCamera.reset(fixture.source.takeFrame().scene());
                        } else if (frames() == 350) {
                            timing("frame-timing.txt", "16 x 17 board, one unit, deployment and range markings");
                            frameMillis.clear();
                            SwingUtilities.invokeAndWait(() -> largeBoard(fixture));
                        } else if (frames() == 410) {
                            capture("large-isometric.png");
                        } else if (frames() > 430 && frames() % 20 == 0) {
                            int x = (int) (frames() % 48);
                            SwingUtilities.invokeLater(() -> fixture.view.cursor(new Coords(x, 20)));
                        }
                        if (frames() == 550) {
                            timing("large-frame-timing.txt", "48 x 51 board, 36 units, ranges, attack line and cursor updates");
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                private void timing(String file, String sample) {
                    double average = frameMillis.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
                    Collections.sort(frameMillis);
                    double percentile95 = frameMillis.get((int) (frameMillis.size() * 0.95));
                    new FileHandle(new File(output, file)).writeString(
                          "Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER)
                                + "\nSample: " + sample + "; 1280 x 800, after warm-up"
                                + "\nAverage frame: " + average + " ms"
                                + "\nAverage rate: " + (1000 / average) + " FPS"
                                + "\n95th percentile frame: " + percentile95 + " ms\n", false);
                }

                private void clickHex(Coords coords) {
                    Vector3 point = screenPosition(coords);
                    int x = (int) point.x;
                    int y = (int) point.y;
                    Gdx.input.getInputProcessor().touchDown(x, y, 0, Input.Buttons.LEFT);
                    Gdx.input.getInputProcessor().touchUp(x, y, 0, Input.Buttons.LEFT);
                }

                private void cameraDrag(boolean shift, int dx, int dy) {
                    Input original = Gdx.input;
                    Input input = spy(original);
                    doReturn(shift).when(input).isKeyPressed(Input.Keys.SHIFT_LEFT);
                    Gdx.input = input;
                    try {
                        input.getInputProcessor().touchDown(500, 300, 0, Input.Buttons.RIGHT);
                        input.getInputProcessor().touchDragged(500 + dx, 300 + dy, 0);
                        input.getInputProcessor().touchUp(500 + dx, 300 + dy, 0, Input.Buttons.RIGHT);
                    } finally {
                        Gdx.input = original;
                    }
                }

                private long capture(String filename) {
                    return GpuBoardTestUi.capture(new File(output, filename));
                }

                private void click(String name) {
                    GpuBoardTestUi.click(name);
                }

            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) {
            throw new AssertionError("GPU smoke test failed", failure.get());
        }
    }

    private static void largeBoard(GpuBoardFixture fixture) {
        try {
            Board original = fixture.game.getBoard();
            Hex[] hexes = new Hex[48 * 51];
            for (int y = 0; y < 51; y++) {
                for (int x = 0; x < 48; x++) {
                    hexes[y * 48 + x] = original.getHex(x % 16, y % 17).duplicate();
                }
            }
            fixture.view.markDeploymentHexesFor(null);
            fixture.game.setBoard(new Board(48, 51, hexes));
            Player opponent = new Player(2, "GPU opponent");
            opponent.setTeam(2);
            fixture.game.addPlayer(opponent.getId(), opponent);
            for (int id = 2; id <= 36; id++) {
                Entity entity = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
                entity.setId(id);
                entity.setOwner(id % 2 == 0 ? opponent : fixture.player);
                entity.setDeployed(true);
                entity.setPosition(id == 2 ? new Coords(8, 5) : new Coords(id * 7 % 48, id * 13 % 51));
                fixture.game.addEntity(entity, false);
                fixture.view.addSprites(List.of(new MovementEnvelopeSprite(fixture.view, Color.CYAN, entity.getPosition(), 63)));
            }
            fixture.view.redrawAllEntities();
            fixture.view.addAttack(new WeaponAttackAction(1, 2,
                  fixture.entity.getEquipmentNum(fixture.entity.getWeaponList().getFirst())));
            fixture.source.refresh();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
