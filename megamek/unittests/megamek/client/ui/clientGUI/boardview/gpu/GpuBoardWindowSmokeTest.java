/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.UnitOverviewOverlay;
import megamek.client.ui.dialogs.miniReport.MiniReportDisplayDialog;
import megamek.client.ui.dialogs.miniReport.MiniReportDisplayPanel;
import megamek.client.ui.entityreadout.LiveReadoutDialog;
import megamek.common.Report;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

/** Exercises the real Swing/native window handoff and the shared menu through Scene2D input. */
@Tag("on-demand")
class GpuBoardWindowSmokeTest {
    private record ClientWindow(JFrame frame, CommonMenuBar menus, BoardView view, JMenuItem gpuChoice,
          UnitOverviewOverlay overview) { }

    @Test
    void classicReportStaysHiddenUntilReturningFromTheNativeBoard() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        boolean enabled = preferences.getMiniReportEnabled();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            ClientWindow ui = onSwing(() -> createClientWindow(fixture));
            ClientGUI gui = ui.view().getClientgui();
            MiniReportDisplayDialog report = onSwing(() -> {
                MiniReportDisplayDialog dialog = new MiniReportDisplayDialog(ui.frame(), gui);
                when(gui.getMiniReportDisplayDialog()).thenReturn(dialog);
                when(gui.getMiniReportDisplay()).thenReturn(mock(MiniReportDisplayPanel.class));
                doAnswer(invocation -> {
                    if (GpuBoardWindow.isActiveFor(gui)) {
                        invocation.callRealMethod();
                    } else {
                        // The fixture has no classic split panes. Observe the restored visibility request instead.
                        dialog.setVisible(invocation.getArgument(0));
                    }
                    return null;
                }).when(gui).setMiniReportLocation(anyBoolean());
                preferences.setMiniReportEnabled(true);
                dialog.setVisible(true);
                return dialog;
            });
            try {
                onSwing(() -> { ui.gpuChoice().doClick(0); return null; });
                await(() -> onSwing(() -> !ui.frame().isVisible()));
                onSwing(() -> {
                    assertTrue(GpuBoardWindow.isActiveFor(gui));
                    assertFalse(report.isVisible(), "Hide an already open classic report during the handoff");
                    fixture.game.setPhase(GamePhase.FIRING_REPORT);
                    fixture.game.setAllReports(List.of(List.of(new Report(3000),
                          new Report(6065).addDesc(fixture.entity).add(10).add("Left Torso"))));
                    gui.setMiniReportLocation(true);
                    assertFalse(report.isVisible(), "Phase and preference updates must not reopen the legacy dialog");
                    assertTrue(preferences.getMiniReportEnabled(), "Suppressing the old window preserves 2D preferences");
                    return null;
                });
                await(() -> onGl(() -> GpuBoardTestUi.stage().getRoot().findActor("report-readout:1") != null));
                input(() -> GpuBoardTestUi.click("report-readout:1"));
                await(() -> onSwing(() -> java.util.Arrays.stream(ui.frame().getOwnedWindows())
                      .anyMatch(window -> window instanceof LiveReadoutDialog && window.isVisible() && window.isAlwaysOnTop())));
                onSwing(() -> {
                    assertFalse(report.isVisible(), "Opening a unit's details must not revive the old report");
                    for (var window : ui.frame().getOwnedWindows()) {
                        if (window instanceof LiveReadoutDialog) {
                            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                        }
                    }
                    GpuBoardWindow.showClassic(gui);
                    return null;
                });
                await(() -> onSwing(() -> ui.frame().isVisible() && report.isVisible()));
                assertFalse(onSwing(() -> GpuBoardWindow.isActiveFor(gui)));
            } finally {
                onSwing(() -> {
                    GpuBoardWindow.closeFor(ui.view());
                    report.dispose();
                    ui.frame().dispose();
                    ui.view().dispose();
                    preferences.removePreferenceChangeListener(ui.menus());
                    preferences.setMiniReportEnabled(enabled);
                    return null;
                });
            }
        }
    }

    @Test
    void switchesExclusiveWindowsThroughMenusAndRestoresClassicOnNativeClose() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            ClientWindow ui = onSwing(() -> createClientWindow(fixture));
            Coords position = fixture.entity.getPosition();
            AtomicInteger overlayClicks = new AtomicInteger();
            AtomicInteger unitClicks = new AtomicInteger();
            GUIPreferences preferences = GUIPreferences.getInstance();
            float originalScale = preferences.getGUIScale();
            boolean originalOverview = preferences.getShowUnitOverview();
            try {
                onSwing(() -> {
                    preferences.setShowUnitOverview(true);
                    ui.view().addOverlay(ui.overview());
                    ui.view().addBoardViewListener(new BoardViewListenerAdapter() {
                        @Override
                        public void unitSelected(BoardViewEvent event) {
                            assertEquals(fixture.entity.getId(), event.getEntityId());
                            unitClicks.incrementAndGet();
                        }
                    });
                    ui.view().addOverlay(new IDisplayable() {
                        private Rectangle bounds() {
                            float scale = preferences.getGUIScale();
                            return new Rectangle(Math.round(20 * scale), Math.round(30 * scale),
                                  Math.round(80 * scale), Math.round(40 * scale));
                        }

                        @Override
                        public void draw(Graphics graphics, Rectangle rect) {
                            Rectangle bounds = bounds();
                            graphics.setColor(Color.CYAN);
                            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                        }

                        @Override
                        public boolean isHit(Point point, Dimension size) {
                            if (bounds().contains(point)) {
                                overlayClicks.incrementAndGet();
                                return true;
                            }
                            return false;
                        }
                    });
                    ui.view().centerOnHex(position);
                    ui.gpuChoice().doClick(0);
                    assertTrue(ui.frame().isVisible(), "Keep the original UI until the first GPU frame is ready");
                    return null;
                });
                await(() -> onSwing(() -> !ui.frame().isVisible()));
                awaitMaximizedWindow();
                await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames() >= 5));
                onGl(() -> {
                    assertTrue(unitIsCentered(fixture), "A focus request before opening the GPU view must survive its initial fit");
                    return null;
                });
                assertTrue(onSwing(() -> ui.frame().isDisplayable()), "Switching must preserve the original client");
                // The classic window is hidden, so a client dialog must be raised above the native window.
                JDialog probe = onSwing(() -> {
                    JDialog dialog = new JDialog(ui.frame(), "GPU dialog probe", false);
                    dialog.setSize(160, 90);
                    dialog.setVisible(true);
                    return dialog;
                });
                await(() -> onSwing(probe::isAlwaysOnTop));
                onSwing(() -> { probe.dispose(); return null; });
                input(() -> ((Lwjgl3Graphics) Gdx.graphics).getWindow().restoreWindow());
                for (int[] size : new int[][] { { 900, 600 }, { 2043, 1200 }, { 2560, 1600 }, { 3840, 2160 }, { 1280, 800 } }) {
                    onSwing(() -> { preferences.setValue(GUIPreferences.GUI_SCALE, size[0] == 2560 ? 1.5f : 1f); return null; });
                    input(() -> assertTrue(Gdx.graphics.setWindowedMode(size[0], size[1])));
                    long resizedFrame = onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames());
                    // Allow the EDT to publish the resized overlay and upload it on the render thread.
                    await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames() > resizedFrame + 15));
                    assertFalse(onSwing(() -> ui.frame().isVisible()), "Resizing must not fall back to the classic board");
                    int previousClicks = overlayClicks.get();
                    input(() -> {
                        float scale = Gdx.graphics.getWidth() / GpuBoardTestUi.stage().getWidth();
                        int x = Math.round(40 * scale);
                        int y = Math.round((GpuBoardUi.TOP_HEIGHT + 40) * scale);
                        Gdx.input.getInputProcessor().touchDown(x, y, 0, Input.Buttons.LEFT);
                        Gdx.input.getInputProcessor().touchUp(x, y, 0, Input.Buttons.LEFT);
                    });
                    onSwing(() -> {
                        assertEquals(previousClicks + 1, overlayClicks.get(), "Scaled HUD input must match the painted widget");
                        return null;
                    });
                    // Exercise the actual sidebar, independently of any phase selection handler.
                    float zoom = onGl(() -> {
                        GpuBattleView battle = (GpuBattleView) Gdx.app.getApplicationListener();
                        battle.boardCamera.setIsometric(size[0] != 900);
                        battle.boardCamera.pan(140, -100);
                        assertFalse(unitIsCentered(fixture));
                        return battle.boardCamera.camera.zoom;
                    });
                    Vector3 direction = onGl(() -> new Vector3(((GpuBattleView) Gdx.app.getApplicationListener()).boardCamera.camera.direction));
                    int previousUnitClicks = unitClicks.get();
                    input(() -> {
                        float scale = Gdx.graphics.getWidth() / GpuBoardTestUi.stage().getWidth();
                        float overlayScale = scale / (size[0] == 2560 ? 1.5f : 1f);
                        int x = Math.round(Gdx.graphics.getWidth() - 33 * overlayScale);
                        int y = Math.round(GpuBoardUi.TOP_HEIGHT * scale + 29 * overlayScale);
                        Gdx.input.getInputProcessor().touchDown(x, y, 0, Input.Buttons.LEFT);
                        Gdx.input.getInputProcessor().touchUp(x, y, 0, Input.Buttons.LEFT);
                    });
                    await(() -> onGl(() -> unitIsCentered(fixture)));
                    assertEquals(previousUnitClicks + 1, unitClicks.get(), "Sidebar centering must retain the existing selection event");
                    onGl(() -> {
                        GpuBattleView battle = (GpuBattleView) Gdx.app.getApplicationListener();
                        assertEquals(zoom, battle.boardCamera.camera.zoom, 0.001f, "Centering preserves zoom");
                        assertTrue(direction.epsilonEquals(battle.boardCamera.camera.direction, 0.001f), "Centering preserves the view angle");
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                        assertTrue(output.isDirectory() || output.mkdirs());
                        GpuBoardTestUi.capture(new File(output, "resize-" + size[0] + ".png"));
                        return null;
                    });
                }
                input(() -> GpuBoardTestUi.clickText(Messages.getString("CommonMenuBar.FileMenu")));
                captureMenu("file-menu.png");
                input(() -> Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE));
                input(() -> GpuBoardTestUi.clickText(Messages.getString("CommonMenuBar.ViewMenu")));
                captureMenu("menu-bar.png");
                onGl(() -> {
                    GpuBattleView battle = (GpuBattleView) Gdx.app.getApplicationListener();
                    float before = battle.boardCamera.camera.zoom;
                    GpuBoardTestUi.clickText(Messages.getString("CommonMenuBar.viewZoomIn"));
                    assertNotEquals(before, battle.boardCamera.camera.zoom, "The menu must zoom the active GPU camera");
                    Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                    return null;
                });
                input(() -> GpuBoardTestUi.clickText(Messages.getString("CommonMenuBar.ViewMenu")));
                onGl(() -> {
                    GpuBoardTestUi.clickText(Messages.getString("CommonMenuBar.viewClassicBoard"));
                    return null;
                });
                await(() -> onSwing(() -> ui.frame().isVisible()));
                assertSame(ui.menus(), onSwing(() -> ui.frame().getJMenuBar()));
                assertEquals(position, fixture.entity.getPosition());

                // Reopen through the same classic menu, then exercise the native window's close operation.
                onSwing(() -> { ui.gpuChoice().doClick(0); return null; });
                await(() -> onSwing(() -> !ui.frame().isVisible()));
                awaitMaximizedWindow();
                onGl(() -> { ((Lwjgl3Graphics) Gdx.graphics).getWindow().closeWindow(); return null; });
                await(() -> onSwing(() -> ui.frame().isVisible()));

                // Client disposal while in 3D must never bring the old frame back.
                onSwing(() -> { ui.gpuChoice().doClick(0); return null; });
                await(() -> onSwing(() -> !ui.frame().isVisible()));
                onSwing(() -> {
                    GpuBoardWindow.closeFor(ui.view());
                    ui.frame().dispose();
                    return null;
                });
                await(() -> Thread.getAllStackTraces().keySet().stream()
                      .noneMatch(thread -> thread.getName().equals("MegaMek-GPU-board")));
                onSwing(() -> {
                    assertFalse(ui.frame().isDisplayable());
                    assertFalse(ui.frame().isVisible());
                    return null;
                });
            } finally {
                onSwing(() -> {
                    preferences.setValue(GUIPreferences.GUI_SCALE, originalScale);
                    preferences.setShowUnitOverview(originalOverview);
                    preferences.removePreferenceChangeListener(ui.overview());
                    GpuBoardWindow.closeFor(ui.view());
                    ui.frame().dispose();
                    ui.menus().die();
                    return null;
                });
                await(() -> Thread.getAllStackTraces().keySet().stream()
                      .noneMatch(thread -> thread.getName().equals("MegaMek-GPU-board")));
            }
        }
    }

    private static void awaitMaximizedWindow() throws Exception {
        await(() -> onGl(() -> {
            long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
            return GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE
                  && GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
        }));
        onGl(() -> {
            long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
            assertEquals(GLFW.GLFW_TRUE, GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_DECORATED),
                  "The maximized board must retain its title bar and window controls");
            assertFalse(Gdx.graphics.isFullscreen(), "The board must remain a normal desktop window");
            return null;
        });
    }

    private static boolean unitIsCentered(GpuBoardFixture fixture) {
        Coords position = fixture.entity.getPosition();
        Vector3 expected = BoardGeometry.center(position, fixture.game.getBoard().getHex(position).getLevel());
        return ((GpuBattleView) Gdx.app.getApplicationListener()).boardCamera.focus.epsilonEquals(expected, 0.01f);
    }

    private ClientWindow createClientWindow(GpuBoardFixture fixture) {
        fixture.source.close();
        ClientGUI gui = mock(ClientGUI.class);
        Client client = mock(Client.class);
        JFrame frame = new JFrame("MegaMek - Classic board switch test");
        CommonMenuBar menus = CommonMenuBar.getMenuBarForGame();
        menus.setPhase(GamePhase.MOVEMENT);
        frame.add(fixture.view.getComponent());
        BoardView view = spy(fixture.view);
        doReturn(gui).when(view).getClientgui();
        when(gui.getClient()).thenReturn(client);
        when(client.getGame()).thenReturn(fixture.game);
        when(client.getLocalPlayer()).thenReturn(fixture.player);
        when(gui.getFrame()).thenReturn(frame);
        when(gui.getMenuBar()).thenReturn(menus);
        when(gui.getCurrentBoardView()).thenReturn(Optional.of(view));
        when(gui.boardViews()).thenReturn(List.of(view));
        when(gui.getBoardView()).thenReturn(view);
        when(gui.getBoardView(any(BoardLocation.class))).thenReturn(view);
        when(gui.getMainPanel()).thenReturn(new JPanel());
        doCallRealMethod().when(gui).centerOnUnit(any());
        doAnswer(invocation -> {
            BoardLocation location = invocation.getArgument(0);
            if (fixture.game.hasBoardLocation(location)) {
                view.centerOnHex(location.coords());
            }
            return null;
        }).when(gui).centerOnHex(any());
        UnitOverviewOverlay overview = new UnitOverviewOverlay(gui);
        menus.addActionListener(event -> {
            if (event.getActionCommand().equals(ClientGUI.VIEW_GPU_BOARD)) {
                GpuBoardWindow.open(view, () -> fixture.panel);
            } else if (event.getActionCommand().equals(ClientGUI.VIEW_CLASSIC_BOARD)) {
                GpuBoardWindow.showClassic(gui);
            }
        });
        frame.setJMenuBar(menus);
        frame.setSize(960, 700);
        frame.setVisible(true);
        JMenu viewMenu = (JMenu) java.util.Arrays.stream(menus.getComponents())
              .filter(component -> component instanceof JMenu menu
                    && menu.getText().equals(Messages.getString("CommonMenuBar.ViewMenu"))).findFirst().orElseThrow();
        JMenuItem gpuChoice = java.util.Arrays.stream(viewMenu.getMenuComponents())
              .filter(component -> component instanceof JMenuItem item
                    && ClientGUI.VIEW_GPU_BOARD.equals(item.getActionCommand()))
              .map(JMenuItem.class::cast).findFirst().orElseThrow();
        return new ClientWindow(frame, menus, view, gpuChoice, overview);
    }

    private static <T> T onSwing(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeLater(task);
        return task.get(30, TimeUnit.SECONDS);
    }

    private static <T> T onGl(Callable<T> action) throws Exception {
        Application app = Gdx.app;
        FutureTask<T> task = new FutureTask<>(action);
        app.postRunnable(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static void captureMenu(String name) throws Exception {
        await(() -> onGl(() -> GpuBoardTestUi.stage().getRoot().findActor("tactical-menu").getColor().a == 1));
        onGl(() -> {
            assertFalse(GpuBoardTestUi.stage().getRoot().findActor("command-search").getParent().isVisible(),
                  "Menu-bar dropdowns do not include command search");
            File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
            assertTrue(output.isDirectory() || output.mkdirs());
            GpuBoardTestUi.capture(new File(output, name));
            return null;
        });
    }

    private static void input(Runnable action) throws Exception {
        long previous = onGl(() -> {
            action.run();
            return ((GpuBattleView) Gdx.app.getApplicationListener()).frames();
        });
        await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames() > previous));
    }

    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        while (!condition.call()) {
            assertTrue(System.nanoTime() < deadline, "Timed out waiting for the board window handoff");
            Thread.sleep(25);
        }
    }
}
