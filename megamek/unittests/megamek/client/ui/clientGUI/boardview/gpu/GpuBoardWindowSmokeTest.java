/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
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
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.BoardViewsContainer;
import megamek.client.ui.clientGUI.CommandBarPanel;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.UnitOverviewOverlay;
import megamek.client.ui.dialogs.miniReport.MiniReportDisplayDialog;
import megamek.client.ui.dialogs.miniReport.MiniReportDisplayPanel;
import megamek.client.ui.dialogs.forceDisplay.ForceDisplayDialog;
import megamek.client.ui.dialogs.forceDisplay.ForceDisplayPanel;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayDialog;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.dialogs.BotCommands.BotCommandsDialog;
import megamek.client.ui.dialogs.BotCommands.BotCommandsPanel;
import megamek.client.ui.dialogs.minimap.MinimapDialog;
import megamek.client.ui.entityreadout.LiveReadoutDialog;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.panels.StartingScenarioPanel;
import megamek.client.ui.panels.WaitingForServerPanel;
import megamek.common.Report;
import megamek.common.Player;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.lwjgl.glfw.GLFW;

/** Exercises the real Swing/native window handoff and the shared menu through Scene2D input. */
@Tag("on-demand")
class GpuBoardWindowSmokeTest {
    private boolean boardStyle;

    @BeforeEach
    void saveBoardStyle() {
        boardStyle = GUIPreferences.getInstance().getUse3DBoard();
    }

    @AfterEach
    void restoreBoardStyle() throws Exception {
        await(() -> Thread.getAllStackTraces().keySet().stream()
              .noneMatch(thread -> thread.getName().equals("MegaMek-GPU-board")));
        GUIPreferences.getInstance().setUse3DBoard(boardStyle);
    }
    private record ClientWindow(JFrame frame, CommonMenuBar menus, BoardView view, JMenuItem gpuChoice,
          UnitOverviewOverlay overview) { }

