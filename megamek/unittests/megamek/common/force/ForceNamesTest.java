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
package megamek.common.force;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ForceNamesTest {

    @Test
    void eachSideIsOfferedNamesInItsOwnStyle() {
        assertTrue(ForceNames.suggestions(ForceNames.Style.INNER_SPHERE).contains("Charlie Lance"));
        assertTrue(ForceNames.suggestions(ForceNames.Style.CLAN).contains("Gamma Star"));
        assertEquals(ForceNames.Style.CLAN, ForceNames.styleFor("Clan Wolf", List.of()));
        assertEquals(ForceNames.Style.COMSTAR, ForceNames.styleFor("Word of Blake", List.of()));
        assertEquals(ForceNames.Style.INNER_SPHERE, ForceNames.styleFor("Lyran Allies", List.of()));
    }

    @Test
    void aNameThatSaysNothingIsGeneric() {
        assertTrue(ForceNames.isGeneric("", "Princess"));
        assertTrue(ForceNames.isGeneric("Force", "Princess"));
        assertTrue(ForceNames.isGeneric("Force 2", "Princess"));
        assertTrue(ForceNames.isGeneric("princess", "Princess"));
        assertFalse(ForceNames.isGeneric("Command Lance", "Princess"));
    }

    @Test
    void forcesWithNoNameAreCalledInOrder() {
        assertEquals("Alpha Lance", ForceNames.callsignName(0, ForceNames.Style.INNER_SPHERE));
        assertEquals("Charlie Lance", ForceNames.callsignName(2, ForceNames.Style.INNER_SPHERE));
        assertEquals("Gamma Star", ForceNames.callsignName(2, ForceNames.Style.CLAN));
        assertEquals("Level II Gamma", ForceNames.callsignName(2, ForceNames.Style.COMSTAR));
    }
}
