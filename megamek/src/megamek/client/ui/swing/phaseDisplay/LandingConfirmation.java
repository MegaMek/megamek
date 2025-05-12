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
package megamek.client.ui.swing.phaseDisplay;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;

public class LandingConfirmation extends SimpleConfirmDialog {

    /**
     * Creates a dialog asking the player if a planned landing should be performed, ending the movement phase for
     * this unit directly.
     */
    protected LandingConfirmation(ClientGUI clientGui) {
        super(clientGui);
    }

    @Override
    protected String message() {
        return Messages.getString("LandingConfirmation.message");
    }

    @Override
    protected String title() {
        return Messages.getString("LandingConfirmation.title");
    }
}
