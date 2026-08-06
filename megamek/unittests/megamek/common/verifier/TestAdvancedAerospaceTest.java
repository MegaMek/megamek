/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Vector;

import megamek.common.bays.Bay;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.NavalRepairFacility;
import megamek.common.units.SpaceStation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class TestAdvancedAerospaceTest {

    private static EntityVerifier verifier;
    private Vector<Bay> bays;

    @BeforeAll
    static void beforeAll() {
        File file = new File(TestAdvancedAerospaceTest.class.getResource("empty-verifier-options.xml").getFile());
        verifier = EntityVerifier.getInstance(file);
    }

    @BeforeEach
    void beforeEach() {
        bays = new Vector<>();
    }

    private Jumpship createJumpship() {
        Jumpship js = mock(Jumpship.class);
        when(js.getTransportBays()).thenReturn(bays);
        when(js.hasETypeFlag(ArgumentMatchers.anyLong()))
              .thenAnswer(inv -> ((Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP) & (Long) inv.getArguments()[0]) != 0);
        return js;
    }

    private SpaceStation createStation() {
        SpaceStation ss = mock(SpaceStation.class);
        when(ss.getTransportBays()).thenReturn(bays);
        when(ss.hasETypeFlag(ArgumentMatchers.anyLong()))
              .thenAnswer(inv -> ((Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP | Entity.ETYPE_SPACE_STATION)
                    & (Long) inv.getArguments()[0]) != 0);
        return ss;
    }

    @Test
    void correctBaysPassesWithSingleRepair() {
        Jumpship js = createJumpship();
        TestAdvancedAerospace test = new TestAdvancedAerospace(js, verifier.aeroOption, "test");

        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));

        assertTrue(test.correctBays(new StringBuffer()));
    }

    @Test
    void correctBaysFailsWhenRepairHasNoFacing() {
        Jumpship js = createJumpship();
        TestAdvancedAerospace test = new TestAdvancedAerospace(js, verifier.aeroOption, "test");

        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NONE, false));

        assertFalse(test.correctBays(new StringBuffer()));
    }

    @Test
    void correctBaysFailsWhenMultipleRepairHaveSameFacing() {
        SpaceStation ss = createStation();
        TestAdvancedAerospace test = new TestAdvancedAerospace(ss, verifier.aeroOption, "test");

        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));

        assertFalse(test.correctBays(new StringBuffer()));
    }

    @Test
    void correctBaysPassesWhenShipHasMultipleRepair() {
        Jumpship js = createJumpship();
        TestAdvancedAerospace test = new TestAdvancedAerospace(js, verifier.aeroOption, "test");
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_AFT, false));
        assertTrue(test.correctBays(new StringBuffer()));
    }

    @Test
    void correctBaysPassesWhenStationHasMultipleRepair() {
        SpaceStation ss = createStation();
        TestAdvancedAerospace test = new TestAdvancedAerospace(ss, verifier.aeroOption, "test");

        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_AFT, false));

        assertTrue(test.correctBays(new StringBuffer()));
    }
}
