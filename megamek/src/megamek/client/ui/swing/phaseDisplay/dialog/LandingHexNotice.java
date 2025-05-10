/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.phaseDisplay.dialog;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;

/**
 * @see #LandingHexNotice(ClientGUI)
 */
public class LandingHexNotice extends SimpleNagNotice {

    /**
     * Creates a dialog telling the player to select a touch down hex for a horizontal or vertical landing from
     * an atmospheric board (when the aero-on-ground movement option is not used).
     */
    public LandingHexNotice(ClientGUI clientGui) {
        super(clientGui);
    }

    protected String message() {
        return Messages.getString("LandingHexNotice.message");
    }

    protected String title() {
        return Messages.getString("LandingHexNotice.title");
    }

    protected String preferenceKey() {
        return "ShowLandingHexNotice";
    }
}
