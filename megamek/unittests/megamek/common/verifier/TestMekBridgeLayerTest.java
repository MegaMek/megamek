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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Construction tests for the Bridge-Layer (AVLB) placement rule (TM p.242 / TW): only quad BattleMeks/IndustrialMeks
 * may mount a bridgelayer, and only in a torso location. Exercises {@link TestMek#isValidMekLocation} - the method MML
 * uses to decide whether equipment may be placed in a location - so an illegal placement is blocked, not merely
 * flagged.
 *
 * @author Claude Code (Opus 4.8)
 */
class TestMekBridgeLayerTest {

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("a quad mek may mount a bridgelayer in a torso location")
    void quadMekTorsoIsValid() {
        Mek quad = mock(Mek.class);
        when(quad.isQuadMek()).thenReturn(true);
        when(quad.locationIsTorso(anyInt())).thenReturn(true);
        assertTrue(TestMek.isValidMekLocation(quad, MiscType.createHeavyBridgeLayer(), Mek.LOC_RIGHT_TORSO,
              new StringBuffer()));
    }

    @Test
    @DisplayName("a biped mek may NOT mount a bridgelayer (quad-only) - the placement is blocked")
    void bipedMekIsRejected() {
        Mek biped = mock(Mek.class);
        when(biped.isQuadMek()).thenReturn(false);
        when(biped.locationIsTorso(anyInt())).thenReturn(true);
        StringBuffer buffer = new StringBuffer();
        assertFalse(TestMek.isValidMekLocation(biped, MiscType.createHeavyBridgeLayer(), Mek.LOC_RIGHT_TORSO, buffer));
        assertTrue(buffer.toString().toLowerCase().contains("quad"),
              "the rejection message should explain the quad-only requirement: " + buffer);
    }

    @Test
    @DisplayName("even on a quad, a bridgelayer must be in a torso (not an arm/leg/head)")
    void quadNonTorsoIsRejected() {
        Mek quad = mock(Mek.class);
        when(quad.isQuadMek()).thenReturn(true);
        when(quad.locationIsTorso(anyInt())).thenReturn(false);
        StringBuffer buffer = new StringBuffer();
        assertFalse(TestMek.isValidMekLocation(quad, MiscType.createLightBridgeLayer(), Mek.LOC_HEAD, buffer));
        assertTrue(buffer.toString().toLowerCase().contains("torso"),
              "the rejection message should require a torso location: " + buffer);
    }
}
