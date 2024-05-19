/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.event.*;

public class SBFClientGUIGameListener extends GameListenerAdapter {

    private final SBFClientGUI clientGUI;

    public SBFClientGUIGameListener(SBFClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
//        // This is a really lame place for this, but I couldn't find a
//        // better one without making massive changes (which didn't seem
//        // worth it for one little feature).
//        if (bv.getLocalPlayer() != client.getLocalPlayer()) {
//            // The adress based comparison is somewhat important.
//            // Use of the /reset command can cause the player to get reset,
//            // and the equals function of Player isn't powerful enough.
//            bv.setLocalPlayer(client.getLocalPlayer());
//        }
//        // Make sure the ChatterBox starts out deactived.
//        bv.setChatterBoxActive(false);
//
        // Swap to this phase's panel.
        clientGUI.switchPanel(e.getNewPhase());
//        clientGUI.menuBar.setPhase(phase);
//        clientGUI.cb.moveToEnd();
    }
}
