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
package megamek.client.ui.dialogs.buttonDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** Unit ID entry in the Bot Configuration "Add Unit" box (issue #9173). */
class BotConfigTargetUnitDialogTest {

    @Test
    void rangeAddsEveryUnitFromStartToEnd() {
        assertEquals(Set.of(3, 4, 5, 6, 7), BotConfigTargetUnitDialog.parseUnitIds("3-7"));
    }

    @Test
    void spaceAfterCommaKeepsTheNextId() {
        assertEquals(Set.of(3, 4), BotConfigTargetUnitDialog.parseUnitIds("3, 4"));
    }

    @Test
    void spacesAroundRangeAreIgnored() {
        assertEquals(Set.of(2, 3, 4, 10), BotConfigTargetUnitDialog.parseUnitIds(" 2 - 4 , 10 "));
    }

    @Test
    void plainListStillWorks() {
        assertEquals(Set.of(3, 4, 8), BotConfigTargetUnitDialog.parseUnitIds("3,4,8"));
    }

    @Test
    void backwardsRangeAndTextAreSkipped() {
        assertEquals(Set.of(5), BotConfigTargetUnitDialog.parseUnitIds("7-3, abc, 5"));
    }

    @Test
    void emptyTextGivesNoIds() {
        assertTrue(BotConfigTargetUnitDialog.parseUnitIds("").isEmpty());
    }
}
