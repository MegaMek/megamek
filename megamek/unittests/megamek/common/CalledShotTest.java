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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CalledShotTest {

    @Test
    void newCalledShotStartsAtNone() {
        CalledShot calledShot = new CalledShot();

        assertEquals(CalledShot.CALLED_NONE, calledShot.getCall());
    }

    @Test
    void switchCalledShotCyclesThroughEveryLocationAndBackToNone() {
        CalledShot calledShot = new CalledShot();

        assertEquals(CalledShot.CALLED_HIGH, calledShot.switchCalledShot());
        assertEquals(CalledShot.CALLED_LOW, calledShot.switchCalledShot());
        assertEquals(CalledShot.CALLED_LEFT, calledShot.switchCalledShot());
        assertEquals(CalledShot.CALLED_RIGHT, calledShot.switchCalledShot());
        assertEquals(CalledShot.CALLED_NONE, calledShot.switchCalledShot());
    }

    @Test
    void setCallReachesAnyLocationInOneStep() {
        CalledShot calledShot = new CalledShot();

        // The keybinds set a location directly rather than stepping to it, so every location must be
        // reachable from every other one with a single call.
        calledShot.setCall(CalledShot.CALLED_RIGHT);
        assertEquals(CalledShot.CALLED_RIGHT, calledShot.getCall());

        calledShot.setCall(CalledShot.CALLED_HIGH);
        assertEquals(CalledShot.CALLED_HIGH, calledShot.getCall());

        calledShot.setCall(CalledShot.CALLED_NONE);
        assertEquals(CalledShot.CALLED_NONE, calledShot.getCall());
    }

    @Test
    void setCallIgnoresValuesOutsideTheCalledRange() {
        CalledShot calledShot = new CalledShot();
        calledShot.setCall(CalledShot.CALLED_LOW);

        // A malformed packet must not be able to leave the called shot in an unknown state.
        calledShot.setCall(CalledShot.CALLED_NUM);
        assertEquals(CalledShot.CALLED_LOW, calledShot.getCall());

        calledShot.setCall(-1);
        assertEquals(CalledShot.CALLED_LOW, calledShot.getCall());

        calledShot.setCall(Integer.MAX_VALUE);
        assertEquals(CalledShot.CALLED_LOW, calledShot.getCall());
    }

    @Test
    void resetClearsAnActiveCalledShot() {
        CalledShot calledShot = new CalledShot();
        calledShot.setCall(CalledShot.CALLED_LEFT);

        calledShot.reset();

        assertEquals(CalledShot.CALLED_NONE, calledShot.getCall());
    }

    @Test
    void displayableNameMatchesTheSelectedLocation() {
        CalledShot calledShot = new CalledShot();

        assertEquals("", calledShot.getDisplayableName());

        calledShot.setCall(CalledShot.CALLED_HIGH);
        assertEquals("HIGH", calledShot.getDisplayableName());

        calledShot.setCall(CalledShot.CALLED_RIGHT);
        assertEquals("RIGHT", calledShot.getDisplayableName());
    }
}
