/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.phaseDisplay;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;

/**
 * @see #LandingHexNotice(ClientGUI)
 */
public class LandingHexNotice extends SimpleNagNotice {

    /**
     * Creates a dialog telling the player to select a touch-down hex for a horizontal or vertical landing from an
     * atmospheric board (when the aero-on-ground movement option is not used).
     */
    public LandingHexNotice(ClientGUI clientGui) {
        super(clientGui);
    }

    @Override
    protected String message() {
        return Messages.getString("LandingHexNotice.message");
    }

    @Override
    protected String title() {
        return Messages.getString("LandingHexNotice.title");
    }

    @Override
    protected String preferenceKey() {
        return "ShowLandingHexNotice";
    }
}
