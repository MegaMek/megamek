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

import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for UnitEnrichment class and its serializer.
 */
class UnitEnrichmentTest {

    @Test
    void testFromEntity() {
        Entity mockEntity = Mockito.mock(Entity.class);
        Mockito.when(mockEntity.getChassis()).thenReturn("Locust");
        Mockito.when(mockEntity.getModel()).thenReturn("LCT-1V");
        Mockito.when(mockEntity.getRole()).thenReturn(megamek.common.units.UnitRole.SCOUT);
        Mockito.when(mockEntity.getInitialBV()).thenReturn(432);
        Mockito.when(mockEntity.getOriginalWalkMP()).thenReturn(8);
        Mockito.when(mockEntity.getOriginalRunMP()).thenReturn(12);
        Mockito.when(mockEntity.getOriginalJumpMP()).thenReturn(0);
        Mockito.when(mockEntity.getHeatCapacity()).thenReturn(10);
        Mockito.when(mockEntity.getTotalOArmor()).thenReturn(64);
        Mockito.when(mockEntity.getTotalOInternal()).thenReturn(40);
        Mockito.when(mockEntity.getHeight()).thenReturn(1); // will become 2 in UnitEnrichment
        Mockito.when(mockEntity.hasECM()).thenReturn(false);
        Mockito.when(mockEntity.getActiveAMS()).thenReturn(new java.util.ArrayList<>());
        Mockito.when(mockEntity.getMaxWeaponRange()).thenReturn(12);
        Mockito.when(mockEntity.getWeaponListWithHHW()).thenReturn(new java.util.ArrayList<>());
        Mockito.when(mockEntity.getWeaponList()).thenReturn(new java.util.ArrayList<>());

        UnitEnrichment data = UnitEnrichment.fromEntity(mockEntity);

        assertEquals("Locust", data.get(UnitEnrichment.Field.CHASSIS));
        assertEquals("LCT-1V", data.get(UnitEnrichment.Field.MODEL));
        assertEquals("SCOUT", data.get(UnitEnrichment.Field.ROLE));
        assertEquals(432, data.get(UnitEnrichment.Field.BV));
        assertEquals(8, data.get(UnitEnrichment.Field.WALK_MP));
        assertEquals(2, data.get(UnitEnrichment.Field.HEIGHT));
    }

    @Test
    void testSerializer() {
        UnitEnrichment data = new UnitEnrichment();
        data.put(UnitEnrichment.Field.CHASSIS, "Stinger")
              .put(UnitEnrichment.Field.BV, 350);

        UnitEnrichmentSerializer serializer = new UnitEnrichmentSerializer();
        String serialized = serializer.serialize(data);

        assertNotNull(serialized);
        assertTrue(serialized.contains("Stinger"));
        assertTrue(serialized.contains("350"));
    }
}
