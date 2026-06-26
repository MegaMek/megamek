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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PilotSPAHelper methods
 */
class PilotSPAHelperTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testWindWalkerValidForAerospaceFighter() {
        AeroSpaceFighter aero = new AeroSpaceFighter();
        aero.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(aero), "Wind Walker should be valid for Aerospace Fighters");
    }

    @Test
    void testWindWalkerValidForLandAirMek() {
        LandAirMek lam = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
        lam.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(lam), "Wind Walker should be valid for Land-Air Meks");
    }

    @Test
    void testWindWalkerValidForWiGEVehicle() {
        Tank wigeVehicle = new Tank();
        wigeVehicle.setMovementMode(EntityMovementMode.WIGE);
        wigeVehicle.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(wigeVehicle), "Wind Walker should be valid for WiGE vehicles");
    }

    @Test
    void testWindWalkerValidForGliderProtoMek() {
        ProtoMek gliderProto = new ProtoMek();
        gliderProto.setIsGlider(true);
        gliderProto.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(gliderProto), "Wind Walker should be valid for Glider ProtoMeks");
    }

    @Test
    void testWindWalkerInvalidForNonGliderProtoMek() {
        ProtoMek standardProto = new ProtoMek();
        standardProto.setIsGlider(false);
        standardProto.setCrew(new Crew(CrewType.SINGLE));

        assertFalse(PilotSPAHelper.isWindWalkerValid(standardProto),
              "Wind Walker should NOT be valid for non-Glider ProtoMeks");
    }

    @Test
    void testWindWalkerInvalidForBipedMek() {
        BipedMek mek = new BipedMek();
        mek.setCrew(new Crew(CrewType.SINGLE));

        assertFalse(PilotSPAHelper.isWindWalkerValid(mek), "Wind Walker should NOT be valid for Biped Meks");
    }

    @Test
    void testWindWalkerInvalidForTrackedVehicle() {
        Tank trackedVehicle = new Tank();
        trackedVehicle.setMovementMode(EntityMovementMode.TRACKED);
        trackedVehicle.setCrew(new Crew(CrewType.SINGLE));

        assertFalse(PilotSPAHelper.isWindWalkerValid(trackedVehicle),
              "Wind Walker should NOT be valid for Tracked vehicles");
    }

    @Test
    void testWindWalkerInvalidForHoverVehicle() {
        Tank hoverVehicle = new Tank();
        hoverVehicle.setMovementMode(EntityMovementMode.HOVER);
        hoverVehicle.setCrew(new Crew(CrewType.SINGLE));

        assertFalse(PilotSPAHelper.isWindWalkerValid(hoverVehicle),
              "Wind Walker should NOT be valid for hover vehicles");
    }

    @Test
    void testWindWalkerValidForVTOLVehicle() {
        VTOL vtol = new VTOL();
        vtol.setMovementMode(EntityMovementMode.VTOL);
        vtol.setCrew(new Crew(CrewType.SINGLE));

        // VTOLs are valid for WindWalker effects only if they are counted as airborne,
        // which only occurs when a VTOL using VTOL movement, as per
        // total warfare 2023 pg 20. VTOL are considered ground units when not using VTOL movement mode.
        // Therefore, Wind Walker, which requires airborne units would only be applicable when VTOL is using VTOL movement mode.

        assertTrue(PilotSPAHelper.isWindWalkerValid(vtol),
              "Wind Walker should be valid for VTOL vehicles when in VTOL movement,");
    }

    @Test
    void testWindWalkerInvalidForVTOLVehicle() {
        VTOL vtol = new VTOL();
        vtol.setCrew(new Crew(CrewType.SINGLE));

        // VTOLs are valid for WindWalker effects only if they are counted as airborne,
        // which only occurs when a VTOL using VTOL movement, as per 
        // total warfare 2023 pg 20. VTOL are considered ground units when not using VTOL movement mode.
        // Therefore, Wind Walker, which requires airborne units would only be applicable when VTOL is using VTOL movement mode.

        assertFalse(PilotSPAHelper.isWindWalkerValid(vtol),
              "Wind Walker should NOT be valid for VTOL vehicles when NOT in VTOL movement");
    }

    @Test
    void testWindWalkerValidForSmallCraft() {
        SmallCraft smallCraft = new SmallCraft();
        smallCraft.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(smallCraft), "Wind Walker should be valid for Small Craft");
    }

    @Test
    void testWindWalkerValidForConvFighter() {
        ConvFighter convFighter = new ConvFighter();
        convFighter.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(convFighter),
              "Wind Walker should be valid for Conventional Fighters");
    }

    @Test
    void testWindWalkerValidForFixedWingSupport() {
        FixedWingSupport fixedWing = new FixedWingSupport();
        fixedWing.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(fixedWing), "Wind Walker should be valid for Fixed Wing Support");
    }

    @Test
    void testWindWalkerValidForDropShip() {
        Dropship dropship = new Dropship();
        dropship.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(dropship), "Wind Walker should be valid for DropShips");
    }

    @Test
    void testWindWalkerValidForJumpShip() {
        Jumpship jumpship = new Jumpship();
        jumpship.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(jumpship), "Wind Walker should be valid for Jumpships");
    }

    @Test
    void testWindWalkerValidForWarShip() {
        Warship warship = new Warship();
        warship.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(warship), "Wind Walker should be valid for Warships");
    }

    @Test
    void testWindWalkerValidForSpaceStation() {
        SpaceStation spaceStation = new SpaceStation();
        spaceStation.setCrew(new Crew(CrewType.SINGLE));

        assertTrue(PilotSPAHelper.isWindWalkerValid(spaceStation), "Wind Walker should be valid for Space Stations");
    }
}
