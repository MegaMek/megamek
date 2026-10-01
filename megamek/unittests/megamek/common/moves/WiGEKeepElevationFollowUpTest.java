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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.options.OptionsConstants;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LandAirMek;
import megamek.common.units.SupportTank;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #9101, follow-ups to #5676. For WiGE movement, climb mode means Keep Elevation (+2 MP per hex, TW p.55).
 * <ul>
 *     <li>A LAM that converts to AirMek partway through its move starts WiGE movement with Keep Elevation off.</li>
 *     <li>The TacOps descent bonus (+1 MP per three hexes descended) counts the unit's own altitude, so a WiGE holding
 *     its altitude over a slope earns nothing.</li>
 * </ul>
 * Units start at 0101 facing south, so each FORWARDS step enters the next hex down the board.
 */
class WiGEKeepElevationFollowUpTest extends GameBoardTestCase {

    static {
        initializeBoard("FLAT", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              end""");

        initializeBoard("LONG_SLOPE_DOWN", """
              size 1 7
              hex 0101 6 "" ""
              hex 0102 5 "" ""
              hex 0103 4 "" ""
              hex 0104 3 "" ""
              hex 0105 2 "" ""
              hex 0106 1 "" ""
              hex 0107 0 "" ""
              end""");
    }

    @AfterEach
    void tearDown() {
        getGame().getOptions()
              .getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS)
              .setValue(false);
    }

    private LandAirMek climbingLamInMekMode() {
        LandAirMek lam = (LandAirMek) MMTestUtilities.getEntityForUnitTesting("Shadow Hawk LAM SHD-X2", false);
        assertNotNull(lam, "the Shadow Hawk LAM test unit should load");
        // The SHD-X2 is bimodal (no AirMek mode); make it a standard LAM so it can convert to AirMek
        lam.setLAMType(LandAirMek.LAM_STANDARD);
        lam.setClimbMode(true);
        return lam;
    }

    @Test
    void convertingToAirMekTurnsKeepElevationOff() {
        setBoard("FLAT");
        MovePath path = getMovePathFor(climbingLamInMekMode(), 0, null, MoveStepType.CONVERT_MODE);

        MoveStep convertStep = path.getLastStep();
        assertEquals(EntityMovementMode.WIGE, convertStep.getMovementMode(), "the LAM converts to AirMek");
        assertFalse(convertStep.climbMode(), "an AirMek starts WiGE movement with Keep Elevation off");
    }

    /** The control: a LAM that stays a Mek keeps the climb mode it started with. */
    @Test
    void stayingAMekKeepsClimbing() {
        setBoard("FLAT");
        MovePath path = getMovePathFor(climbingLamInMekMode(), 0, null, MoveStepType.FORWARDS);

        assertTrue(path.getLastStep().climbMode(), "a Mek keeps its climb mode");
    }

    private MovePath flyDownTheSlope(MoveStepType climbModeStep, int hexes) {
        setBoard("LONG_SLOPE_DOWN");
        getGame().getOptions()
              .getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS)
              .setValue(true);
        MoveStepType[] steps = new MoveStepType[hexes + 1];
        steps[0] = climbModeStep;
        for (int i = 1; i <= hexes; i++) {
            steps[i] = MoveStepType.FORWARDS;
        }
        return getMovePathFor(new SupportTank(), 1, EntityMovementMode.WIGE, steps);
    }

    @Test
    void followingTheTerrainDownEarnsTheDescentBonus() {
        MovePath path = flyDownTheSlope(MoveStepType.CLIMB_MODE_OFF, 6);

        assertTrue(path.isMoveLegal());
        assertEquals(2, path.getLastStep().getWiGEBonus(), "six hexes of descent earn two bonus MP");
    }

    @Test
    void holdingAltitudeEarnsNoDescentBonus() {
        MovePath path = flyDownTheSlope(MoveStepType.CLIMB_MODE_ON, 3);

        assertTrue(path.isMoveLegal());
        assertEquals(0, path.getLastStep().getWiGEBonus(), "a WiGE holding its altitude is not descending");
    }
}
