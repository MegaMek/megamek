/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import megamek.client.IClient;
import megamek.common.InGameObject;

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

    InGameObject getSelectedUnit();

    IClient getIClient();
}
