/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

import megamek.client.ui.util.UIUtil;

/**
 * This key dispatcher catches Ctrl-C / Ctrl-V keypresses in the lobby to allow copy/pasting units without being
 * obstructed by focus. If not done in this way, a table such as the player table will catch these key events before the
 * Menubar can get them. Thus, the Menubar accelerators for Unit Copy/Paste are only for show, they do not actually
 * catch these keys.
 *
 * @author Simon (Juliez)
 */
public class LobbyKeyDispatcher implements KeyEventDispatcher {

    private ChatLounge lobby;

    public LobbyKeyDispatcher(ChatLounge cl) {
        lobby = cl;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evt) {

        // Don't consider hotkeys when the clientgui has a dialog visible and only react to key presses (not release)
        if (((lobby.getClientgui() != null) && lobby.getClientgui().shouldIgnoreHotKeys())
              || lobby.isIgnoringEvents()
              || UIUtil.isModalDialogDisplayed()
              || (evt.getID() != KeyEvent.KEY_PRESSED)) {
            return false;
        }

        int keyCode = evt.getKeyCode();
        int modifiers = evt.getModifiersEx();

        if ((keyCode == KeyEvent.VK_V) && (modifiers == KeyEvent.CTRL_DOWN_MASK)) {
            lobby.importClipboard();
            return true;

        } else if ((keyCode == KeyEvent.VK_C) && (modifiers == KeyEvent.CTRL_DOWN_MASK)) {
            lobby.copyToClipboard();
            return true;
        }

        return false;
    }

}
