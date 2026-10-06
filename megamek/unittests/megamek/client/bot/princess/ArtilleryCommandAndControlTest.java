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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.client.bot.princess.ArtilleryCommandAndControl.ArtilleryOrder;
import megamek.client.bot.princess.ArtilleryCommandAndControl.SpecialAmmo;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the artillery order state used by the artillery chat command (auto/halt/barrage/volley/single).
 *
 * @author HammerGS
 */
class ArtilleryCommandAndControlTest {

    private ArtilleryCommandAndControl artilleryCommandAndControl;
    private Entity mockShooter;

    @BeforeEach
    void beforeEach() {
        artilleryCommandAndControl = new ArtilleryCommandAndControl();
        mockShooter = mock(Entity.class);
        when(mockShooter.getId()).thenReturn(42);
    }

    @Test
    void testDefaultOrderIsAutoWithNoTargets() {
        assertFalse(artilleryCommandAndControl.isArtilleryHalted());
        assertFalse(artilleryCommandAndControl.isArtilleryBarrage());
        assertFalse(artilleryCommandAndControl.isArtilleryVolley());
        assertFalse(artilleryCommandAndControl.isArtillerySingle());
        assertTrue(artilleryCommandAndControl.getArtilleryTargets().isEmpty());
    }

    @Test
    void testBarrageOrderKeepsTargets() {
        artilleryCommandAndControl.addArtilleryTargets(List.of(new Coords(7, 7), new Coords(7, 9)));
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.BARRAGE, SpecialAmmo.STANDARD);

        assertTrue(artilleryCommandAndControl.isArtilleryBarrage());
        assertTrue(artilleryCommandAndControl.contains(new Coords(7, 7)));
        assertTrue(artilleryCommandAndControl.contains(new Coords(7, 9)));
        assertFalse(artilleryCommandAndControl.contains(new Coords(1, 1)));
    }

    @Test
    void testVolleyShooterTracking() {
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.VOLLEY, SpecialAmmo.STANDARD);

        assertFalse(artilleryCommandAndControl.hasAlreadyFired(mockShooter),
              "A unit that has not been marked must not count as having fired");
        artilleryCommandAndControl.setShooter(mockShooter);
        assertTrue(artilleryCommandAndControl.hasAlreadyFired(mockShooter),
              "A marked unit must count as having fired this volley");
    }

    @Test
    void testNewOrderClearsVolleyShooters() {
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.VOLLEY, SpecialAmmo.STANDARD);
        artilleryCommandAndControl.setShooter(mockShooter);

        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.VOLLEY, SpecialAmmo.STANDARD);
        assertFalse(artilleryCommandAndControl.hasAlreadyFired(mockShooter),
              "Issuing a new order must reset the volley shooter tracking");
    }

    @Test
    void testResetRestoresAutoOrder() {
        artilleryCommandAndControl.addArtilleryTargets(List.of(new Coords(7, 7)));
        artilleryCommandAndControl.setArtilleryOrder(ArtilleryOrder.HALT, SpecialAmmo.SMOKE);

        artilleryCommandAndControl.reset();

        assertFalse(artilleryCommandAndControl.isArtilleryHalted());
        assertEquals(SpecialAmmo.STANDARD, artilleryCommandAndControl.getAmmo());
        assertTrue(artilleryCommandAndControl.getArtilleryTargets().isEmpty());
    }
}
