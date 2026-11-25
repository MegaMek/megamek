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

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A heat-neutral unit needs heat sinks for the 2 heat a RISC laser pulse module adds (IO:AE p. 87); a laser insulator
 * takes 1 off its laser's heat, to a minimum of 1 (TO:AUE p. 134).
 */
class HeatNeutralLaserEnhancementTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @ParameterizedTest
    @CsvSource({ "false, false, 3", "false, true, 2", "true, false, 5", "true, true, 4" })
    void aMediumLaserNeedsSinksForItsEnhancedHeat(boolean pulseModule, boolean insulator, int sinks) throws Exception {
        Tank tank = new Tank();
        Mounted<?> laser = tank.addEquipment(EquipmentType.get("Medium Laser"), Tank.LOC_FRONT);
        if (pulseModule) {
            tank.addEquipment(EquipmentType.get("ISRISCLaserPulseModule"), Tank.LOC_FRONT).setLinked(laser);
        }
        if (insulator) {
            tank.addEquipment(EquipmentType.get("ISLaserInsulator"), Tank.LOC_FRONT).setLinked(laser);
        }

        assertEquals(sinks, TestEntity.calcHeatNeutralHSRequirement(tank));
    }
}
