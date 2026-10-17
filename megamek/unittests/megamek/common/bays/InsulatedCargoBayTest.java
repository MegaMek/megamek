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
package megamek.common.bays;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * An insulated cargo bay starts with every door working, like every other bay. Before, it always started with one
 * working door, so a custom DropShip with a two-door insulated bay arrived in MekHQ with a damaged door.
 */
class InsulatedCargoBayTest {
    private static final double CARGO_TONS = 1350.0;
    private static final int BAY_NUMBER = 5;

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    void everyDoorWorksOnANewBay(int doors) {
        InsulatedCargoBay bay = new InsulatedCargoBay(CARGO_TONS, doors, BAY_NUMBER);

        assertEquals(doors, bay.getDoors());
        assertEquals(doors, bay.getCurrentDoors(), "Every door of a new insulated cargo bay works");
    }
}
