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
package megamek.common.rules.totalwarfare;

import megamek.common.rules.RulesArtillery;
import megamek.common.rules.core.CoreRulesArtillery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TWRulesArtilleryTest {

    RulesArtillery rules = new TWRulesArtillery();

    @BeforeEach
    void setUp() {
    }

    @ParameterizedTest
    @CsvSource({ "1, true, false, 4", "17, true, false, 4", "18, true, false, 4",   // Direct fire
                 "17, false, false, 4", "18, false, false, 7",                      // Indirect fire
                 "10, true, true, 3"                                                // Flak
    })
    void computeArtilleryBaseMod(int distance, boolean direct, boolean flak, int expected) {
        assertEquals(expected, rules.computeArtilleryBaseMod(distance, direct, flak));
    }
}
