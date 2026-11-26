/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.settings;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Test helper for hovering settings controls from tests in other packages. Hover help is only shown after the pointer
 * settles, which a test has no real pointer for, so this fires the hover and then commits the pending help at once.
 */
public final class SettingsHoverTestSupport {
    private SettingsHoverTestSupport() {
    }

    /** Fires a mouse-entered event on {@code component} and shows the hover help it requested straight away. */
    public static void hoverAndSettle(Component component) {
        MouseEvent event = new MouseEvent(component, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 0, 0, 0,
              false);
        for (MouseListener listener : component.getMouseListeners()) {
            listener.mouseEntered(event);
        }
        SettingsContentHost host = SettingsContentHost.findHost(component);
        if (host != null) {
            host.commitPendingHoverHelp(false);
        }
    }
}
