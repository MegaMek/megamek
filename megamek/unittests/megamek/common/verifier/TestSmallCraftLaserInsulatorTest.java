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
import megamek.common.units.Dropship;
import megamek.common.units.SmallCraft;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** TO:AUE p. 134 lists Small Craft, but not DropShips, among the units that may mount Laser Insulators. */
class TestSmallCraftLaserInsulatorTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void smallCraftMayMountAnInsulatedLaser() throws Exception {
        String report = equipmentReport(new SmallCraft());

        assertFalse(report.contains("Cannot mount Laser Insulator"), report);
    }

    @Test
    void dropShipMayNotMountALaserInsulator() throws Exception {
        assertTrue(equipmentReport(new Dropship()).contains("Cannot mount Laser Insulator"));
    }

    private static String equipmentReport(SmallCraft craft) throws Exception {
        craft.setWeight(200);
        var laser = craft.addEquipment(EquipmentType.get("Medium Laser"), SmallCraft.LOC_NOSE);
        craft.addEquipment(EquipmentType.get("ISLaserInsulator"), SmallCraft.LOC_NOSE).setLinked(laser);
        TestSmallCraft verifier = new TestSmallCraft(craft, EntityVerifier.getInstance(null).aeroOption, null);
        StringBuffer report = new StringBuffer();
        verifier.hasIllegalEquipmentCombinations(report);
        return report.toString();
    }
}
