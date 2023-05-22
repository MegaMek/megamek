/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.lobby;

import megamek.client.ui.swing.util.UIUtil;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

/** 
 * This key dispatcher catches Ctrl-C / Ctrl-V keypresses in the lobby to allow copy/pasting units
 * without being obstructed by focus. If not done in this way, a table such as the player table will
 * catch these key events before the Menubar can get them.
 * Thus, the Menubar accelerators for Unit Copy/Paste are only for show, they do not actually catch these keys.
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
