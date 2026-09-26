/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.phaseDisplay.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.client.ui.panels.phaseDisplay.commands.MoveCommand;
import org.junit.jupiter.api.Test;

class MoveCommandTest {

    @Test
    void testBaseMethods() {
        MoveCommand moveCommand = MoveCommand.MOVE_NEXT;
        assertEquals("moveNext", moveCommand.getCmd());
        assertEquals(0, moveCommand.getPriority());

        moveCommand.setPriority(10);
        assertEquals(10, moveCommand.getPriority());
        assertEquals("Next Unit", moveCommand.toString());
    }

    @Test
    void testMoveNextHotKeyString() {
        assertEquals("<BR>&nbsp;&nbsp;Next: Tab&nbsp;&nbsp;Previous: Shift+Tab", MoveCommand.MOVE_NEXT.getHotKeyDesc());
    }

    @Test
    void testMoveWalkHotKeyString() {
        assertEquals("<BR>&nbsp;&nbsp;Toggle Move / Jump: J", MoveCommand.MOVE_WALK.getHotKeyDesc());
    }

    @Test
    void testMoveRunHotKeyString() {
        assertEquals("<BR>&nbsp;&nbsp;Toggle Move / Jump: J", MoveCommand.MOVE_WALK.getHotKeyDesc());
    }

    @Test
    void testMoveTurnHotKeyString() {
        assertEquals("<BR>&nbsp;&nbsp;Left: Shift+A&nbsp;&nbsp;Right: Shift+D", MoveCommand.MOVE_TURN.getHotKeyDesc());
    }

    @Test
    void testMoveModeAirHotKeyString() {
        assertEquals("<BR>&nbsp;&nbsp;Toggle Mode: M", MoveCommand.MOVE_MODE_AIR.getHotKeyDesc());
    }

    @Test
    void testMoveModeConvertHotKeyString() {
        assertEquals("<BR>&nbsp;&nbsp;Toggle Mode: M", MoveCommand.MOVE_MODE_CONVERT.getHotKeyDesc());
    }

    @Test
    void testMoveModeLegHotKeyString() {
        assertEquals("<BR>&nbsp;&nbsp;Toggle Mode: M", MoveCommand.MOVE_MODE_LEG.getHotKeyDesc());
    }

    @Test
    void testMoveModeVeeHotKeyString() {
        assertEquals("<BR>&nbsp;&nbsp;Toggle Mode: M", MoveCommand.MOVE_MODE_VEE.getHotKeyDesc());
    }

    @Test
    void testDefaultHotKeyString() {
        assertEquals("<BR>U", MoveCommand.MOVE_GET_UP.getHotKeyDesc());
    }

    /**
     * Issue #9046: a LAM in AirMek mode that crashes lands prone and must be able to get up (IO:AE p.108), so the
     * AirMek button set has to include Get Up.
     */
    @Test
    void airMekButtonsIncludeGetUp() {
        int airMekFlag = MovementDisplay.CMD_TANK | MovementDisplay.CMD_CONVERTER | MovementDisplay.CMD_AIR_MEK;

        assertTrue(List.of(MoveCommand.values(airMekFlag, null, false)).contains(MoveCommand.MOVE_GET_UP));
    }

    /** The control: an ordinary vehicle still has no Get Up button. */
    @Test
    void vehicleButtonsDoNotIncludeGetUp() {
        assertFalse(List.of(MoveCommand.values(MovementDisplay.CMD_TANK, null, false))
              .contains(MoveCommand.MOVE_GET_UP));
    }
}
