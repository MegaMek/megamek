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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Construction-rule tests for vehicle bulldozers (TM): a unit may mount at most two bulldozer blades, no more than one
 * per location, and a location holding a bulldozer may not also hold a backhoe, chainsaw, combine, dual saw, heavy-duty
 * pile driver, mining drill, rock cutter or wrecking ball.
 */
class BulldozerConstructionTest {

    @BeforeAll
    static void initEquipment() {
        EquipmentType.initializeTypes();
    }

    private static TestTank testTankFor(Tank tank) {
        EntityVerifier verifier = EntityVerifier.getInstance(
              new File("testresources/data/mekfiles/UnitVerifierOptions.xml"));
        return new TestTank(tank, verifier.tankOption, null);
    }

    private static String bulldozerViolations(Tank tank) {
        StringBuffer buffer = new StringBuffer();
        testTankFor(tank).hasIllegalEquipmentCombinations(buffer);
        return buffer.toString();
    }

    private Tank trackedTank() {
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.TRACKED);
        return tank;
    }

    @Test
    @DisplayName("One bulldozer on the front and one on the rear is legal")
    void oneFrontOneRearIsLegal() throws LocationFullException {
        Tank tank = trackedTank();
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_FRONT);
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_REAR);

        String violations = bulldozerViolations(tank);
        assertFalse(violations.toLowerCase().contains("bulldozer"),
              "One bulldozer per Front/Rear should raise no bulldozer construction error, but got: " + violations);
    }

    @Test
    @DisplayName("Three bulldozers exceed the per-unit maximum of two")
    void threeBulldozersExceedMaximum() throws LocationFullException {
        Tank tank = trackedTank();
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_FRONT);
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_REAR);
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_REAR);

        assertTrue(bulldozerViolations(tank).contains("at most"),
              "A third bulldozer blade should be rejected by the per-unit maximum");
    }

    @Test
    @DisplayName("Two bulldozers in the same location are illegal")
    void twoInSameLocationIllegal() throws LocationFullException {
        Tank tank = trackedTank();
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_FRONT);
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_FRONT);

        assertTrue(bulldozerViolations(tank).contains("Only one bulldozer"),
              "Two bulldozers in the same location should be rejected");
    }

    @Test
    @DisplayName("Bulldozer mount location is detected (front vs rear)")
    void bulldozerMountLocationDetected() throws LocationFullException {
        Tank frontTank = trackedTank();
        frontTank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_FRONT);
        assertTrue(frontTank.hasFrontMountedBulldozer(), "Front-mounted bulldozer should be detected at the front");
        assertFalse(frontTank.hasRearMountedBulldozer(), "Front-mounted bulldozer is not rear-mounted");

        Tank rearTank = trackedTank();
        rearTank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_REAR);
        assertTrue(rearTank.hasRearMountedBulldozer(), "Rear-mounted bulldozer (Reverse Buffel) should be detected");
        assertFalse(rearTank.hasFrontMountedBulldozer(), "Rear-mounted bulldozer is not front-mounted");
    }

    @Test
    @DisplayName("A bulldozer may not share a location with a backhoe")
    void bulldozerWithBackhoeSameLocationIllegal() throws LocationFullException {
        Tank tank = trackedTank();
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_FRONT);
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BACKHOE), Tank.LOC_FRONT);

        assertTrue(bulldozerViolations(tank).contains("may not share a location"),
              "A bulldozer and a backhoe in the same location should be rejected");
    }
}
