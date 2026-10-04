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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the combat engineer mission roles: that {@link ModelRecord#addEngineerRoles(EquipmentType, java.util.Set)}
 * derives the umbrella {@link MissionRole#ENGINEER} role plus the correct specialization facet from engineering
 * equipment, and that the new roles parse and apply to the expected unit types.
 */
class CombatEngineerRoleTest {

    @BeforeAll
    static void initEquipment() {
        EquipmentType.initializeTypes();
    }

    private static EnumSet<MissionRole> rolesFor(String internalName) {
        EnumSet<MissionRole> roles = EnumSet.noneOf(MissionRole.class);
        EquipmentType equipment = EquipmentType.get(internalName);
        assertTrue(equipment != null, "Test equipment should exist: " + internalName);
        ModelRecord.addEngineerRoles(equipment, roles);
        return roles;
    }

    @Test
    void bulldozerImpliesFieldworks() {
        EnumSet<MissionRole> roles = rolesFor(EquipmentTypeLookup.BULLDOZER);
        assertTrue(roles.contains(MissionRole.ENGINEER), "Bulldozer is engineering equipment");
        assertTrue(roles.contains(MissionRole.FIELDWORKS), "Bulldozer is a fieldworks tool");
    }

    @Test
    void backhoeImpliesFieldworks() {
        EnumSet<MissionRole> roles = rolesFor(EquipmentTypeLookup.BACKHOE);
        assertTrue(roles.contains(MissionRole.ENGINEER));
        assertTrue(roles.contains(MissionRole.FIELDWORKS), "Backhoe excavates, so it is a fieldworks tool");
    }

    @Test
    void bridgeLayerImpliesBridging() {
        EnumSet<MissionRole> roles = rolesFor(EquipmentTypeLookup.LIGHT_BRIDGE_LAYER);
        assertTrue(roles.contains(MissionRole.ENGINEER));
        assertTrue(roles.contains(MissionRole.BRIDGE_LAYER), "A bridge layer is a bridging asset");
    }

    @Test
    void mineSweeperImpliesMinesweeping() {
        EnumSet<MissionRole> roles = rolesFor("ISMineSweeper");
        assertTrue(roles.contains(MissionRole.ENGINEER));
        assertTrue(roles.contains(MissionRole.MINESWEEPER));
    }

    @Test
    void rockCutterImpliesDemolition() {
        EnumSet<MissionRole> roles = rolesFor(EquipmentTypeLookup.ROCK_CUTTER);
        assertTrue(roles.contains(MissionRole.ENGINEER));
        assertTrue(roles.contains(MissionRole.DEMOLITION), "A rock cutter breaches structures");
    }

    @Test
    void salvageArmIsEngineerWithoutASpecificFacet() {
        EnumSet<MissionRole> roles = rolesFor(EquipmentTypeLookup.SALVAGE_ARM);
        assertTrue(roles.contains(MissionRole.ENGINEER), "A salvage arm marks a recovery/engineer unit");
        assertFalse(roles.contains(MissionRole.BRIDGE_LAYER));
        assertFalse(roles.contains(MissionRole.DEMOLITION));
        assertFalse(roles.contains(MissionRole.FIELDWORKS));
        assertFalse(roles.contains(MissionRole.MINESWEEPER));
    }

    @Test
    void infantryEngineerEquipmentImpliesTheMatchingFacet() {
        // Infantry engineer specializations are implemented as carried equipment, so they derive through
        // the same path as vehicle and Mek gear.
        assertTrue(rolesFor(EquipmentTypeLookup.VIBRO_SHOVEL).contains(MissionRole.FIELDWORKS),
              "The vibro-shovel is the trench/fieldworks engineer tool");
        assertTrue(rolesFor(EquipmentTypeLookup.DEMOLITION_CHARGE).contains(MissionRole.DEMOLITION),
              "The demolition charge is the demolition engineer tool");
        assertTrue(rolesFor(EquipmentTypeLookup.INFANTRY_BRIDGE_KIT).contains(MissionRole.BRIDGE_LAYER),
              "The infantry bridge kit is the bridging engineer tool");
    }

    @Test
    void nonEngineeringEquipmentAddsNoEngineerRoles() {
        // A hatchet is an F_CLUB item but not an engineering tool; it must not be mistaken for one.
        EnumSet<MissionRole> roles = rolesFor("Hatchet");
        assertFalse(roles.contains(MissionRole.ENGINEER), "A hatchet is a weapon, not engineering equipment");
    }

    @Test
    void newRolesParseFromTheirDataStrings() {
        assertEquals(MissionRole.BRIDGE_LAYER, MissionRole.parseRole("bridge"));
        assertEquals(MissionRole.BRIDGE_LAYER, MissionRole.parseRole("bridge layer"));
        assertEquals(MissionRole.DEMOLITION, MissionRole.parseRole("demolition"));
        assertEquals(MissionRole.FIREFIGHTER, MissionRole.parseRole("firefighter"));
        assertEquals(MissionRole.FIREFIGHTER, MissionRole.parseRole("firefighting"));
        assertEquals(MissionRole.FIELDWORKS, MissionRole.parseRole("fieldworks"));
        assertEquals(MissionRole.FIELDWORKS, MissionRole.parseRole("trench"));
    }

    @Test
    void newRolesRoundTripThroughTheirDataStrings() {
        // A role written to a year file as toString() must parse back to the same role, since parseRole treats
        // underscores as spaces (e.g. "bridge_layer" -> BRIDGE_LAYER).
        for (MissionRole role : EnumSet.of(MissionRole.BRIDGE_LAYER, MissionRole.DEMOLITION,
              MissionRole.FIREFIGHTER, MissionRole.FIELDWORKS)) {
            assertEquals(role, MissionRole.parseRole(role.toString()), "Role should round-trip: " + role);
        }
    }

    @Test
    void engineerFacetsApplyToGroundCombatUnitTypes() {
        for (MissionRole role : EnumSet.of(MissionRole.BRIDGE_LAYER, MissionRole.DEMOLITION,
              MissionRole.FIREFIGHTER, MissionRole.FIELDWORKS)) {
            assertTrue(role.fitsUnitType(UnitType.MEK), role + " should apply to Meks");
            assertTrue(role.fitsUnitType(UnitType.TANK), role + " should apply to ground vehicles");
            assertTrue(role.fitsUnitType(UnitType.INFANTRY), role + " should apply to conventional infantry");
            assertFalse(role.fitsUnitType(UnitType.AEROSPACE_FIGHTER),
                  role + " should not apply to aerospace fighters");
        }
    }
}
