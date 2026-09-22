/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.logging.MMLogger;

/** Owns the default battle window. A single libGDX application avoids competing global Gdx contexts. */
public final class GpuBoardWindow {
    static final boolean DEFAULT_VSYNC = true;
    /** The planar compatibility layer omits wreck sprites when the shared unit renderer supplies them. */
    public static boolean modelsEnabled() { return GpuUnitModels.ENABLED; }
    private static final MMLogger LOGGER = MMLogger.create(GpuBoardWindow.class);
    private static GpuBoardWindow active;
    private final ClientGUI gui;
    private final BoardView initialView;
    private final Supplier<JComponent> panel;
    private final Timer startupTimer;
    private volatile GpuBoardSource source;
    private volatile String loadingMessage = Messages.getString("ClientGUI.waitingOnTheServer");
    private final Window classicWindow;
    private final Map<Dialog, Boolean> dialogOnTop = new IdentityHashMap<>();
    /**
     * The classic window hides while the GPU view runs, so a Swing dialog owned by it (deployment elevation choices,
     * alerts, item pickers) would open behind the native window. Every client dialog that opens while the GPU window is
     * presented is raised above it instead.
     */
    private final AWTEventListener dialogListener = event -> {
        if (event.getID() == ComponentEvent.COMPONENT_SHOWN
              && event.getSource() instanceof Dialog dialog && belongsToClassicWindow(dialog)) {
            dialogOnTop.putIfAbsent(dialog, dialog.isAlwaysOnTop());
            dialog.setAlwaysOnTop(true);
            dialog.toFront();
        }
    };
    private volatile Lwjgl3Application application;
    private volatile boolean closing;
    private volatile boolean presented;
    private volatile boolean restoreClassic;
    private volatile boolean exitRequested;
    private volatile Throwable startupFailure;
    private Runnable afterClose;

    private GpuBoardWindow(ClientGUI gui, BoardView view, Supplier<JComponent> panel) {
        this.gui = gui;
        initialView = view;
        this.panel = panel;
        classicWindow = gui == null ? SwingUtilities.getWindowAncestor(view.getPanel()) : gui.getFrame();
        startupTimer = new Timer(100, event -> initializeSource());
    }

    public static synchronized void open(BoardView view, Supplier<JComponent> panel) {
        open(view.getClientgui(), view, panel);
    }

    /** Start the chosen board window even before a scenario or server has delivered its first map. */
    public static synchronized void open(ClientGUI gui, Supplier<JComponent> panel) {
        open(gui, null, panel);
    }

