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
package megamek.ai.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for WeaponDataEncoder.
 */
class WeaponDataEncoderTest {

    @Test
    void testGetEncodedWeaponDataSuccess() {
        Entity mockEntity = Mockito.mock(Entity.class);
        WeaponMounted mockWeapon = Mockito.mock(WeaponMounted.class);
        WeaponType mockType = Mockito.mock(WeaponType.class);
        Entity mockWeaponEntity = Mockito.mock(Entity.class);

        // Set up weapon and type
        Mockito.when(mockWeapon.getType()).thenReturn(mockType);
        Mockito.when(mockWeapon.getEntity()).thenReturn(mockWeaponEntity);

        // Setup type ranges
        Mockito.when(mockType.getShortRange()).thenReturn(3);
        Mockito.when(mockType.getMediumRange()).thenReturn(6);
        Mockito.when(mockType.getLongRange()).thenReturn(9);

        // Set up weapon entity equipment mapping
        Mockito.when(mockWeaponEntity.getEquipmentNum(mockWeapon)).thenReturn(0);
        Mockito.doReturn(mockWeapon).when(mockWeaponEntity).getEquipment(Mockito.anyInt());
        Mockito.when(mockWeaponEntity.getWeaponArc(0)).thenReturn(1);

        // Set up entity's weapon list
        Mockito.when(mockEntity.getWeaponListWithHHW()).thenReturn(List.of(mockWeapon));

        // Note: Compute.computeTotalDamage is static and might be hard to mock if it's not already mocked by the framework.
        // If Compute.computeTotalDamage(mockWeapon) returns a default value, we check for that.
        // Assuming it's a real call, we can't easily mock it without PowerMock or similar, which I should avoid.
        // Let's see if it works with real Compute.

        List<Integer> encoded = WeaponDataEncoder.getEncodedWeaponData(mockEntity);

        assertNotNull(encoded);
        assertEquals(5, encoded.size());
        // Since we can't easily mock static Compute, we check ranges and arc which are mocked.
        assertEquals(1, encoded.get(1)); // arc
        assertEquals(3, encoded.get(2)); // short
        assertEquals(6, encoded.get(3)); // medium
        assertEquals(9, encoded.get(4)); // long
    }

    @Test
    void testGetEncodedWeaponDataNullMounted() {
        Entity mockEntity = Mockito.mock(Entity.class);
        WeaponMounted mockWeapon = Mockito.mock(WeaponMounted.class);
        Entity mockWeaponEntity = Mockito.mock(Entity.class);

        Mockito.when(mockWeapon.getEntity()).thenReturn(mockWeaponEntity);
        Mockito.when(mockWeaponEntity.getEquipmentNum(mockWeapon)).thenReturn(0);
        // Return null for equipment
        Mockito.when(mockWeaponEntity.getEquipment(0)).thenReturn(null);

        Mockito.when(mockEntity.getWeaponListWithHHW()).thenReturn(List.of(mockWeapon));

        List<Integer> encoded = WeaponDataEncoder.getEncodedWeaponData(mockEntity);

        assertNotNull(encoded);
        assertTrue(encoded.isEmpty()); // Should return early and not add anything
    }

    @Test
    void testGetEncodedWeaponDataException() {
        Entity mockEntity = Mockito.mock(Entity.class);
        WeaponMounted mockWeapon = Mockito.mock(WeaponMounted.class);

        // Trigger an exception by having getEntity throw one
        Mockito.when(mockWeapon.getEntity()).thenThrow(new RuntimeException("Test Exception"));

        Mockito.when(mockEntity.getWeaponListWithHHW()).thenReturn(List.of(mockWeapon));

        List<Integer> encoded = WeaponDataEncoder.getEncodedWeaponData(mockEntity);

        assertNotNull(encoded);
        assertEquals(5, encoded.size());
        for (int val : encoded) {
            assertEquals(-1, val);
        }
    }
}
