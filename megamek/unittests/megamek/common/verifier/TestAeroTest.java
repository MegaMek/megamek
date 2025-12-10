/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.units.AeroSpaceFighter;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestAeroTest {

    private AeroSpaceFighter aero;

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        aero = new AeroSpaceFighter();
    }

    @Test
    void testCalculateEngineRatingForAeroSpaceFighter() {
        int rating;

        // 25-ton ASF desiring Safe Thrust of 2 should be 0
        rating = TestAero.calculateEngineRating(aero, 25, 2);
        assertEquals(0, rating);

        // 25-ton ASF desiring Safe Thrust of 5 should be 75 (tons * (desiredSafeThrust
        // -2))
        rating = TestAero.calculateEngineRating(aero, 25, 5);
        assertEquals(75, rating);

        // 50-ton ASF desiring Safe Thrust of 5 should be 150
        rating = TestAero.calculateEngineRating(aero, 50, 5);
        assertEquals(150, rating);

        // 100-ton ASF desiring Safe Thrust of 6 should be 400
        rating = TestAero.calculateEngineRating(aero, 100, 6);
        assertEquals(400, rating);
    }
}
