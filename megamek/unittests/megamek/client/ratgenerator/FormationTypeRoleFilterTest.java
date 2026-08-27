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
import java.util.Set;

import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Covers which mission-role filter a formation applies to the units it draws from.
 *
 * <p>A mission role is a hard filter on the unit table, and this class has two consumers that mean different things
 * by it: the Formation Builder passes the formation's own roles deliberately, while the Force Generator passes the
 * roles of the node it is filling. Adding the formation's roles on top of whatever the caller passed made those two
 * fight, and took Anti-Air and Artillery Fire - both of which declare {@code MIXED_ARTILLERY}, a role almost no unit
 * carries - down to an empty table so neither could ever be generated.</p>
 */
class FormationTypeRoleFilterTest {

    private static Parameters parametersWith(Set<MissionRole> roles) {
        return new Parameters(null, UnitType.MEK, 3067, "B", null, ModelRecord.NETWORK_NONE,
              EnumSet.noneOf(EntityMovementMode.class), roles, 0, null);
    }

    /**
     * The regression guard. A formation must leave the caller's role filter exactly as it found it; the caller is the
     * one that knows whether it is filling a node or answering "what could fill this formation".
     */
    @Test
    void aFormationDoesNotAddItsOwnRolesToTheCallersFilter() {
        FormationType antiAir = FormationType.getFormationType("Anti-Air");
        assertTrue(antiAir.getMissionRoles().contains(MissionRole.MIXED_ARTILLERY),
              "this test is only meaningful while Anti-Air still declares a mission role of its own");

        Parameters parameters = parametersWith(EnumSet.noneOf(MissionRole.class));
        antiAir.generateFormation(parameters, 4, ModelRecord.NETWORK_NONE, false);

        assertTrue(parameters.getRoles().isEmpty(),
              "a caller that asked for no role filter must not come back with one");
        assertFalse(parameters.getRoles().contains(MissionRole.MIXED_ARTILLERY),
              "the formation's own mission role must not be forced onto the caller");
    }

    /** A caller that does want the formation's roles passes them itself, and they must survive untouched. */
    @Test
    void aCallersOwnRoleFilterIsLeftAlone() {
        FormationType antiAir = FormationType.getFormationType("Anti-Air");

        Parameters parameters = parametersWith(EnumSet.of(MissionRole.RECON));
        antiAir.generateFormation(parameters, 4, ModelRecord.NETWORK_NONE, false);

        // getRoles hands back a list copy, so compare contents rather than the collection itself.
        assertEquals(1, parameters.getRoles().size(),
              "the filter the caller asked for must be the only filter it gets");
        assertTrue(parameters.getRoles().contains(MissionRole.RECON),
              "the caller's own role must survive untouched");
    }
}
