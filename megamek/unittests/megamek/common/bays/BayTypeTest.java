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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import megamek.common.units.*;

class BayTypeTest {

    @Test
    void getTypeForEntityReturnsNullForNullEntity() {
        assertNull(BayType.getTypeForEntity(null));
    }

    @Test
    void getTypeForEntityReturnsFighterForAeroSpaceFighter() {
        AeroSpaceFighter entity = mock(AeroSpaceFighter.class);
        when(entity.isFighter()).thenReturn(true);
        assertEquals(BayType.FIGHTER, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsFighterForConvFighter() {
        ConvFighter entity = mock(ConvFighter.class);
        when(entity.isFighter()).thenReturn(true);
        assertEquals(BayType.FIGHTER, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsFighterForFixedWingSupport() {
        FixedWingSupport entity = mock(FixedWingSupport.class);
        when(entity.isFighter()).thenReturn(true);
        when(entity.getWeight()).thenReturn(50.0);
        assertEquals(BayType.FIGHTER, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsSmallCraftForHeavyFixedWingSupport() {
        FixedWingSupport entity = mock(FixedWingSupport.class);
        when(entity.isFighter()).thenReturn(true);
        when(entity.hasETypeFlag(Entity.ETYPE_FIXED_WING_SUPPORT)).thenReturn(true);
        when(entity.hasETypeFlag(Entity.ETYPE_AERO)).thenReturn(true);
        when(entity.getWeight()).thenReturn(200.0);
        assertEquals(BayType.SMALL_CRAFT, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsMekForBipedMek() {
        BipedMek entity = mock(BipedMek.class);
        when(entity.hasETypeFlag(Entity.ETYPE_MEK)).thenReturn(true);
        assertEquals(BayType.MEK, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsMekForQuadMek() {
        QuadMek entity = mock(QuadMek.class);
        when(entity.hasETypeFlag(Entity.ETYPE_MEK)).thenReturn(true);
        assertEquals(BayType.MEK, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsMekForLandAirMek() {
        LandAirMek entity = mock(LandAirMek.class);
        when(entity.hasETypeFlag(Entity.ETYPE_MEK)).thenReturn(true);
        assertEquals(BayType.MEK, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsProtoMek() {
        ProtoMek entity = mock(ProtoMek.class);
        when(entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)).thenReturn(true);
        assertEquals(BayType.PROTOMEK, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsSmallCraft() {
        SmallCraft entity = mock(SmallCraft.class);
        when(entity.hasETypeFlag(Entity.ETYPE_AERO)).thenReturn(true);
        when(entity.getWeight()).thenReturn(100.0);
        assertEquals(BayType.SMALL_CRAFT, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsLightVehicle() {
        Tank entity = mock(Tank.class);
        when(entity.hasETypeFlag(Entity.ETYPE_TANK)).thenReturn(true);
        when(entity.getWeight()).thenReturn(30.0);
        assertEquals(BayType.VEHICLE_LIGHT, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsHeavyVehicle() {
        Tank entity = mock(Tank.class);
        when(entity.hasETypeFlag(Entity.ETYPE_TANK)).thenReturn(true);
        when(entity.getWeight()).thenReturn(75.0);
        assertEquals(BayType.VEHICLE_HEAVY, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsSuperHeavyVehicle() {
        SuperHeavyTank entity = mock(SuperHeavyTank.class);
        when(entity.hasETypeFlag(Entity.ETYPE_TANK)).thenReturn(true);
        when(entity.getWeight()).thenReturn(150.0);
        assertEquals(BayType.VEHICLE_SH, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsInfantryFoot() {
        Infantry entity = mock(Infantry.class);
        when(entity.hasETypeFlag(Entity.ETYPE_INFANTRY)).thenReturn(true);
        when(entity.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)).thenReturn(false);
        when(entity.getMovementMode()).thenReturn(EntityMovementMode.INF_LEG);
        assertEquals(BayType.INFANTRY_FOOT, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsInfantryJump() {
        Infantry entity = mock(Infantry.class);
        when(entity.hasETypeFlag(Entity.ETYPE_INFANTRY)).thenReturn(true);
        when(entity.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)).thenReturn(false);
        when(entity.getMovementMode()).thenReturn(EntityMovementMode.INF_JUMP);
        assertEquals(BayType.INFANTRY_JUMP, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsNullForDropShip() {
        Dropship entity = mock(Dropship.class);
        when(entity.hasETypeFlag(Entity.ETYPE_AERO)).thenReturn(true);
        when(entity.getWeight()).thenReturn(3500.0);
        assertNull(BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsNullForJumpShip() {
        Jumpship entity = mock(Jumpship.class);
        assertNull(BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsNullForWarShip() {
        Warship entity = mock(Warship.class);
        assertNull(BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsNullForSpaceStation() {
        SpaceStation entity = mock(SpaceStation.class);
        assertNull(BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsNullForGunEmplacement() {
        Entity entity = mock(Entity.class);
        when(entity.isBuildingEntityOrGunEmplacement()).thenReturn(true);
        when(entity.hasETypeFlag(Entity.ETYPE_TANK)).thenReturn(true);
        when(entity.getWeight()).thenReturn(30.0);
        assertNull(BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsVehicleForVTOL() {
        VTOL entity = mock(VTOL.class);
        when(entity.hasETypeFlag(Entity.ETYPE_TANK)).thenReturn(true);
        when(entity.getWeight()).thenReturn(30.0);
        assertEquals(BayType.VEHICLE_LIGHT, BayType.getTypeForEntity(entity));
    }

    @Test
    void getTypeForEntityReturnsFighterForFighterSquadron() {
        FighterSquadron entity = mock(FighterSquadron.class);
        when(entity.isFighter()).thenReturn(true);
        assertEquals(BayType.FIGHTER, BayType.getTypeForEntity(entity));
    }
}
