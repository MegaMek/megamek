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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers taking back the roles a formation added when the formation cannot be filled.
 *
 * <p>A Heavy vehicle lance rolled as a Recon formation gets the recon role. In 3026 no heavy vehicle meets the
 * Recon Lance's Cruise 5, so the formation is abandoned and the slots are filled as an ordinary lance. That lance
 * used to keep the recon role, and the only heavy vehicle tagged for recon is the Hi-Scout Drone Carrier, so all four
 * slots took one (MegaMek/mekhq#11219).</p>
 */
class FormationFallbackRoleTest {

    @Test
    @DisplayName("The roles a formation pick adds are recorded; roles the lance already had are not")
    void recordsOnlyTheRolesTheFormationAdded() {
        ForceDescriptor lance = node(MissionRole.URBAN);
        Set<MissionRole> before = new HashSet<>(lance.getRoles());

        lance.getRoles().add(MissionRole.RECON);
        lance.recordFormationRoles(before);

        assertEquals(Set.of(MissionRole.RECON), lance.getFormationRoles());
    }

    @Test
    @DisplayName("An abandoned formation's role is removed from the lance and every slot under it")
    void abandonedFormationRoleIsDroppedFromTheWholeLance() {
        ForceDescriptor lance = node(MissionRole.URBAN);
        Set<MissionRole> before = new HashSet<>(lance.getRoles());
        lance.getRoles().add(MissionRole.RECON);
        lance.recordFormationRoles(before);

        ForceDescriptor firstSlot = node(MissionRole.URBAN, MissionRole.RECON);
        ForceDescriptor secondSlot = node(MissionRole.URBAN, MissionRole.RECON);
        ForceDescriptor nestedElement = node(MissionRole.URBAN, MissionRole.RECON);
        secondSlot.getSubForces().add(nestedElement);

        Set<MissionRole> dropped = lance.dropFormationRoles(List.of(firstSlot, secondSlot));

        assertEquals(Set.of(MissionRole.RECON), dropped);
        for (ForceDescriptor descriptor : List.of(lance, firstSlot, secondSlot, nestedElement)) {
            assertEquals(Set.of(MissionRole.URBAN), descriptor.getRoles(),
                  "the recon role goes; the urban role the lance already had stays");
        }
        assertTrue(lance.getFormationRoles().isEmpty());
    }

    @Test
    @DisplayName("A recon role the lance had before its formation was picked is kept")
    void inheritedRoleIsKept() {
        // A recon detachment that then rolls a Recon formation: the formation adds nothing new.
        ForceDescriptor lance = node(MissionRole.RECON);
        Set<MissionRole> before = new HashSet<>(lance.getRoles());
        lance.getRoles().add(MissionRole.RECON);
        lance.recordFormationRoles(before);
        ForceDescriptor slot = node(MissionRole.RECON);

        Set<MissionRole> dropped = lance.dropFormationRoles(List.of(slot));

        assertTrue(dropped.isEmpty());
        assertEquals(Set.of(MissionRole.RECON), lance.getRoles());
        assertEquals(Set.of(MissionRole.RECON), slot.getRoles());
    }

    @Test
    @DisplayName("A formation that added no role leaves every role alone")
    void formationWithoutARoleChangesNothing() {
        ForceDescriptor lance = node(MissionRole.URBAN);
        lance.recordFormationRoles(new HashSet<>(lance.getRoles()));
        ForceDescriptor slot = node(MissionRole.URBAN);

        assertTrue(lance.dropFormationRoles(List.of(slot)).isEmpty());
        assertEquals(Set.of(MissionRole.URBAN), slot.getRoles());
    }

    private static ForceDescriptor node(MissionRole... roles) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setUnitType(UnitType.TANK);
        for (MissionRole role : roles) {
            descriptor.getRoles().add(role);
        }
        return descriptor;
    }
}