    @Test
    void startsDirectlyInThreeDimensionsWithoutConstructingTheClassicViewport() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            ClientWindow ui = onSwing(() -> createClientWindow(fixture, false));
            try {
                onSwing(() -> {
                    ui.view().centerOn(fixture.entity);
                    assertEquals(fixture.entity.getId(), ui.view().getCenterRequest().entityId());
                    assertNull(ui.view().getPanel().getParent());
                    return null;
                });
                openNative(ui);
                await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames() >= 5));
                onSwing(() -> {
                    verify(ui.view(), never()).getComponent();
                    assertNull(ui.view().getPanel().getParent());
                    assertFalse(ui.frame().isVisible());
                    assertTrue(GpuBoardWindow.isActiveFor(ui.view().getClientgui()));
                    GpuBoardWindow.showClassic(ui.view().getClientgui());
                    return null;
                });
                await(() -> onSwing(() -> ui.frame().isVisible()));
                onSwing(() -> {
                    verify(ui.view()).getComponent();
                    assertTrue(ui.view().getPanel().isShowing(), "The legacy viewport is created on explicit return");
                    return null;
                });
            } finally {
                onSwing(() -> {
                    GpuBoardWindow.closeFor(ui.view());
                    ui.frame().dispose();
                    ui.view().dispose();
                    GUIPreferences.getInstance().removePreferenceChangeListener(ui.menus());
                    return null;
                });
            }
        }
    }

    @Test
    void unitDialogsOpenOverTheNativeBoardAndKeepTheirClassicSettings() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        int location = preferences.getUnitDisplayLocation();
        boolean unitEnabled = preferences.getUnitDisplayEnabled();
        boolean forceEnabled = preferences.getForceDisplayEnabled();
        boolean overviewEnabled = preferences.getShowUnitOverview();
        AtomicInteger restoredLocation = new AtomicInteger(-1);
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            ClientWindow ui = onSwing(() -> createClientWindow(fixture));
            ClientGUI gui = ui.view().getClientgui();
            UnitDisplayDialog unit = onSwing(() -> {
                preferences.setUnitDisplayLocation(1);
                preferences.setUnitDisplayEnabled(false);
                preferences.setForceDisplayEnabled(false);
                preferences.setShowUnitOverview(false);
                UnitDisplayPanel panel = new UnitDisplayPanel(gui);
                UnitDisplayDialog dialog = new UnitDisplayDialog(ui.frame(), gui);
                when(gui.getUnitDisplay()).thenReturn(panel);
                when(gui.getUnitDisplayDialog()).thenReturn(dialog);
                panel.displayEntity(fixture.entity);
                doCallRealMethod().when(gui).setUnitDisplayVisible(anyBoolean());
                doAnswer(invocation -> {
                    if (GpuBoardWindow.isActiveFor(gui)) {
                        invocation.callRealMethod();
                    } else {
                        // Observe the request; this fixture has no legacy split panes.
                        restoredLocation.set(preferences.getUnitDisplayLocation());
                    }
                    return null;
                }).when(gui).setUnitDisplayLocation(anyBoolean());
                doCallRealMethod().when(gui).setForceDisplayVisible(anyBoolean());
                doCallRealMethod().when(gui).actionPerformed(any());
                doCallRealMethod().when(gui).preferenceChange(any());
                ui.menus().addActionListener(gui);
                preferences.addPreferenceChangeListener(gui);
                ui.view().addOverlay(ui.overview());
                return dialog;
            });
            ForceDisplayDialog force = onSwing(() -> {
                ForceDisplayDialog dialog = new ForceDisplayDialog(ui.frame(), gui);
                ForceDisplayPanel panel = new ForceDisplayPanel(gui);
                when(gui.getForceDisplayPanel()).thenReturn(panel);
                dialog.add(panel);
                when(gui.getForceDisplayDialog()).thenReturn(dialog);
                // Reuse a dialog that has already emitted WINDOW_OPENED in the classic view.
                dialog.setVisible(true);
                dialog.setVisible(false);
                return dialog;
            });
            try {
                openNative(ui);
                await(() -> onSwing(() -> ui.view().sidePanelInset() > 0));
                input(() -> GpuBoardTestUi.click("battle-report-toggle"));
                pressShortcut(KeyCommandBind.UNIT_DISPLAY);
                await(() -> onSwing(() -> unit.isShowing() && unit.isAlwaysOnTop()));
                pressShortcut(KeyCommandBind.FORCE_DISPLAY);
                await(() -> onSwing(() -> force.isShowing() && force.isAlwaysOnTop()));
                onSwing(() -> {
                    assertFalse(ui.frame().isVisible());
                    assertTrue(SwingUtilities.isDescendingFrom(gui.getUnitDisplay(), unit));
                    assertSame(fixture.entity, gui.getUnitDisplay().getCurrentEntity());
                    assertEquals(1, preferences.getUnitDisplayLocation());
                    force.dispatchEvent(new WindowEvent(force, WindowEvent.WINDOW_CLOSING));
                    force.setAlwaysOnTop(false);
                    fixture.game.setPhase(GamePhase.FIRING_REPORT);
                    ui.menus().setPhase(GamePhase.FIRING_REPORT);
                    return null;
                });
                pressShortcut(KeyCommandBind.FORCE_DISPLAY);
                await(() -> onSwing(() -> force.isShowing() && force.isAlwaysOnTop()));
                pressShortcut(KeyCommandBind.UNIT_DISPLAY);
                await(() -> onSwing(() -> !unit.isVisible()));
                pressShortcut(KeyCommandBind.UNIT_DISPLAY);
                await(() -> onSwing(() -> unit.isShowing() && unit.isAlwaysOnTop()));
                input(() -> GpuBoardTestUi.click("battle-report-toggle"));
                pressShortcut(KeyCommandBind.UNIT_OVERVIEW);
                onSwing(() -> {
                    assertFalse(preferences.getShowUnitOverview());
                    assertTrue(ui.view().sidePanelInset() > 0);
                    GpuBoardWindow.showClassic(gui);
                    return null;
                });
                await(() -> onSwing(() -> ui.frame().isVisible() && restoredLocation.get() == 1));
                onSwing(() -> {
                    assertFalse(unit.isAlwaysOnTop());
                    assertFalse(force.isAlwaysOnTop());
                    assertEquals(0, ui.view().sidePanelInset(), "2D retains its hidden overview preference");
                    return null;
                });
            } finally {
                onSwing(() -> {
                    preferences.removePreferenceChangeListener(gui);
                    preferences.removePreferenceChangeListener(gui.getForceDisplayPanel());
                    fixture.game.removeGameListener(gui.getForceDisplayPanel());
                    GpuBoardWindow.closeFor(ui.view());
                    unit.dispose();
                    force.dispose();
                    ui.frame().dispose();
                    ui.view().dispose();
                    ui.menus().die();
                    preferences.setUnitDisplayLocation(location);
                    preferences.setUnitDisplayEnabled(unitEnabled);
                    preferences.setForceDisplayEnabled(forceEnabled);
                    preferences.setShowUnitOverview(overviewEnabled);
                    return null;
                });
            }
        }
    }

    @Test
    void nativeStartupShowsPhaseMessagesBeforeTheFirstMapArrives() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            ClientWindow ui = onSwing(() -> createClientWindow(fixture, false));
            ClientGUI gui = ui.view().getClientgui();
            AtomicReference<JComponent> phase = new AtomicReference<>();
            try {
                Application previous = Gdx.app;
                onSwing(() -> {
                    phase.set(new StartingScenarioPanel());
                    when(gui.getCurrentBoardView()).thenReturn(Optional.empty());
                    GpuBoardWindow.open(gui, phase::get);
                    return null;
                });
                await(() -> Gdx.app != null && Gdx.app != previous);
                await(() -> onGl(() -> {
                    var label = GpuBoardTestUi.stage().getRoot().findActor("board-loading-message");
                    return label instanceof com.badlogic.gdx.scenes.scene2d.ui.Label message
                          && message.getText().toString().contains("Starting Scenario");
                }));
                assertFalse(onSwing(() -> ui.frame().isVisible()));
                long window = onGl(() -> ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle());
                onSwing(() -> {
                    verify(ui.view(), never()).getComponent();
                    phase.set(new WaitingForServerPanel());
                    return null;
                });
                await(() -> onGl(() -> {
                    com.badlogic.gdx.scenes.scene2d.ui.Label message = GpuBoardTestUi.stage().getRoot()
                          .findActor("board-loading-message");
                    return message.getText().toString().equals(Messages.getString("ClientGUI.waitingOnTheServer"));
                }));
                onSwing(() -> {
                    when(gui.getCurrentBoardView()).thenReturn(Optional.of(ui.view()));
                    return null;
                });
                await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames() >= 5));
                onGl(() -> {
                    assertEquals(window, ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle());
                    assertTrue(GpuBoardTestUi.stage().getRoot().findActor("board-phase-notice").isVisible());
                    assertNull(GpuBoardTestUi.stage().getRoot().findActor("board-loading-message"));
                    return null;
                });
                onSwing(() -> { phase.set(fixture.panel); return null; });
                await(() -> onGl(() -> !GpuBoardTestUi.stage().getRoot().findActor("board-phase-notice").isVisible()));
                onSwing(() -> {
                    verify(ui.view(), never()).getComponent();
                    assertFalse(ui.frame().isVisible());
                    return null;
                });
                BoardView replacement = onSwing(() -> {
                    BoardView view = new BoardView(fixture.game, null, gui, 0);
                    view.setLocalPlayer(fixture.player);
                    ui.view().dispose();
                    when(gui.getCurrentBoardView()).thenReturn(Optional.of(view));
                    return view;
                });
                try {
                    assertTrue(GpuBoardWindow.isActiveFor(gui), "Replacing the map must not dispose the client's window");
                    onGl(() -> {
                        assertEquals(window, ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle());
                        return null;
                    });
                    Application restarting = Gdx.app;
                    onSwing(() -> {
                        GpuBoardWindow.showClassic(gui);
                        GpuBoardWindow.open(gui, phase::get);
                        return null;
                    });
                    await(() -> Gdx.app != null && Gdx.app != restarting);
                    await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames() > 0));
                    assertFalse(onSwing(() -> ui.frame().isVisible()), "A queued native restart never flashes 2D");
                } finally {
                    onSwing(() -> {
                        GpuBoardWindow.closeFor(gui);
                        replacement.dispose();
                        return null;
                    });
                }
            } finally {
                onSwing(() -> {
                    GpuBoardWindow.closeFor(gui);
                    ui.frame().dispose();
                    ui.view().dispose();
                    ui.menus().die();
                    return null;
                });
            }
        }
    }

    @Test
    void nativeCloseUsesTheSavePromptAndCancellationKeepsTheSameWindow() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        boolean noSaveNag = preferences.getNoSaveNag();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            ClientWindow ui = onSwing(() -> createClientWindow(fixture, false));
            ClientGUI gui = ui.view().getClientgui();
            AtomicInteger quit = new AtomicInteger();
            try {
                onSwing(() -> {
                    preferences.setValue(GUIPreferences.ADVANCED_NO_SAVE_NAG, false);
                    doCallRealMethod().when(gui).handleExit();
                    doAnswer(invocation -> {
                        quit.incrementAndGet();
                        GpuBoardWindow.closeFor(gui);
                        ui.frame().dispose();
                        return null;
                    }).when(gui).die();
                    return null;
                });
                openNative(ui);
                long window = onGl(() -> ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle());
                for (int response : new int[] { JOptionPane.CANCEL_OPTION, JOptionPane.YES_OPTION, JOptionPane.NO_OPTION }) {
                    onGl(() -> {
                        // Invoke LWJGL's installed OS close callback; closeWindow() would bypass its confirmation hook.
                        var callback = GLFW.glfwSetWindowCloseCallback(window, null);
                        GLFW.glfwSetWindowCloseCallback(window, callback);
                        callback.invoke(window);
                        return null;
                    });
                    await(() -> onSwing(() -> savePrompt(ui.frame()) != null));
                    onSwing(() -> {
                        JOptionPane prompt = savePrompt(ui.frame());
                        assertEquals(Messages.getString("ClientGUI.gameSaveDialogMessage"), prompt.getMessage());
                        assertTrue(SwingUtilities.getWindowAncestor(prompt).isAlwaysOnTop());
                        prompt.setValue(response);
                        return null;
                    });
                    onSwing(() -> null);
                    if (response != JOptionPane.NO_OPTION) {
                        assertEquals(0, quit.get(), "Cancelling or failing to save must keep the game open");
                        assertTrue(GpuBoardWindow.isActiveFor(gui));
                        assertFalse(onSwing(() -> ui.frame().isVisible()));
                        assertEquals(window, onGl(() -> ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle()));
                    }
                }
                await(() -> !GpuBoardWindow.isActiveFor(gui));
                assertEquals(1, quit.get());
                assertFalse(onSwing(() -> ui.frame().isVisible()));
            } finally {
                onSwing(() -> {
                    for (Window owned : ui.frame().getOwnedWindows()) {
                        owned.dispose();
                    }
                    GpuBoardWindow.closeFor(gui);
                    ui.frame().dispose();
                    ui.view().dispose();
                    ui.menus().die();
                    preferences.setValue(GUIPreferences.ADVANCED_NO_SAVE_NAG, noSaveNag);
                    return null;
                });
            }
        }
    }

    private static JOptionPane savePrompt(JFrame frame) {
        for (Window window : frame.getOwnedWindows()) {
            if (window instanceof JDialog dialog && dialog.isShowing()) {
                for (var child : dialog.getContentPane().getComponents()) {
                    if (child instanceof JOptionPane pane) {
                        return pane;
                    }
                }
            }
        }
        return null;
    }

    @Test
    void botCommandsAndMinimapStayAccessibleWithoutTheClassicDock() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        int location = preferences.getBotCommandsLocation();
        boolean botEnabled = preferences.getBotCommandsEnabled();
        boolean mapEnabled = preferences.getMinimapEnabled();
        int botAuto = preferences.getBotCommandsAutoDisplayNonReportPhase();
        int mapAuto = preferences.getMinimapAutoDisplayNonReportPhase();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            ClientWindow ui = onSwing(() -> createClientWindow(fixture, false));
            ClientGUI gui = ui.view().getClientgui();
            BotCommandsDialog bot = onSwing(() -> {
                Player player = new Player(77, "Bot");
                player.setBot(true);
                fixture.game.addPlayer(player.getId(), player);
                preferences.setBotCommandsLocation(ClientGUI.BOT_COMMANDS_LOCATION_DOCKED);
                preferences.setBotCommandsEnabled(true);
                preferences.setBotCommandAutoDisplayNonReportPhase(GUIPreferences.SHOW);
                preferences.setMinimapAutoDisplayNonReportPhase(GUIPreferences.SHOW);
                BotCommandsDialog dialog = new BotCommandsDialog(ui.frame(), gui);
                BotCommandsPanel panel = new BotCommandsPanel(gui.getClient(), null, null, gui);
                CommandBarPanel bar = new CommandBarPanel(gui);
                JPanel top = new JPanel(new BorderLayout());
                top.add(bar, BorderLayout.NORTH);
                setField(ClientGUI.class, gui, "botCommandsPanel", panel);
                setField(ClientGUI.class, gui, "commandBarPanel", bar);
                setField(ClientGUI.class, gui, "panTop", top);
                when(gui.getBotCommandsDialog()).thenReturn(dialog);
                doCallRealMethod().when(gui).preferenceChange(any());
                doCallRealMethod().when(gui).actionPerformed(any());
                preferences.addPreferenceChangeListener(gui);
                ui.menus().addActionListener(gui);
                return dialog;
            });
            MinimapDialog minimap = onSwing(() -> {
                MinimapDialog dialog = new MinimapDialog(ui.frame());
                dialog.add(new JPanel());
                dialog.setSize(240, 200);
                when(gui.getMiniMapDialog()).thenReturn(dialog);
                setField(AbstractClientGUI.class, gui, "miniMaps", java.util.Map.of(0, dialog));
                return dialog;
            });
            try {
                openNative(ui);
                await(() -> onSwing(() -> bot.isShowing() && bot.isAlwaysOnTop()
                      && minimap.isShowing() && minimap.isAlwaysOnTop()));
                assertEquals(ClientGUI.BOT_COMMANDS_LOCATION_DOCKED, preferences.getBotCommandsLocation());
                input(() -> GpuBoardTestUi.click("battle-report-toggle"));
                pressShortcut(KeyCommandBind.MINIMAP);
                await(() -> onSwing(() -> !minimap.isVisible()));
                pressShortcut(KeyCommandBind.MINIMAP);
                await(() -> onSwing(minimap::isShowing));
                onSwing(() -> {
                    preferences.setBotCommandsEnabled(false);
                    assertFalse(bot.isVisible());
                    preferences.setBotCommandsEnabled(true);
                    assertTrue(bot.isVisible());
                    GpuBoardWindow.showClassic(gui);
                    return null;
                });
                await(() -> onSwing(() -> ui.frame().isVisible() && !bot.isVisible()));
                onSwing(() -> {
                    assertEquals(0, bot.getContentPane().getComponentCount(), "The same panel returns to the 2D dock");
                    assertTrue(minimap.isVisible());
                    assertFalse(minimap.isAlwaysOnTop());
                    return null;
                });
            } finally {
                onSwing(() -> {
                    preferences.removePreferenceChangeListener(gui);
                    GpuBoardWindow.closeFor(ui.view());
                    bot.dispose();
                    minimap.dispose();
                    ui.frame().dispose();
                    ui.view().dispose();
                    ui.menus().die();
                    preferences.setBotCommandsLocation(location);
                    preferences.setBotCommandsEnabled(botEnabled);
                    preferences.setBotCommandAutoDisplayNonReportPhase(botAuto);
                    preferences.setMinimapAutoDisplayNonReportPhase(mapAuto);
                    preferences.setMinimapEnabled(mapEnabled);
                    return null;
                });
            }
        }
    }

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
                openNative(ui);
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
    void switchesExclusiveWindowsThroughMenusAndNeverRestoresClassicOnClientDisposal() throws Exception {
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
                    return null;
                });
                openNative(ui);
                await(() -> onSwing(() -> !ui.frame().isVisible()));
                awaitMaximizedWindow();
                await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames() >= 5));
                await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).boardCamera.entranceOpacity() == 1));
                onGl(() -> {
                    BoardCamera camera = ((GpuBattleView) Gdx.app.getApplicationListener()).boardCamera;
                    assertTrue(camera.isIsometric());
                    for (var tile : fixture.source.takeFrame().scene().tiles()) {
                        for (int corner = 0; corner < 6; corner++) {
                            Vector3 point = camera.camera.project(BoardGeometry.corner(tile.coords(), tile.elevation(), corner),
                                  0, 0, camera.camera.viewportWidth, camera.camera.viewportHeight);
                            assertTrue(point.x > 0 && point.x < camera.camera.viewportWidth
                                  && point.y > 0 && point.y < camera.camera.viewportHeight,
                                  "Opening the 3D board must fit the whole map despite earlier 2D focus requests");
                        }
                    }
                    return null;
                });
                onSwing(() -> { ui.view().centerOnHex(position); return null; });
                await(() -> onGl(() -> unitIsCentered(fixture)));
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
                    Runnable sidebarClick = () -> {
                        float scale = Gdx.graphics.getWidth() / GpuBoardTestUi.stage().getWidth();
                        float overlayScale = scale / (size[0] == 2560 ? 1.5f : 1f);
                        int x = Math.round(Gdx.graphics.getWidth() - 33 * overlayScale);
                        int y = Math.round(GpuBoardUi.TOP_HEIGHT * scale + 29 * overlayScale);
                        Gdx.input.getInputProcessor().touchDown(x, y, 0, Input.Buttons.LEFT);
                        Gdx.input.getInputProcessor().touchUp(x, y, 0, Input.Buttons.LEFT);
                    };
                    input(sidebarClick);
                    onSwing(() -> {
                        assertEquals(fixture.entity.getId(), ui.view().getCenterRequest().entityId());
                        return null;
                    });
                    awaitNavigation();
                    assertEquals(previousUnitClicks + 1, unitClicks.get(), "Sidebar centering must retain the existing selection event");
                    Vector3 settled = onGl(() -> {
                        GpuBattleView battle = (GpuBattleView) Gdx.app.getApplicationListener();
                        assertEquals(zoom, battle.boardCamera.camera.zoom, 0.001f, "Centering preserves zoom");
                        assertTrue(direction.epsilonEquals(battle.boardCamera.camera.direction, 0.001f), "Centering preserves the view angle");
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                        assertTrue(output.isDirectory() || output.mkdirs());
                        GpuBoardTestUi.capture(new File(output, "resize-" + size[0] + ".png"));
                        return battle.boardCamera.focus.cpy();
                    });
                    input(sidebarClick);
                    onSwing(() -> {
                        assertEquals(previousUnitClicks + 2, unitClicks.get());
                        return null;
                    });
                    awaitNavigation();
                    onGl(() -> {
                        GpuBattleView battle = (GpuBattleView) Gdx.app.getApplicationListener();
                        assertTrue(settled.epsilonEquals(battle.boardCamera.focus, .001f),
                              "A second sidebar click must retain the same framing, not snap to the unit's hex");
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

                assertFalse(preferences.getUse3DBoard(), "The last selected board is saved");
                assertEquals(ClientGUI.VIEW_GPU_BOARD, ui.gpuChoice().getActionCommand());
                // Reopen through the same menu. Client disposal must never bring the old frame back.
                openNative(ui);
                await(() -> onSwing(() -> !ui.frame().isVisible()));
                awaitMaximizedWindow();
                assertTrue(preferences.getUse3DBoard());
                assertEquals(ClientGUI.VIEW_CLASSIC_BOARD, ui.gpuChoice().getActionCommand());
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
        return createClientWindow(fixture, true);
    }

    private ClientWindow createClientWindow(GpuBoardFixture fixture, boolean classic) {
        fixture.source.close();
        ClientGUI gui = mock(ClientGUI.class, invocation -> switch (invocation.getMethod().getName()) {
            case "refreshAuxiliaryWindows", "setMapVisible", "setBotCommandsLocation",
                 "setPlayerListVisible", "setRoundsInAirVisible" -> invocation.callRealMethod();
            default -> org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        BoardViewsContainer container = mock(BoardViewsContainer.class);
        when(container.isClassicViewEnabled()).thenAnswer(invocation -> !GpuBoardWindow.isActiveFor(gui));
        setField(AbstractClientGUI.class, gui, "boardViewsContainer", container);
        setField(AbstractClientGUI.class, gui, "miniMaps", new HashMap<>());
        Client client = mock(Client.class);
        JFrame frame = new JFrame("MegaMek - Classic board switch test");
        CommonMenuBar menus = CommonMenuBar.getMenuBarForGame();
        menus.setPhase(GamePhase.MOVEMENT);
        if (classic) {
            frame.add(fixture.view.getComponent());
        }
        BoardView view = spy(fixture.view);
        // BoardViewPanel retains its original owner; let that owner create its viewport on demand too.
        doAnswer(invocation -> fixture.view.getComponent()).when(view).getComponent();
        doReturn(gui).when(view).getClientgui();
        when(gui.getClient()).thenReturn(client);
        when(client.getGame()).thenReturn(fixture.game);
        when(client.getLocalPlayer()).thenReturn(fixture.player);
        when(gui.getFrame()).thenReturn(frame);
        doAnswer(invocation -> {
            frame.getContentPane().removeAll();
            if (invocation.getArgument(0, Boolean.class)) {
                frame.add(view.getComponent());
            } else {
                fixture.view.releaseClassicView();
                view.releaseClassicView();
            }
            frame.validate();
            return null;
        }).when(gui).setClassicBoardViewEnabled(anyBoolean());
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
                GUIPreferences.getInstance().setUse3DBoard(true);
                GpuBoardWindow.open(view, () -> fixture.panel);
            } else if (event.getActionCommand().equals(ClientGUI.VIEW_CLASSIC_BOARD)) {
                GUIPreferences.getInstance().setUse3DBoard(false);
                GpuBoardWindow.showClassic(gui);
            }
        });
        frame.setJMenuBar(menus);
        frame.setSize(960, 700);
        frame.setVisible(classic);
        JMenu viewMenu = (JMenu) java.util.Arrays.stream(menus.getComponents())
              .filter(component -> component instanceof JMenu menu
                    && menu.getText().equals(Messages.getString("CommonMenuBar.ViewMenu"))).findFirst().orElseThrow();
        JMenuItem gpuChoice = java.util.Arrays.stream(viewMenu.getMenuComponents())
              .filter(component -> component instanceof JMenuItem item
                    && ClientGUI.VIEW_GPU_BOARD.equals(item.getActionCommand()))
              .map(JMenuItem.class::cast).findFirst().orElseThrow();
        return new ClientWindow(frame, menus, view, gpuChoice, overview);
    }

    private static void openNative(ClientWindow ui) throws Exception {
        Application previous = Gdx.app;
        onSwing(() -> {
            ui.gpuChoice().doClick(0);
            assertTrue(GpuBoardWindow.isActiveFor(ui.view().getClientgui()), "Native ownership includes startup");
            return null;
        });
        await(() -> Gdx.app != null && Gdx.app != previous);
        awaitMaximizedWindow();
        await(() -> onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames() > 0));
        assertFalse(onSwing(() -> ui.frame().isVisible()), "The native window replaces the old window");
    }

    private static <T> T onSwing(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeLater(task);
        return task.get(30, TimeUnit.SECONDS);
    }

    private static void awaitNavigation() throws Exception {
        long previous = onGl(() -> ((GpuBattleView) Gdx.app.getApplicationListener()).frames());
        await(() -> onGl(() -> {
            GpuBattleView battle = (GpuBattleView) Gdx.app.getApplicationListener();
            return battle.frames() > previous + 2 && !battle.boardCamera.isFraming();
        }));
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

    private static void pressShortcut(KeyCommandBind bind) throws Exception {
        input(() -> GpuBoardTestUi.press(bind));
        onSwing(() -> null);
    }

    private static void setField(Class<?> owner, Object instance, String name, Object value) {
        try {
            var field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
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
