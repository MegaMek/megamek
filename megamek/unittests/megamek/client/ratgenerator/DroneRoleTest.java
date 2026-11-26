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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers the {@link MissionRole#DRONE} role, which keeps drones and drone carriers out of generated forces.
 *
 * <p>A Mercenary heavy recon vehicle lance in 3026 built four Hi-Scout Drone Carriers (MegaMek/mekhq#11219): a carrier
 * arrives without its drones, and the drones alone are no use to a combat force. Units are given the role from their
 * drone equipment, can also be tagged {@code drone} in the year files, and are only generated when drones are asked
 * for.</p>
 */
class DroneRoleTest {
    /** Availability and strictness are arbitrary here; only whether a unit survives the filter is under test. */
    private static final double AVAILABILITY = 5.0;
    private static final int YEAR = 3026;
    private static final int STRICTNESS = 3;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("The role parses from its data string and round-trips through it")
    void roleParsesFromItsDataString() {
        assertEquals(MissionRole.DRONE, MissionRole.parseRole("drone"));
        assertEquals(MissionRole.DRONE, MissionRole.parseRole("DRONE"), "year files are not case-consistent");
        assertEquals(MissionRole.DRONE, MissionRole.parseRole(MissionRole.DRONE.toString()));
    }

    @Test
    @DisplayName("Drones come as any unit type")
    void droneFitsEveryUnitType() {
        for (int unitType : new int[] { UnitType.MEK, UnitType.TANK, UnitType.AEROSPACE_FIGHTER, UnitType.DROPSHIP }) {
            assertTrue(MissionRole.DRONE.fitsUnitType(unitType), UnitType.getTypeName(unitType));
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "ISDroneCarrierControlSystem", "ISDroneOperatingSystem", "SmartRoboticControlSystem",
                             "ShieldedAeroSRCS" })
    @DisplayName("Drone equipment gives the drone role")
    void droneEquipmentGivesTheRole(String internalName) {
        EquipmentType equipment = EquipmentType.get(internalName);
        assertNotNull(equipment, internalName + " must exist");
        Set<MissionRole> roles = EnumSet.noneOf(MissionRole.class);

        ModelRecord.addDroneRole(equipment, roles);

        assertEquals(Set.of(MissionRole.DRONE), roles);
    }

    @Test
    @DisplayName("Other equipment does not")
    void otherEquipmentDoesNotGiveTheRole() {
        Set<MissionRole> roles = EnumSet.noneOf(MissionRole.class);

        ModelRecord.addDroneRole(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), roles);

        assertFalse(roles.contains(MissionRole.DRONE));
    }

    @Test
    @DisplayName("A drone carrier is never generated for a general draw or a recon draw")
    void droneCarrierIsNotGenerated() {
        ModelRecord droneCarrier = modelWithRoles("Hi-Scout Drone Carrier", "recon,drone");

        assertNull(adjustFor(droneCarrier), "a general draw must not return a drone carrier");
        assertNull(adjustFor(droneCarrier, MissionRole.RECON), "a recon draw must not return a drone carrier");
    }

    @Test
    @DisplayName("Asking for drones still returns them")
    void askingForDronesReturnsThem() {
        ModelRecord droneCarrier = modelWithRoles("Hi-Scout Drone Carrier", "recon,drone");

        assertNotNull(adjustFor(droneCarrier, MissionRole.DRONE));
    }

    @Test
    @DisplayName("Asking for drones returns nothing else")
    void askingForDronesExcludesOtherUnits() {
        ModelRecord scoutTank = modelWithRoles("Pegasus Scout Hover Tank", "recon");

        assertNull(adjustFor(scoutTank, MissionRole.DRONE));
        assertNotNull(adjustFor(scoutTank, MissionRole.RECON), "a recon unit without the drone role is unaffected");
    }

    private static Double adjustFor(ModelRecord model, MissionRole... desiredRoles) {
        Set<MissionRole> roles = EnumSet.noneOf(MissionRole.class);
        for (MissionRole role : desiredRoles) {
            roles.add(role);
        }
        return MissionRole.adjustAvailabilityByRole(AVAILABILITY, roles, model, YEAR, STRICTNESS);
    }

    private static ModelRecord modelWithRoles(String chassis, String roles) {
        ModelRecord model = new ModelRecord(chassis, "");
        model.addRoles(roles);
        return model;
    }
}
