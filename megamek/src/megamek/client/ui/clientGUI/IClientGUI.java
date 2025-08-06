/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI;

import javax.swing.JComponent;
import javax.swing.JFrame;

import megamek.client.IClient;

public interface IClientGUI {

    /**
     * @return The JFrame this client is displayed in.
     */
    JFrame getFrame();

    /**
     * Returns true if a dialog is visible on top of the ClientGUI. For example, the MegaMekController should ignore
     * hotkeys if there is a dialog, like the CommonSettingsDialog, open.
     *
     * @return True when hotkey events should not be forwarded to this ClientGUI
     */
    boolean shouldIgnoreHotKeys();

    /**
     * Registers this ClientGUI as a listener wherever it's needed. It is generally considered bad practice to do this
     * in the constructor.
     */
    void initialize();

    /**
     * Performs shut down for threads and sockets and other things that can be disposed.
     */
    void die();

    IClient getClient();

    JComponent turnTimerComponent();

    default boolean isChatBoxActive() {
        return false;
    }

    void setChatBoxActive(boolean active);

    void clearChatBox();
}
