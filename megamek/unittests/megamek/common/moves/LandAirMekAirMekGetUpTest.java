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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.units.LandAirMek;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #9046: a LAM in AirMek mode that crashes (for example after losing a side torso, IO:AE p.108) lands prone. The
 * movement panel offered no Get Up button for it; these tests pin that the server accepts the Get Up the button sends.
 */
class LandAirMekAirMekGetUpTest extends GameBoardTestCase {

    static {
        initializeBoard("OPEN_2X2", """
              size 2 2
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              end""");
    }

    private LandAirMek fallenAirMek;

    @BeforeEach
    void setUp() {
        setBoard("OPEN_2X2");
        fallenAirMek = (LandAirMek) MMTestUtilities.getEntityForUnitTesting("Shadow Hawk LAM SHD-X2", false);
        assertNotNull(fallenAirMek, "the Shadow Hawk LAM test unit should load");
        fallenAirMek.setConversionMode(LandAirMek.CONV_MODE_AIR_MEK);
        fallenAirMek.setProne(true);
    }

    @Test
    void fallenAirMekCanGetUp() {
        MovePath getUp = getMovePathFor(fallenAirMek, 0, null, MoveStepType.GET_UP);

        assertTrue(getUp.isMoveLegal(), "getting up should be a legal move for a prone AirMek");
        assertFalse(getUp.getFinalProne(), "the AirMek should be standing after getting up");
    }

    /** The control: without the Get Up step the AirMek is still lying down, so the test above is not vacuous. */
    @Test
    void fallenAirMekStaysProneWithoutGettingUp() {
        MovePath noSteps = getMovePathFor(fallenAirMek, 0, null);

        assertTrue(noSteps.getFinalProne());
    }
}
