/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.loaders.MekFileParser;
import megamek.common.preference.PreferenceManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real GL layout/input and the EDT-to-render report snapshot boundary. */
@Tag("on-demand")
class GpuReportSmokeTest {
    @Test
    void reportReaderFiltersLiveHistoryAndFitsSmallWindows() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        File output = new File(System.getProperty("megamek.gpu.screenshots"));
        assertTrue(output.isDirectory() || output.mkdirs());
        var preferences = PreferenceManager.getClientPreferences();
        String oldKeywords = preferences.getReportKeywords();
        String oldFilters = preferences.getReportFilterKeywords();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            var atlas = new MekFileParser(new File("testresources/data/mekfiles/Atlas AS7-D.mtf")).getEntity();
            var locust = new MekFileParser(new File("testresources/data/mekfiles/Locust LCT-1V.mtf")).getEntity();
            SwingUtilities.invokeAndWait(() -> {
                preferences.setReportKeywords("LRM\nER Large Laser");
                preferences.setReportFilterKeywords("LRM Laser\nLRM");
                var shooter = fixture.entity;
                atlas.setId(42);
                locust.setId(43);
                for (var target : List.of(atlas, locust)) {
                    target.setOwner(fixture.player);
                    fixture.game.addEntity(target, false);
                }
                List<Report> first = List.of(new Report(2000), new Report(6065).addDesc(atlas).add(5).add("Right Arm"));
                List<Report> second = new ArrayList<>(List.of(new Report(3000), new Report(3100).addDesc(shooter)));
                for (int index = 0; index < 12; index++) {
                    second.addAll(GpuReportLogTest.attack(index % 2 == 0 ? atlas : locust,
                          index % 2 == 0 ? "ER Large Laser" : "LRM 20", index % 3 != 0));
                }
                fixture.game.setAllReports(new ArrayList<>(List.of(new ArrayList<>(first), second)));
                fixture.game.setRoundCount(2);
                fixture.game.setPhase(GamePhase.FIRING_REPORT);
                fixture.source.refresh();
            });
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;
                private float chosenWidth;

