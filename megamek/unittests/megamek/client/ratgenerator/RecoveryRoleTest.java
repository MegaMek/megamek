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

import megamek.common.units.UnitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@link MissionRole#RECOVERY} role, which says a vehicle drags downed units home.
 *
 * <p>Recovery is the one support capability that cannot be read off the unit itself: a MASH vehicle carries MASH
 * theatres and a repair vehicle carries a Mobile Field Base, but the BattleMek Recovery Vehicle carries only a cargo
 * bay, so in the data it looked exactly like a flatbed truck. These tests pin the two halves that matter - that the
 * role parses from the year files, and that asking for it does not hand back the cargo haulers it used to.</p>
 */
class RecoveryRoleTest {
    /** Availability and strictness are arbitrary here; only whether a unit survives the filter is under test. */
    private static final double AVAILABILITY = 5.0;
    private static final int YEAR = 3067;
    private static final int STRICTNESS = 3;

    @Test
    @DisplayName("The role parses from its data string and round-trips through it")
    void roleParsesFromItsDataString() {
        assertEquals(MissionRole.RECOVERY, MissionRole.parseRole("recovery"));
        assertEquals(MissionRole.RECOVERY, MissionRole.parseRole("RECOVERY"), "year files are not case-consistent");
        assertEquals(MissionRole.RECOVERY, MissionRole.parseRole(MissionRole.RECOVERY.toString()),
              "a role written out as toString() must parse back to the same role");
    }

    @Test
    @DisplayName("Recovery applies to ground vehicles and nothing else")
    void recoveryFitsGroundVehiclesOnly() {
        assertTrue(MissionRole.RECOVERY.fitsUnitType(UnitType.TANK), "recovery vehicles are ground vehicles");
        for (int unitType : new int[] { UnitType.MEK, UnitType.VTOL, UnitType.NAVAL, UnitType.INFANTRY,
                                        UnitType.BATTLE_ARMOR, UnitType.AEROSPACE_FIGHTER, UnitType.DROPSHIP }) {
            assertFalse(MissionRole.RECOVERY.fitsUnitType(unitType),
                  UnitType.getTypeName(unitType) + " fields no recovery vehicles");
        }
    }

    @Test
    @DisplayName("Asking for recovery keeps recovery vehicles")
    void askingForRecoveryKeepsRecoveryVehicles() {
        // What a tagged BattleMek Recovery Vehicle looks like: it keeps the roles it already had, because a
        // recovery vehicle with a cargo bay is still cargo capable.
        ModelRecord recoveryVehicle = modelWithRoles("BattleMek Recovery Vehicle", "recovery,cargo,support");

        assertNotNull(adjustFor(recoveryVehicle, MissionRole.RECOVERY),
              "a vehicle tagged for recovery must survive a call for recovery");
    }

    @Test
    @DisplayName("Asking for recovery no longer hands back cargo trucks")
    void askingForRecoveryExcludesCargoTrucks() {
        // The Flatbed Truck carries the roles recovery vehicles used to be indistinguishable from.
        ModelRecord flatbedTruck = modelWithRoles("Flatbed Truck", "cargo,support");

        assertNull(adjustFor(flatbedTruck, MissionRole.RECOVERY),
              "a cargo hauler must not answer a call for recovery, which is the whole point of the role");
    }

    @Test
    @DisplayName("A civilian recovery vehicle is left out unless civilians were asked for")
    void civilianRecoveryVehiclesAreExcludedUnlessRequested() {
        ModelRecord civilianWrecker = modelWithRoles("Civilian Wrecker", "recovery,cargo,civilian");

        assertNull(adjustFor(civilianWrecker, MissionRole.RECOVERY),
              "a military call must not return a civilian wrecker");
        assertNotNull(adjustFor(civilianWrecker, MissionRole.RECOVERY, MissionRole.CIVILIAN),
              "asking for civilian units as well must return it");
    }

    @Test
    @DisplayName("Cargo still behaves as it did, so tagging a vehicle costs it nothing")
    void taggingForRecoveryDoesNotCostAVehicleItsCargoRole() {
        ModelRecord recoveryVehicle = modelWithRoles("BattleMek Recovery Vehicle", "recovery,cargo,support");

        assertNotNull(adjustFor(recoveryVehicle, MissionRole.CARGO),
              "a recovery vehicle with a cargo bay must still answer a call for cargo");
    }

    /** Runs the availability filter for the given desired roles, returning {@code null} when the unit is excluded. */
    private static Double adjustFor(ModelRecord model, MissionRole... desiredRoles) {
        Set<MissionRole> roles = EnumSet.noneOf(MissionRole.class);
        for (MissionRole role : desiredRoles) {
            roles.add(role);
        }
        return MissionRole.adjustAvailabilityByRole(AVAILABILITY, roles, model, YEAR, STRICTNESS);
    }

    /** A bare model record carrying the roles a year file would have given it. */
    private static ModelRecord modelWithRoles(String chassis, String roles) {
        ModelRecord model = new ModelRecord(chassis, "");
        model.addRoles(roles);
        return model;
    }
}
