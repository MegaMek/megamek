/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import megamek.common.AeroSpaceFighter;
import megamek.common.EquipmentType;

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
