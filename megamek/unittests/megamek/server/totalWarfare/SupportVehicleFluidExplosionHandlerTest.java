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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.Report;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Support Vehicle Inferno Fuel cook-off rule (TO:AUE p.173): only an unsealed Support
 * Vehicle actually carrying Inferno Fuel rolls for a cook-off; protected or non-carrying units are
 * skipped entirely.
 */
class SupportVehicleFluidExplosionHandlerTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static AmmoType infernoFuel() {
        AmmoType ammoType = AmmoType.getMunitionsFor(AmmoTypeEnum.FLUID_GUN).stream()
              .filter(candidate -> candidate.getMunitionType().contains(Munitions.M_INFERNO_FUEL))
              .findFirst()
              .orElse(null);
        assertNotNull(ammoType, "expected Inferno Fuel Fluid Gun ammo");
        return ammoType;
    }

    private static SupportTank unsealedSupportTank() {
        SupportTank tank = new SupportTank();
        tank.setMovementMode(EntityMovementMode.WHEELED);
        tank.setWeight(20);
        return tank;
    }

    private static SupportVehicleFluidExplosionHandler handler() {
        TWGameManager gameManager = mock(TWGameManager.class);
        // If the (random) roll detonates the fuel, the handler asks the manager to explode the bin.
        lenient().when(gameManager.explodeEquipment(any(), anyInt(), any())).thenReturn(new Vector<>());
        return new SupportVehicleFluidExplosionHandler(gameManager);
    }

    @Test
    void nonSupportVehicleIsNeverChecked() throws LocationFullException {
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.WHEELED);
        tank.setWeight(20);
        tank.addEquipment(infernoFuel(), Tank.LOC_FRONT);

        Vector<Report> reports = handler().checkInfernoFuelCookOff(tank, "test");
        assertTrue(reports.isEmpty(), "A non-Support-Vehicle never makes the Inferno Fuel cook-off roll");
    }

    @Test
    void unsealedSupportVehicleWithoutInfernoFuelIsNotChecked() {
        SupportTank tank = unsealedSupportTank();

        Vector<Report> reports = handler().checkInfernoFuelCookOff(tank, "test");
        assertTrue(reports.isEmpty(), "Without Inferno Fuel aboard there is nothing to cook off");
    }

    @Test
    void sealedSupportVehicleIsProtected() throws LocationFullException {
        SupportTank tank = unsealedSupportTank();
        tank.addEquipment(infernoFuel(), Tank.LOC_FRONT);
        tank.addEquipment(EquipmentType.get("Environmental Sealing"), Tank.LOC_BODY);

        Vector<Report> reports = handler().checkInfernoFuelCookOff(tank, "test");
        assertTrue(reports.isEmpty(), "Environmental Sealing protects the Inferno Fuel from cook-off");
    }

    @Test
    void unsealedSupportVehicleWithInfernoFuelRollsForCookOff() throws LocationFullException {
        SupportTank tank = unsealedSupportTank();
        tank.addEquipment(infernoFuel(), Tank.LOC_FRONT);

        Vector<Report> reports = handler().checkInfernoFuelCookOff(tank, "test");
        assertFalse(reports.isEmpty(),
              "An unsealed Support Vehicle carrying Inferno Fuel always reports its cook-off roll");
    }
}
