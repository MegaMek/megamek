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
package megamek.client.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.List;

import org.junit.jupiter.api.Test;

class KeyCommandBindTest {
    private static final List<KeyCommandBind> CAMERA_BINDS = List.of(KeyCommandBind.CAMERA_ROTATE_LEFT,
          KeyCommandBind.CAMERA_ROTATE_RIGHT, KeyCommandBind.CAMERA_TILT_UP, KeyCommandBind.CAMERA_TILT_DOWN,
          KeyCommandBind.CAMERA_RESET, KeyCommandBind.CAMERA_FIT_BOARD);

    @Test
    void theFullLookupFindsMenuBarBindsThatTheDispatcherLookupSkips() {
        KeyCommandBind zoomIn = KeyCommandBind.ZOOM_IN;
        assertTrue(zoomIn.isMenuBar);
        assertFalse(KeyCommandBind.getBindByKey(zoomIn.keyDefault, zoomIn.modifiersDefault).contains(zoomIn));
        assertTrue(KeyCommandBind.getAllBindsByKey(zoomIn.keyDefault, zoomIn.modifiersDefault).contains(zoomIn));
    }

    @Test
    void theFullLookupMatchesTheModifiersToo() {
        List<KeyCommandBind> plainW = KeyCommandBind.getAllBindsByKey(KeyEvent.VK_W, 0);
        assertTrue(plainW.contains(KeyCommandBind.SCROLL_NORTH));
        assertFalse(plainW.contains(KeyCommandBind.MOVE_STEP_FORWARD));
    }

    @Test
    void cameraBindDefaultsDoNotTakeAKeyThatIsAlreadyInUse() {
        for (KeyCommandBind cameraBind : CAMERA_BINDS) {
            for (KeyCommandBind other : KeyCommandBind.values()) {
                boolean sameKey = (other.keyDefault == cameraBind.keyDefault)
                      && (other.modifiersDefault == cameraBind.modifiersDefault);
                assertEquals(other == cameraBind, sameKey,
                      cameraBind + " and " + other + " must not share a default key");
            }
        }
    }

    @Test
    void cameraBindsReachTheKeyDispatcherLookup() {
        // A menu bar bind would be skipped by the settings dialog's duplicate handling and by the dispatcher.
        for (KeyCommandBind cameraBind : CAMERA_BINDS) {
            assertFalse(cameraBind.isMenuBar, cameraBind + " must be an ordinary bind");
        }
    }
}
