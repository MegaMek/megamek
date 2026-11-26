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
 */
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * TO:AUE p. 134: "Laser insulators are limited to smaller units (200 tons and less)". A naval combat vehicle, built up
 * to 300 tons, is the lightest unit able to mount insulators that can exceed the limit.
 */
class LaserInsulatorTonnageTest {
    private static final String MESSAGE = "Laser insulators are limited to units of 200 tons or less";

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void aTwoHundredTonVehicleMayMountALaserInsulator() throws Exception {
        assertFalse(insulatedNavalVehicleReport(200).contains(MESSAGE));
    }

    @Test
    void aVehicleOverTwoHundredTonsMayNotMountALaserInsulator() throws Exception {
        assertTrue(insulatedNavalVehicleReport(201).contains(MESSAGE));
    }

    private static String insulatedNavalVehicleReport(double weight) throws Exception {
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.NAVAL);
        tank.setWeight(weight);
        Mounted<?> laser = tank.addEquipment(EquipmentType.get("Medium Laser"), Tank.LOC_FRONT);
        tank.addEquipment(EquipmentType.get("ISLaserInsulator"), Tank.LOC_FRONT).setLinked(laser);
        StringBuffer report = new StringBuffer();
        new TestTank(tank, EntityVerifier.getInstance(null).tankOption, null).hasIllegalEquipmentCombinations(report);
        return report.toString();
    }
}
