package megamek.client.ui.swing;

import javax.swing.*;

public interface IClientGUI {

    /**
     * @return The JFrame this client is displayed in.
     */
    JFrame getFrame();

    /**
     * Returns true if a dialog is visible on top of the ClientGUI. For example, the MegaMekController
     * should ignore hotkeys if there is a dialog, like the CommonSettingsDialog, open.
     *
     * @return True when hotkey events should not be forwarded to this ClientGUI
     */
    boolean shouldIgnoreHotKeys();

    /**
     * Registers this ClientGUI as a listener wherever it's needed.
     * It is generally considered bad practice to do this in the constructor.
     */
    void initialize();

    /**
     * Performs shut down for threads and sockets and other things that can be disposed.
     */
    void die();
}
