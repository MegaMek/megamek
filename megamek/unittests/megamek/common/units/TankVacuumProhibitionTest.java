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

import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests which vehicles a vacuum or trace atmosphere refuses to let onto the field, TO:AR p.35.
 */
class TankVacuumProhibitionTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /**
     * A vehicle that would otherwise be allowed in a vacuum: a fusion power plant, which needs no air, and the
     * Environmental Sealing that keeps the crew alive.
     *
     * @param movementMode the movement mode to give the vehicle
     *
     * @return the vehicle, ready to be asked whether it is doomed
     */
    private Tank sealedFusionVehicle(EntityMovementMode movementMode) throws LocationFullException {
        Tank tank = new Tank();
        tank.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        tank.setMovementMode(movementMode);
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.SV_ENVIRONMENTAL_SEALING_CHASSIS_MOD), Tank.LOC_BODY);
        return tank;
    }

    @Test
    @DisplayName("A sealed, fusion-powered tracked vehicle can operate in a vacuum")
    void aSealedTrackedVehicleSurvivesVacuum() throws LocationFullException {
        Tank trackedVehicle = sealedFusionVehicle(EntityMovementMode.TRACKED);

        assertFalse(trackedVehicle.doomedInVacuum(),
              "Environmental Sealing and a fusion plant are what a ground vehicle needs for a vacuum");
    }

    @Test
    @DisplayName("A hovercraft cannot operate in a vacuum however well sealed it is")
    void aHovercraftIsDoomedInVacuumEvenWhenSealed() throws LocationFullException {
        Tank hovercraft = sealedFusionVehicle(EntityMovementMode.HOVER);

        assertTrue(hovercraft.doomedInVacuum(),
              "a hovercraft has no air to ride on, so sealing does not save it");
    }

    @Test
    @DisplayName("A WiGE cannot operate in a vacuum however well sealed it is")
    void aWigeIsDoomedInVacuumEvenWhenSealed() throws LocationFullException {
        Tank wigeVehicle = sealedFusionVehicle(EntityMovementMode.WIGE);

        assertTrue(wigeVehicle.doomedInVacuum(),
              "a WiGE has no air to ride on, so sealing does not save it");
    }

    @Test
    @DisplayName("An unsealed ground vehicle is still doomed in a vacuum")
    void anUnsealedVehicleIsStillDoomed() {
        Tank trackedVehicle = new Tank();
        trackedVehicle.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        trackedVehicle.setMovementMode(EntityMovementMode.TRACKED);

        assertTrue(trackedVehicle.doomedInVacuum(), "without Environmental Sealing the crew cannot breathe");
    }

    @Test
    @DisplayName("An internal combustion vehicle is doomed in a vacuum whatever its sealing")
    void anInternalCombustionVehicleIsDoomed() throws LocationFullException {
        Tank tank = new Tank();
        tank.setEngine(new Engine(100, Engine.COMBUSTION_ENGINE, 0));
        tank.setMovementMode(EntityMovementMode.TRACKED);
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.SV_ENVIRONMENTAL_SEALING_CHASSIS_MOD), Tank.LOC_BODY);

        assertTrue(tank.doomedInVacuum(), "an engine that burns air cannot run without any");
    }

    @Test
    @DisplayName("A VTOL cannot operate in a vacuum")
    void aVtolIsDoomedInVacuum() {
        VTOL vtol = new VTOL();
        vtol.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));

        assertTrue(vtol.doomedInVacuum(), "a rotor needs air to bite on");
    }

    /**
     * Guards against the Environmental Sealing lookup name drifting: if the equipment cannot be found the sealed
     * vehicles above would silently become unsealed and their tests would pass for the wrong reason.
     */
    @Test
    @DisplayName("The Environmental Sealing equipment used by these tests exists")
    void environmentalSealingEquipmentIsFound() throws LocationFullException {
        Tank trackedVehicle = sealedFusionVehicle(EntityMovementMode.TRACKED);

        assertTrue(trackedVehicle.hasEnvironmentalSealing(), "the test vehicle should really be sealed");
    }
}