                @Override
                public void render() {
                    super.render();
                    try {
                        tick++;
                        if (tick == 1) {
                            boardCamera.setIsometric(true);
                        } else if (tick == 8) {
                            assertTrue(panel().isVisible(), "Report phases open the native reader");
                            assertStatus("12 events");
                            assertKeywordShortcuts();
                            assertBounds();
                            assertEquals(GpuPanelDock.WIDTH, panel().getWidth(), 1,
                                  "The report starts at the same width as the fire panel");
                            Table readout = panel().findActor("report-readout:42");
                            assertTrue(readout.getChildren().first() instanceof Image icon && icon.getDrawable() != null,
                                  "Use the client's real unit artwork for report thumbnails");
                            GpuBoardTestUi.capture(new File(output, "battle-report.png"));
                            clickInformation();
                            assertTrue(((Label) panel().findActor("report-rolls:0")).getText().toString()
                                  .contains("target movement"), "The inline linked value opens its original explanation");
                            clickInformation();
                            assertTrue(panel().findActor("report-rolls:0") == null);
                            dragWidth(630);
                            assertEquals(630, panel().getWidth(), 1);
                            assertBounds();
                            clickInformation();
                            assertTrue(panel().findActor("report-rolls:0") != null,
                                  "Links still work after the panel reflows at a wider width");
                            GpuBoardTestUi.click("report-unit:42");
                        } else if (tick == 12) {
                            assertStatus("6 events");
                            SelectBox<?> unit = GpuBoardTestUi.stage().getRoot().findActor("report-unit");
                            assertEquals(42, ((GpuReportLog.Unit) unit.getSelected()).id());
                            assertTrue(panel().findActor("report-rolls:0") != null);
                            assertTrue(panel().findActor("report-rolls:1") == null);
                            GpuBoardTestUi.capture(new File(output, "battle-report-unit.png"));
                            dragWidth(5000);
                            assertEquals(GpuBoardTestUi.stage().getWidth() - 24, panel().getWidth(), 1,
                                  "Dragging beyond the window clamps the report to the viewport");
                            dragWidth(0);
                            assertEquals(GpuPanelDock.WIDTH, panel().getWidth(), 1,
                                  "The minimum width keeps the filters and event text usable");
                            dragWidth(480);
                            chosenWidth = panel().getWidth();
                            assertStatus("6 events");
                            search().setText("Right Arm");
                        } else if (tick == 16) {
                            assertStatus("0 events");
                            GpuBoardTestUi.click("report-reset");
                            assertStatus("13 events");
                            SelectBox<?> turn = GpuBoardTestUi.stage().getRoot().findActor("report-turn");
                            turn.setSelectedIndex(3); // Explicit turn 1, after Latest, All and turn 2.
                            turn.fire(new ChangeEvent());
                            assertStatus("1 events");
                            assertTrue(((Label) panel().findActor("report-text:0")).getText().toString().contains("Right Arm"));
                            GpuBoardTestUi.click("report-latest");
                            Gdx.graphics.setWindowedMode(900, 600);
                        } else if (tick == 28) {
                            assertStatus("12 events");
                            assertBounds();
                            assertEquals(chosenWidth, panel().getWidth(), 1, "Window resizing preserves the chosen width");
                            ScrollPane scroll = panel().findActor("report-scroll");
                            assertTrue(scroll.getHeight() >= 120, "Small windows retain a usable event viewport");
                            GpuBoardTestUi.capture(new File(output, "battle-report-small.png"));
                            GpuBoardTestUi.click("report-details");
                        } else if (tick == 32) {
                            assertTrue(panel().findActor("report-rolls:0") != null);
                            GpuBoardTestUi.capture(new File(output, "battle-report-details.png"));
                            GpuBoardTestUi.click("report-close");
                            assertFalse(panel().isVisible());
                            GpuBoardTestUi.click("battle-report-toggle");
                        } else if (tick == 36) {
                            assertTrue(panel().isVisible());
                            assertEquals(chosenWidth, panel().getWidth(), 1, "Reopening preserves the chosen width");
                            GpuBoardTestUi.click("report-search");
                            Gdx.input.getInputProcessor().keyTyped('L');
                            Gdx.input.getInputProcessor().keyTyped('R');
                            Gdx.input.getInputProcessor().keyTyped('M');
                            assertStatus("6 events");
                            GpuBoardTestUi.click("report-close");
                            Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.game.setPhase(GamePhase.PHYSICAL_REPORT);
                                fixture.source.refresh();
                            });
                        } else if (tick == 44) {
                            assertTrue(panel().isVisible(), "A new report phase can reopen a dismissed reader");
                            assertEquals("LRM", search().getText(), "Review filters survive incoming phase changes");
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.game.setPhase(GamePhase.MOVEMENT);
                                fixture.source.refresh();
                            });
                        } else if (tick == 48) {
                            assertFalse(panel().isVisible(), "An automatic report closes when play resumes");
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                private Table panel() { return GpuBoardTestUi.stage().getRoot().findActor("battle-report"); }

                private TextField search() { return panel().findActor("report-search"); }

                private void assertKeywordShortcuts() {
                    GpuBoardTestUi.stage().setKeyboardFocus(null);
                    GpuBoardTestUi.press(KeyCommandBind.REPORT_KEY_NEXT);
                    assertStatus("12 events  /  Match 1 of 6");
                    GpuBoardTestUi.press(KeyCommandBind.REPORT_KEY_NEXT);
                    assertStatus("12 events  /  Match 2 of 6");
                    GpuBoardTestUi.press(KeyCommandBind.REPORT_KEY_PREV);
                    assertStatus("12 events  /  Match 1 of 6");
                    GpuBoardTestUi.stage().setKeyboardFocus(search());
                    GpuBoardTestUi.press(KeyCommandBind.REPORT_KEY_SELECT_NEXT);
                    SelectBox<?> keyword = panel().findActor("report-keyword");
                    assertEquals("ER Large Laser", keyword.getSelected());
                    GpuBoardTestUi.press(KeyCommandBind.REPORT_KEY_SELECT_PREVIOUS);
                    assertEquals("LRM", keyword.getSelected());
                    assertEquals("", search().getText());
                    GpuBoardTestUi.stage().setKeyboardFocus(null);
                    GpuBoardTestUi.press(KeyCommandBind.REPORT_KEY_FILTER);
                    assertStatus("12 events");
                    GpuBoardTestUi.press(KeyCommandBind.REPORT_FILTER_KEY_SELECT_NEXT);
                    assertStatus("6 events");
                    GpuBoardTestUi.press(KeyCommandBind.REPORT_KEY_FILTER);
                    assertStatus("12 events");
                    int oldKey = KeyCommandBind.REPORT_KEY_NEXT.key;
                    try {
                        KeyCommandBind.REPORT_KEY_NEXT.key = java.awt.event.KeyEvent.VK_F8;
                        GpuBoardTestUi.press(KeyCommandBind.REPORT_KEY_NEXT);
                        assertStatus("12 events  /  Match 1 of 6");
                    } finally {
                        KeyCommandBind.REPORT_KEY_NEXT.key = oldKey;
                    }
                    GpuBoardTestUi.click("report-latest");
                }

                private void clickInformation() {
                    GpuReportText body = panel().findActor("report-text:0");
                    for (int y = 0; y < body.getHeight(); y++) {
                        for (int x = 0; x < body.getWidth(); x++) {
                            var link = body.linkAt(x, y);
                            if (link != null && link.detail().contains("gunnery")) {
                                ScrollPane scroll = panel().findActor("report-scroll");
                                Vector2 inRows = body.localToAscendantCoordinates(scroll.getWidget(), new Vector2(x, y));
                                scroll.scrollTo(inRows.x, inRows.y, 1, 1);
                                scroll.updateVisualScroll();
                                GpuBoardTestUi.stage().draw();
                                Vector2 point = body.localToStageCoordinates(new Vector2(x, y));
                                GpuBoardTestUi.stage().stageToScreenCoordinates(point);
                                var input = Gdx.input.getInputProcessor();
                                input.mouseMoved(Math.round(point.x), Math.round(point.y));
                                input.touchDown(Math.round(point.x), Math.round(point.y), 0, Input.Buttons.LEFT);
                                input.touchUp(Math.round(point.x), Math.round(point.y), 0, Input.Buttons.LEFT);
                                return;
                            }
                        }
                    }
                    throw new AssertionError("The to-hit value must retain a clickable area after word wrapping");
                }

                private void dragWidth(float width) {
                    Actor edge = panel().findActor("dock-resize");
                    Vector2 from = edge.localToStageCoordinates(new Vector2(edge.getWidth() / 2, edge.getHeight() / 2));
                    Vector2 to = from.cpy().add(panel().getWidth() - width, 0);
                    GpuBoardTestUi.stage().stageToScreenCoordinates(from);
                    GpuBoardTestUi.stage().stageToScreenCoordinates(to);
                    Gdx.input.getInputProcessor().touchDown(Math.round(from.x), Math.round(from.y), 0, Input.Buttons.LEFT);
                    Gdx.input.getInputProcessor().touchDragged(Math.round(to.x), Math.round(to.y), 0);
                    Gdx.input.getInputProcessor().touchUp(Math.round(to.x), Math.round(to.y), 0, Input.Buttons.LEFT);
                }

                private void assertStatus(String text) {
                    Label status = panel().findActor("report-status");
                    assertTrue(status.getText().toString().startsWith(text), status.getText().toString());
                }

                private void assertBounds() {
                    assertTrue(panel().getTop() <= GpuBoardTestUi.stage().getHeight() - GpuBoardUi.TOP_HEIGHT);
                    assertTrue(panel().getY() >= GpuBoardUi.TURN_HEIGHT);
                    assertTrue(panel().getX() >= 0);
                    assertEquals(GpuBoardTestUi.stage().getWidth() - 12, panel().getRight(), 1,
                          "The right edge stays anchored while resizing");
                }
            }, GpuBoardWindow.configuration(false));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setReportKeywords(oldKeywords);
                preferences.setReportFilterKeywords(oldFilters);
            });
        }
        if (failure.get() != null) {
            throw new AssertionError("Native report reader failed", failure.get());
        }
    }
}
