/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real Scene2D input and layout over the rendered board; command snapshots are deterministic UI fixtures. */
@Tag("on-demand")
class GpuBoardUiSmokeTest {
    @Test
    void tacticalMenuHandlesLiveCommandsKeyboardSearchAndSmallWindows() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger chosen = new AtomicInteger();
        AtomicInteger committed = new AtomicInteger();
        AtomicInteger leaked = new AtomicInteger();
        AtomicInteger playbackToggles = new AtomicInteger();
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private GpuBoardUi controls;
                private GpuBoardSource.Frame snapshot;
                private int tick;

                @Override
                public void create() {
                    super.create();
                    controls = new GpuBoardUi(fixture.source, boardCamera, () -> { }, playbackToggles::incrementAndGet);
                    snapshot = presentation(fixture.source.takeFrame(), chosen, committed, false);
                    controls.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                          new GpuDisplayScale().read(fixture.source.uiPreferences.scale()));
                    controls.update(snapshot, "Speed: 1x");
                    Gdx.input.setInputProcessor(new InputMultiplexer(controls.stage, new InputAdapter() {
                        @Override
                        public boolean touchDown(int x, int y, int pointer, int button) {
                            leaked.incrementAndGet();
                            return true;
                        }
                    }));
                }

                @Override
                public void render() {
                    super.render();
                    try {
                        controls.update(snapshot, "Speed: 1x");
                        controls.draw();
                        tick++;
                        if (tick == 5) {
                            GpuBoardTestUi.click("playback");
                            assertEquals(1, playbackToggles.get());
                            controls.setPlaybackPaused(true);
                            TextButton playbackButton = controls.stage.getRoot().findActor("playback");
                            assertEquals(megamek.client.ui.Messages.getString("GpuBoard.resumePlayback"), playbackButton.getText().toString());
                            GpuBoardTestUi.click("playback");
                            assertEquals(2, playbackToggles.get());
                            controls.setPlaybackPaused(false);
                            controls.inspect(new Coords(5, 5), 750, 200);
                        } else if (tick == 25) {
                            assertBounds();
                            assertFalse(controls.stage.getRoot().findActor("command-search").getParent().isVisible(),
                                  "Context menus do not include command search");
                            GpuBoardTestUi.capture(new File(output, "tactical-context.png"));
                            controls.key(Input.Keys.F10, true);
                            GpuBoardTestUi.click("weapons");
                        } else if (tick == 30) {
                            press(Input.Keys.UP);
                            assertEquals("weapon:11", controls.stage.getKeyboardFocus().getName(), "Up starts at the final enabled row");
                            assertTrue(((ScrollPane) controls.stage.getRoot().findActor("command-scroll")).getScrollY() > 0);
                            TextButton wrapped = controls.stage.getRoot().findActor("weapon:11");
                            assertTrue(wrapped.getLabel().getHeight() > wrapped.getStyle().font.getLineHeight(),
                                  "The fixture must exercise a wrapped command name");
                            assertInset(wrapped.getLabel(), wrapped);
                            assertInset(wrapped.getChildren().first(), wrapped);
                            // Search is available after keyboard navigation by clicking the input field.
                            GpuBoardTestUi.click("command-search");
                            for (char character : "disabled".toCharArray()) {
                                controls.stage.keyTyped(character);
                            }
                        } else if (tick == 35) {
                            assertFalse(controls.stage.getRoot().findActor("command-search-hint").isVisible(),
                                  "The empty-field hint must not overlap typed text");
                            press(Input.Keys.DOWN);
                            press(Input.Keys.ENTER);
                            assertEquals(0, chosen.get(), "Disabled search results never execute");
                            TextField search = controls.stage.getRoot().findActor("command-search");
                            search.setText("medium laser 7");
                        } else if (tick == 40) {
                            press(Input.Keys.DOWN);
                            press(Input.Keys.ENTER);
                            assertEquals(1, chosen.get());
                            assertEquals(0, committed.get(), "Selecting a weapon must not finish the phase");
                            assertFalse(controls.acceptsCameraKeys(), "Weapon choices stay open");
                            // A new snapshot revokes availability while the row is focused.
                            snapshot = presentation(snapshot, chosen, committed, true);
                        } else if (tick == 45) {
                            press(Input.Keys.ENTER);
                            assertEquals(1, chosen.get(), "A revoked command cannot execute from keyboard focus");
                            ((TextField) controls.stage.getRoot().findActor("command-search")).setText("");
                            GpuBoardTestUi.click("menu-back");
                        } else if (tick == 50) {
                            assertBounds();
                            assertTrue(controls.stage.getRoot().findActor("command-search-hint").isVisible(),
                                  "The palette hint returns when navigation clears the search");
                            assertEquals(0, leaked.get(), "Menu clicks must not reach the board");
                            // The same layout is clamped at the smallest supported logical viewport.
                            Gdx.graphics.setWindowedMode(900, 600);
                        } else if (tick == 60) {
                            controls.resize(900, 600, GpuDisplayScale.calculate(900, 600, 1, 1, 1));
                            controls.inspect(new Coords(5, 5), 895, 595);
                        } else if (tick == 80) {
                            assertBounds();
                            Label actor = controls.stage.getRoot().findActor("acting-unit");
                            assertTrue(actor.getWidth() > 0, "The unit card retains space beside completion controls");
                            GpuBoardTestUi.capture(new File(output, "tactical-context-small.png"));
                            GpuBoardTestUi.click("board.useHex");
                            assertTrue(controls.plotting());
                            assertTrue(controls.acceptsCameraKeys());
                            assertEquals(2, chosen.get());
                            assertEquals(0, committed.get());
                            controls.key(Input.Keys.ESCAPE, true);
                            assertFalse(controls.plotting());
                            controls.key(Input.Keys.F10, true);
                        } else if (tick == 90) {
                            assertBounds();
                            Actor trigger = controls.stage.getRoot().findActor("all-actions");
                            Vector2 top = trigger.localToStageCoordinates(new Vector2(0, trigger.getHeight()));
                            Table menu = controls.stage.getRoot().findActor("tactical-menu");
                            assertEquals(top.y + 2, menu.getY(), 0.01f, "The palette opens just above its bottom-bar trigger");
                            assertTrue(controls.stage.getRoot().findActor("command-search").getParent().isVisible(),
                                  "Search remains available in All Actions");
                            GpuBoardTestUi.capture(new File(output, "tactical-actions-small.png"));
                            press(Input.Keys.ESCAPE);
                            assertTrue(controls.acceptsCameraKeys());
                            GpuBoardTestUi.click("camera");
                        } else if (tick == 100) {
                            assertBounds();
                            Actor trigger = controls.stage.getRoot().findActor("camera");
                            Vector2 bottom = trigger.localToStageCoordinates(new Vector2());
                            Table menu = controls.stage.getRoot().findActor("tactical-menu");
                            assertEquals(bottom.x, menu.getX(), 0.01f);
                            assertEquals(bottom.y - 2, menu.getTop(), 0.01f, "The dropdown opens just below its trigger");
                            assertFalse(controls.stage.getRoot().findActor("command-search").getParent().isVisible());
                            assertEquals(menu, controls.stage.getKeyboardFocus(), "A hidden search field cannot capture typing");
                            GpuBoardTestUi.capture(new File(output, "camera-menu.png"));
                            GpuBoardTestUi.click("playback");
                            assertFalse(menu.isVisible(), "Toolbar clicks dismiss the popup");
                            assertEquals(3, playbackToggles.get(), "The same click must perform the toolbar action");
                            GpuBoardTestUi.click("camera");
                            GpuBoardTestUi.click("all-actions");
                            assertTrue(menu.isVisible(), "An action that opens a menu replaces the dismissed popup");
                            assertTrue(controls.stage.getRoot().findActor("command-search").getParent().isVisible());
                            GpuBoardTestUi.click("top");
                            assertFalse(menu.isVisible());
                            assertFalse(boardCamera.isIsometric());
                            assertEquals(0, leaked.get(), "Outside UI clicks execute without leaking through to the board");
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                private void press(int key) {
                    controls.stage.keyDown(key);
                    controls.stage.keyUp(key);
                }

                private void assertBounds() {
                    Label fps = controls.stage.getRoot().findActor("fps");
                    assertEquals(Gdx.graphics.getFramesPerSecond() + " FPS", fps.getText().toString());
                    Vector2 corner = fps.localToStageCoordinates(new Vector2());
                    assertTrue(corner.x > controls.stage.getWidth() - 110 && corner.y < GpuBoardUi.TURN_HEIGHT,
                          "FPS must remain visible in the bottom-right corner, including small windows");
                    Table menu = controls.stage.getRoot().findActor("tactical-menu");
                    assertTrue(menu.getX() >= 0 && menu.getRight() <= controls.stage.getWidth());
                    assertTrue(menu.getY() >= 0);
                    assertTrue(menu.getTop() <= controls.stage.getHeight());
                    Actor close = controls.stage.getRoot().findActor("close-menu");
                    assertTrue(close.getWidth() >= 24 && close.getHeight() >= 24);
                    TextButton move = controls.stage.getRoot().findActor("board.useHex");
                    if (move != null) {
                        assertInset(move.getLabel(), move);
                        assertInset(move.getChildren().first(), move);
                    }
                }

                private void assertInset(Actor content, TextButton button) {
                    Vector2 bottom = content.localToAscendantCoordinates(button, new Vector2());
                    Vector2 top = content.localToAscendantCoordinates(button, new Vector2(content.getWidth(), content.getHeight()));
                    assertTrue(bottom.x >= button.getPadLeft() && top.x <= button.getWidth() - button.getPadRight(),
                          "Command content must fit horizontally inside the menu row");
                    assertTrue(bottom.y >= button.getPadBottom() && top.y <= button.getHeight() - button.getPadTop(),
                          "Command content must fit vertically inside the menu row");
                }

                @Override
                public void dispose() {
                    if (controls != null) {
                        controls.dispose();
                    }
                    super.dispose();
                }
            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) {
            throw new AssertionError("Tactical UI smoke test failed", failure.get());
        }
    }

    private static GpuBoardSource.Frame presentation(GpuBoardSource.Frame original, AtomicInteger chosen,
          AtomicInteger committed, boolean revoked) {
        List<BoardScene.Command> weapons = new ArrayList<>();
        weapons.add(new BoardScene.Command("disabled", "Disabled weapon", "No ammunition available.", false, false,
              List.of(), chosen::incrementAndGet));
        for (int i = 0; i < 12; i++) {
            String label = "Medium laser " + i + (i == 11 ? " - fire at the selected unit in this hex" : "");
            weapons.add(new BoardScene.Command("weapon:" + i, label,
                  "Weapon selection uses the current firing controls.", !(revoked && i == 7), false,
                  List.of(), chosen::incrementAndGet));
        }
        List<BoardScene.Command> choices = List.of(
              new BoardScene.Command("board.useHex", "Plot movement here", "Use the current movement tool at this location.",
                    true, false, true, List.of(), chosen::incrementAndGet),
              new BoardScene.Command("weapons", "Weapons and ammunition", "Select a weapon or ammunition type.", true, false,
                    weapons, () -> { }),
              new BoardScene.Command("board.los", "Measure line of sight", "Choose the start and end hexes.", true, false,
                    List.of(), chosen::incrementAndGet),
              new BoardScene.Command("unavailable", "Jump to this location", "Unavailable for the current unit or phase.",
                    false, false, List.of(), chosen::incrementAndGet));
        List<BoardScene.Command> commands = new ArrayList<>(choices);
        commands.add(new BoardScene.Command("done", "Done", "Finish the current phase.", true, true,
              List.of(), committed::incrementAndGet));
        commands.add(new BoardScene.Command("skip", "Skip", "Skip this unit.", false, true,
              List.of(), committed::incrementAndGet));
        BoardScene scene = original.scene();
        BoardScene next = new BoardScene(scene.boardId(), scene.width(), scene.height(), scene.tiles(), scene.units(),
              scene.plannedPath(), 1, scene.phase(), commands, scene.light());
        return new GpuBoardSource.Frame(next, List.of(), new BoardScene.Context(new Coords(5, 5), choices),
              List.of(), original.hud(), "Atlas AS7-D\nHex 0606\nField intelligence comes from the existing board tooltip.",
              null, original.boardGeneration(), "Atlas AS7-D");
    }
}
