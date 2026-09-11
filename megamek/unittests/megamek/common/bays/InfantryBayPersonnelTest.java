/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.bays;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.units.PlatoonType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InfantryBayPersonnelTest {

    @ParameterizedTest
    @CsvSource({
          "FOOT, false, 30", "FOOT, true, 30",
          "JUMP, false, 30", "JUMP, true, 30",
          "MOTORIZED, false, 30", "MOTORIZED, true, 30",
          "MECHANIZED, false, 7", "MECHANIZED, true, 7"
    })
    void bayPersonnelUseTransportCapacityNotFactionPlatoonSize(PlatoonType type, boolean clan, int personnel) {
        assertEquals(personnel, new InfantryBay(1, 1, 1, type).getPersonnel(clan));
        assertEquals(3 * personnel, new InfantryBay(3, 1, 1, type).getPersonnel(clan));
        assertEquals(0, new InfantryBay(0, 1, 1, type).getPersonnel(clan));
    }
}
