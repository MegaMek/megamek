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
package megamek.client.ui.clientGUI.boardview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LOSHeightCalculation Tests")
class LOSHeightCalculationTest {

    @Test
    @DisplayName("Standard Mek should have TW height of 2")
    void standardMekHeight() {
        BipedMek mek = mock(BipedMek.class);
        when(mek.relHeight()).thenReturn(1);
        when(mek.isHullDown()).thenReturn(false);

        assertEquals(2, LOSHeightCalculation.twHeightFromEntity(mek));
    }

    @Test
    @DisplayName("Hull-down Mek should have TW height of 1")
    void hullDownMekHeight() {
        BipedMek mek = mock(BipedMek.class);
        when(mek.relHeight()).thenReturn(1);
        when(mek.isHullDown()).thenReturn(true);

        assertEquals(1, LOSHeightCalculation.twHeightFromEntity(mek));
    }

    @Test
    @DisplayName("Vehicle should have TW height of 1")
    void vehicleHeight() {
        Tank tank = mock(Tank.class);
        when(tank.relHeight()).thenReturn(0);
        when(tank.isHullDown()).thenReturn(false);
        when(tank.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);

        assertEquals(1, LOSHeightCalculation.twHeightFromEntity(tank));
    }

    @Test
    @DisplayName("Superheavy Mek should have TW height of 3")
    void superheavyMekHeight() {
        BipedMek mek = mock(BipedMek.class);
        when(mek.relHeight()).thenReturn(2);
        when(mek.isHullDown()).thenReturn(false);

        assertEquals(3, LOSHeightCalculation.twHeightFromEntity(mek));
    }

    @Test
    @DisplayName("Hull-down superheavy Mek should have TW height of 2")
    void hullDownSuperheavyMekHeight() {
        BipedMek mek = mock(BipedMek.class);
        when(mek.relHeight()).thenReturn(2);
        when(mek.isHullDown()).thenReturn(true);

        assertEquals(2, LOSHeightCalculation.twHeightFromEntity(mek));
    }

    @Test
    @DisplayName("Entity at elevated position should have correct TW height")
    void elevatedEntityHeight() {
        // A VTOL at elevation 5 with relHeight() = 5
        Entity entity = mock(Entity.class);
        when(entity.relHeight()).thenReturn(5);
        when(entity.isHullDown()).thenReturn(false);

        assertEquals(6, LOSHeightCalculation.twHeightFromEntity(entity));
    }

    @Test
    @DisplayName("toAbsoluteHeight converts TW height + hex level correctly")
    void toAbsoluteHeightBasic() {
        // Mek (TW=2) on level 3 hex: abs = (2-1) + 3 = 4
        assertEquals(4, LOSHeightCalculation.toAbsoluteHeight(2, 3));
    }

    @Test
    @DisplayName("toAbsoluteHeight with TW height of 1 on level 0")
    void toAbsoluteHeightMinimal() {
        // Vehicle (TW=1) on level 0 hex: abs = (1-1) + 0 = 0
        assertEquals(0, LOSHeightCalculation.toAbsoluteHeight(1, 0));
    }

    @Test
    @DisplayName("toAbsoluteHeight with negative hex level")
    void toAbsoluteHeightNegativeLevel() {
        // TW=1 on level -2 hex: abs = (1-1) + (-2) = -2
        assertEquals(-2, LOSHeightCalculation.toAbsoluteHeight(1, -2));
    }

    @Test
    @DisplayName("toAbsoluteHeight with superheavy Mek on hill")
    void toAbsoluteHeightSuperheavyOnHill() {
        // Superheavy (TW=3) on level 5 hex: abs = (3-1) + 5 = 7
        assertEquals(7, LOSHeightCalculation.toAbsoluteHeight(3, 5));
    }

    @Test
    @DisplayName("Airborne aero should use altitude, not elevation-derived height")
    void airborneAeroUsesAltitude() {
        AeroSpaceFighter aero = mock(AeroSpaceFighter.class);
        when(aero.getAltitude()).thenReturn(10);
        when(aero.relHeight()).thenReturn(999);
        when(aero.getMovementMode()).thenReturn(EntityMovementMode.AERODYNE);

        assertEquals(10, LOSHeightCalculation.twHeightFromEntity(aero));
    }

    @Test
    @DisplayName("Landed aero should use normal height calculation")
    void landedAeroUsesHeight() {
        AeroSpaceFighter aero = mock(AeroSpaceFighter.class);
        when(aero.getAltitude()).thenReturn(0);
        when(aero.relHeight()).thenReturn(0);
        when(aero.getMovementMode()).thenReturn(EntityMovementMode.AERODYNE);

        assertEquals(1, LOSHeightCalculation.twHeightFromEntity(aero));
    }

    @Test
    @DisplayName("Grounded dropship should use physical height, not altitude")
    void groundedDropshipUsesHeight() {
        // A landed spheroid dropship is 10 levels tall (relHeight=9, TW height=10).
        // It should show as Height: 10, NOT Altitude: 10.
        Dropship dropship = mock(Dropship.class);
        when(dropship.getAltitude()).thenReturn(0);
        when(dropship.relHeight()).thenReturn(9);
        when(dropship.isAirborne()).thenReturn(true);
        when(dropship.getMovementMode()).thenReturn(EntityMovementMode.SPHEROID);

        assertEquals(10, LOSHeightCalculation.twHeightFromEntity(dropship));
    }

    @Test
    @DisplayName("toAbsoluteHeight for altitude unit ignores hex level")
    void toAbsoluteHeightAltitudeUnit() {
        // Altitude 10, hex level 5 -> abs = 10 (altitude is absolute)
        assertEquals(10, LOSHeightCalculation.toAbsoluteHeight(10, 5, true));
    }

    @Test
    @DisplayName("toAbsoluteHeight for ground unit uses hex level")
    void toAbsoluteHeightGroundUnit() {
        // TW=2, hex level 5 -> abs = (2-1) + 5 = 6
        assertEquals(6, LOSHeightCalculation.toAbsoluteHeight(2, 5, false));
    }
}
