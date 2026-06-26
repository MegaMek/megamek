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

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import megamek.utils.EntityLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Standard (TO:AR) ghost target bonus fields on Entity.
 */
class GhostTargetBonusTest {

    private Entity entity;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
    }

    @Test
    void testDefensiveBonusStartsAtZero() {
        assertEquals(0, entity.getGhostTargetDefensiveBonus());
    }

    @Test
    void testOffensiveBonusStartsAtZero() {
        assertEquals(0, entity.getGhostTargetOffensiveBonus());
    }

    @Test
    void testAddDefensiveBonus() {
        entity.addGhostTargetDefensiveBonus(1);
        assertEquals(1, entity.getGhostTargetDefensiveBonus());

        entity.addGhostTargetDefensiveBonus(1);
        assertEquals(2, entity.getGhostTargetDefensiveBonus());
    }

    @Test
    void testAddOffensiveBonus() {
        entity.addGhostTargetOffensiveBonus(1);
        assertEquals(1, entity.getGhostTargetOffensiveBonus());

        entity.addGhostTargetOffensiveBonus(1);
        assertEquals(2, entity.getGhostTargetOffensiveBonus());
    }

    @Test
    void testDefensiveBonusCappedAtThree() {
        entity.addGhostTargetDefensiveBonus(1);
        entity.addGhostTargetDefensiveBonus(1);
        entity.addGhostTargetDefensiveBonus(1);
        assertEquals(3, entity.getGhostTargetDefensiveBonus());

        // Should not exceed +3
        entity.addGhostTargetDefensiveBonus(1);
        assertEquals(3, entity.getGhostTargetDefensiveBonus());
    }

    @Test
    void testOffensiveBonusCappedAtThree() {
        entity.addGhostTargetOffensiveBonus(1);
        entity.addGhostTargetOffensiveBonus(1);
        entity.addGhostTargetOffensiveBonus(1);
        assertEquals(3, entity.getGhostTargetOffensiveBonus());

        // Should not exceed +3
        entity.addGhostTargetOffensiveBonus(1);
        assertEquals(3, entity.getGhostTargetOffensiveBonus());
    }

    @Test
    void testLargeIncrementCappedAtThree() {
        entity.addGhostTargetDefensiveBonus(5);
        assertEquals(3, entity.getGhostTargetDefensiveBonus());

        entity.addGhostTargetOffensiveBonus(10);
        assertEquals(3, entity.getGhostTargetOffensiveBonus());
    }

    @Test
    void testDefensiveAndOffensiveAreIndependent() {
        entity.addGhostTargetDefensiveBonus(2);
        entity.addGhostTargetOffensiveBonus(1);

        assertEquals(2, entity.getGhostTargetDefensiveBonus());
        assertEquals(1, entity.getGhostTargetOffensiveBonus());
    }
}