    private static void open(ClientGUI gui, BoardView view, Supplier<JComponent> panel) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Open the GPU board on the Swing event thread");
        }
        if (active != null) {
            if (active.gui != gui || (gui == null && active.initialView != view)) {
                JOptionPane.showMessageDialog(gui == null ? view.getPanel() : gui.getFrame(),
                      Messages.getString("GpuBoard.alreadyOpen"));
            } else if (!active.closing) {
                active.focus(false);
            } else {
                // A new game may arrive while the previous game's native window is still being disposed.
                active.restoreClassic = false;
                active.afterClose = () -> open(gui, view, panel);
            }
            return;
        }
        GpuBoardWindow window = new GpuBoardWindow(gui, view, panel);
        try {
            active = window;
            if (gui != null) {
                GUIPreferences.getInstance().setUse3DBoard(true);
                gui.getMenuBar().setBoardView3D(true);
                gui.setMiniReportLocation(false);
            }
            Toolkit.getDefaultToolkit().addAWTEventListener(window.dialogListener, AWTEvent.COMPONENT_EVENT_MASK);
            Thread thread = new Thread(window::run, "MegaMek-GPU-board");
            thread.setDaemon(true);
            thread.start();
        } catch (RuntimeException | LinkageError failure) {
            if (active != null) {
                active.restoreDialogPresentation();
            }
            active = null;
            window.reportFailure(failure);
        }
    }

    private void run() {
        Throwable failure = null;
        try {
            // The native window owns startup; its first visible frame begins the entrance animation.
            Lwjgl3ApplicationConfiguration configuration = configuration(false);
            // Fill the desktop work area while keeping the normal title bar and window controls.
            configuration.setDecorated(true);
            configuration.setMaximized(true);
            configuration.setWindowListener(new Lwjgl3WindowAdapter() {
                @Override
                public boolean closeRequested() {
                    requestExit();
                    return false;
                }

                @Override
                public void focusLost() {
                    if (Gdx.app.getApplicationListener() instanceof GpuBattleView battle) {
                        battle.pause();
                    }
                }
            });
            new Lwjgl3Application(new GpuBattleView(null) {
                private boolean presentationRequested;

                @Override
                public void create() {
                    application = (Lwjgl3Application) Gdx.app;
                    super.create();
                    prepareEntrance();
                    if (closing) {
                        application.exit();
                    }
                }

                @Override
                public void render() {
                    setLoadingMessage(loadingMessage);
                    super.render();
                    if (!presentationRequested) {
                        presentationRequested = true;
                        SwingUtilities.invokeLater(GpuBoardWindow.this::present);
                    }
                }
            }, configuration);
        } catch (RuntimeException | LinkageError error) {
            failure = error;
        } finally {
            closing = true;
            application = null;
            if (source != null) {
                source.close();
            }
            Throwable renderingFailure = failure;
            SwingUtilities.invokeLater(() -> finish(renderingFailure == null ? startupFailure : renderingFailure));
        }
    }

    private void present() {
        if (closing) {
            return;
        }
        presented = true;
        focus(true);
        if (classicWindow != null) {
            classicWindow.setVisible(false);
        }
        if (gui != null) {
            gui.setClassicBoardViewEnabled(false);
            gui.refreshAuxiliaryWindows();
        }
        startupTimer.start();
    }

    private void initializeSource() {
        if (closing || source != null) {
            startupTimer.stop();
            return;
        }
        String status = GpuBoardActions.phaseStatus(panel.get()).text();
        loadingMessage = status.isBlank() ? Messages.getString("ClientGUI.waitingOnTheServer") : status;
        BoardView view = gui == null ? initialView : gui.getCurrentBoardView()
              .filter(BoardView.class::isInstance).map(BoardView.class::cast).orElse(null);
        if (view == null) {
            return;
        }
        try {
            source = new GpuBoardSource(view, panel);
            startupTimer.stop();
            application.postRunnable(() -> {
                if (!closing) {
                    ((GpuBattleView) Gdx.app.getApplicationListener()).attachSource(source);
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            startupFailure = failure;
            close(false);
        }
    }

    /** Native close is the client's normal quit action. Cancelled saves leave this window running. */
    private void requestExit() {
        if (exitRequested || closing) {
            return;
        }
        exitRequested = true;
        SwingUtilities.invokeLater(() -> {
            try {
                if (gui == null) {
                    close(false);
                } else {
                    gui.handleExit();
                }
            } finally {
                exitRequested = false;
            }
        });
    }

    /** True when the window is the classic window itself or a dialog chain owned by it. */
    private boolean belongsToClassicWindow(Window window) {
        for (Window current = window; current != null; current = current.getOwner()) {
            if (current == classicWindow) {
                return true;
            }
        }
        return false;
    }

    private void finish(Throwable failure) {
        startupTimer.stop();
        restoreDialogPresentation();
        synchronized (GpuBoardWindow.class) {
            if (active != this) {
                return;
            }
            active = null;
        }
        // The native window is already destroyed. Never resurrect a client that is shutting down.
        if (afterClose != null) {
            afterClose.run();
        } else if (restoreClassic && classicWindow != null) {
            showClassicWindow(gui, classicWindow);
        }
        if (failure != null) {
            reportFailure(failure);
        }
    }

    private void restoreDialogPresentation() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(dialogListener);
        dialogOnTop.forEach(Dialog::setAlwaysOnTop);
        dialogOnTop.clear();
    }

    /** Return to the existing client without disconnecting or changing the game. */
    public static synchronized void showClassic(ClientGUI gui) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showClassic(gui));
            return;
        }
        if (active != null && active.gui == gui) {
            active.close(true);
        } else if (gui.getFrame() != null) {
            showClassicWindow(gui, gui.getFrame());
        }
    }

    /** Includes preparation: classic panels must not open while the native window is starting. */
    public static synchronized boolean isActiveFor(ClientGUI gui) {
        return active != null && active.gui == gui;
    }

    private static void showClassicWindow(ClientGUI gui, Window window) {
        if (gui != null) {
            gui.setClassicBoardViewEnabled(!gui.getClient().getGame().getPhase().isLounge());
            gui.getMenuBar().setBoardView3D(false);
        }
        if (window instanceof Frame frame && (frame.getExtendedState() & Frame.ICONIFIED) != 0) {
            frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
        }
        window.setVisible(true);
        window.toFront();
        window.requestFocus();
        if (gui != null) {
            gui.refreshAuxiliaryWindows();
        }
    }

    static Lwjgl3ApplicationConfiguration configuration(boolean visible) {
        // Supported by current LWJGL; no retired AWT extension or macOS JVM relaunch is needed here.
        Lwjgl3ApplicationConfiguration.useGlfwAsync();
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle(Messages.getString("GpuBoard.title"));
        configuration.setWindowedMode(1280, 800);
        configuration.setWindowSizeLimits(900, 600, -1, -1);
        configuration.setInitialVisible(visible);
        // Keep rendering bounded even when VSync is disabled, we cap at 60fps. With VSync we let it do what it needs...
        configuration.setForegroundFPS(DEFAULT_VSYNC ? 0 : 60);
        configuration.useVsync(DEFAULT_VSYNC);
        configuration.setDepthBits(24);
        configuration.disableAudio(true);
        configuration.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void focusLost() {
                if (Gdx.app != null && Gdx.app.getApplicationListener() instanceof GpuBattleView battle) {
                    battle.pause();
                }
            }
        });
        return configuration;
    }

    private void focus(boolean entering) {
        Lwjgl3Application app = application;
        if (app != null && presented) {
            app.postRunnable(() -> {
                if (!closing) {
                    if (entering && app.getApplicationListener() instanceof GpuBattleView battle) {
                        battle.startEntrance();
                    }
                    var window = ((Lwjgl3Graphics) app.getGraphics()).getWindow();
                    window.setVisible(true);
                    window.focusWindow();
                }
            });
        }
    }

    private void close(boolean returnToClassic) {
        afterClose = null;
        restoreClassic = returnToClassic;
        closing = true;
        startupTimer.stop();
        if (source != null) {
            source.close();
        }
        Lwjgl3Application app = application;
        if (app != null) {
            app.postRunnable(app::exit);
        }
    }

    public static synchronized void closeFor(BoardView view) {
        if (active != null && (active.initialView == view || active.source != null && active.source.currentView() == view)) {
            active.close(false);
        }
    }

    public static synchronized void closeFor(ClientGUI gui) {
        if (active != null && active.gui == gui) {
            active.close(false);
        }
    }

    private void reportFailure(Throwable failure) {
        LOGGER.error("GPU battle view failed", failure);
        Object[] choices = { Messages.getString("CommonMenuBar.viewGpuBoard"),
              Messages.getString("CommonMenuBar.viewClassicBoard"), Messages.getString("MegaMek.Quit.label") };
        int choice = JOptionPane.showOptionDialog(classicWindow, Messages.getString("GpuBoard.unavailable"),
              Messages.getString("CommonMenuBar.viewGpuBoard"), JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
              null, choices, choices[0]);
        if (choice == 0) {
            open(gui, initialView, panel);
        } else if (choice == 1 && gui != null) {
            GUIPreferences.getInstance().setUse3DBoard(false);
            showClassicWindow(gui, classicWindow);
        } else if (gui != null) {
            gui.handleExit();
        }
    }
}
