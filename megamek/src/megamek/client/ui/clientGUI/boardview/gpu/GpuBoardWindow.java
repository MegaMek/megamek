/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.logging.MMLogger;

/** Owns the optional native window. A single libGDX application avoids competing global Gdx contexts. */
public final class GpuBoardWindow {
    private static final MMLogger LOGGER = MMLogger.create(GpuBoardWindow.class);
    private static GpuBoardWindow active;
    private final BoardView view;
    private final GpuBoardSource source;
    private final Window classicWindow;
    /**
     * The classic window hides while the GPU view runs, so a Swing dialog owned by it (deployment elevation choices,
     * alerts, item pickers) would open behind the native window. Every client dialog that opens while the GPU window is
     * presented is raised above it instead.
     */
    private final AWTEventListener dialogListener = event -> {
        if ((event instanceof WindowEvent windowEvent) && (windowEvent.getID() == WindowEvent.WINDOW_OPENED)
              && (windowEvent.getWindow() instanceof Dialog dialog) && belongsToClassicWindow(dialog)) {
            dialog.setAlwaysOnTop(true);
            dialog.toFront();
        }
    };
    private volatile Lwjgl3Application application;
    private volatile boolean closing;
    private volatile boolean presented;
    private volatile boolean restoreClassic = true;

    private GpuBoardWindow(BoardView view, Supplier<JComponent> panel) {
        this.view = view;
        classicWindow = view.getClientgui() == null ? SwingUtilities.getWindowAncestor(view.getPanel())
              : view.getClientgui().getFrame();
        source = new GpuBoardSource(view, panel);
    }

    public static synchronized void open(BoardView view, Supplier<JComponent> panel) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Open the GPU board on the Swing event thread");
        }
        if (active != null) {
            if (active.view.game != view.game) {
                JOptionPane.showMessageDialog(view.getPanel(), Messages.getString("GpuBoard.alreadyOpen"));
            } else if (!active.closing) {
                active.focus();
            }
            return;
        }
        try {
            active = new GpuBoardWindow(view, panel);
            GpuBoardWindow window = active;
            Thread thread = new Thread(window::run, "MegaMek-GPU-board");
            thread.setDaemon(true);
            thread.start();
        } catch (RuntimeException | LinkageError failure) {
            reportFailure(view, failure);
            active = null;
        }
    }

    private void run() {
        Throwable failure = null;
        try {
            // Prepare a complete frame before replacing the visible UI.
            Lwjgl3ApplicationConfiguration configuration = configuration(false);
            new Lwjgl3Application(new GpuBattleView(source) {
                private boolean presentationRequested;

                @Override
                public void create() {
                    application = (Lwjgl3Application) Gdx.app;
                    super.create();
                }

                @Override
                public void render() {
                    super.render();
                    if (!presentationRequested && frames() > 0) {
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
            source.close();
            Throwable renderingFailure = failure;
            SwingUtilities.invokeLater(() -> finish(renderingFailure));
        }
    }

    private void present() {
        if (closing || source.isClosed()) {
            return;
        }
        if (classicWindow != null) {
            classicWindow.setVisible(false);
        }
        presented = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(dialogListener, AWTEvent.WINDOW_EVENT_MASK);
        focus();
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
        Toolkit.getDefaultToolkit().removeAWTEventListener(dialogListener);
        synchronized (GpuBoardWindow.class) {
            if (active != this) {
                return;
            }
            active = null;
        }
        // The native window is already destroyed. Never resurrect a client that is shutting down.
        if (restoreClassic && classicWindow != null && classicWindow.isDisplayable()) {
            showClassicWindow(classicWindow);
        }
        if (failure != null && restoreClassic) {
            reportFailure(view, failure);
        }
    }

    /** Return to the existing client without disconnecting or changing the game. */
    public static synchronized void showClassic(ClientGUI gui) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showClassic(gui));
            return;
        }
        if (active != null && active.view.game == gui.getClient().getGame()) {
            active.close(true);
        } else if (gui.getFrame().isDisplayable()) {
            showClassicWindow(gui.getFrame());
        }
    }

    private static void showClassicWindow(Window window) {
        if (window instanceof Frame frame && (frame.getExtendedState() & Frame.ICONIFIED) != 0) {
            frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
        }
        window.setVisible(true);
        window.toFront();
        window.requestFocus();
    }

    static Lwjgl3ApplicationConfiguration configuration(boolean visible) {
        // Supported by current LWJGL; no retired AWT extension or macOS JVM relaunch is needed here.
        Lwjgl3ApplicationConfiguration.useGlfwAsync();
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle(Messages.getString("GpuBoard.title"));
        configuration.setWindowedMode(1280, 800);
        configuration.setWindowSizeLimits(900, 600, -1, -1);
        configuration.setInitialVisible(visible);
        configuration.setForegroundFPS(60);
        configuration.useVsync(true);
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

    private void focus() {
        Lwjgl3Application app = application;
        if (app != null && presented) {
            app.postRunnable(() -> {
                if (!closing && !source.isClosed()) {
                    var window = ((Lwjgl3Graphics) app.getGraphics()).getWindow();
                    window.setVisible(true);
                    window.focusWindow();
                }
            });
        }
    }

    private void close(boolean returnToClassic) {
        if (!returnToClassic) {
            restoreClassic = false;
        }
        closing = true;
        source.close();
        Lwjgl3Application app = application;
        if (app != null) {
            app.postRunnable(app::exit);
        }
    }

    public static synchronized void closeFor(BoardView view) {
        if (active != null && active.source.currentView() == view) {
            active.close(false);
        }
    }

    private static void reportFailure(BoardView view, Throwable failure) {
        LOGGER.error("GPU battle view failed", failure);
        JOptionPane.showMessageDialog(view.getPanel(), Messages.getString("GpuBoard.unavailable"),
              Messages.getString("CommonMenuBar.viewGpuBoard"), JOptionPane.ERROR_MESSAGE);
    }
}
