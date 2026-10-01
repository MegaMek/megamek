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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.ProtoMek;
import megamek.common.units.SupportTank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #5676: the Default Climb Mode setting is on by default, and for WiGE movement climb mode means Keep Elevation.
 * Every WiGE (vehicle, glider ProtoMek, LAM in AirMek mode) therefore started each turn holding its altitude, and paid
 * +2 MP for every hex of lower terrain (TW p.55), so gliding down a slope cost 3 MP per hex. A WiGE follows the terrain
 * unless the player chooses Keep Elevation, so it now starts each movement with climb mode off.
 */
class WiGEDefaultClimbModeTest extends GameBoardTestCase {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    static {
        initializeBoard("SLOPE_DOWN", """
              size 1 5
              hex 0101 4 "" ""
              hex 0102 3 "" ""
              hex 0103 2 "" ""
              hex 0104 1 "" ""
              hex 0105 0 "" ""
              end""");
    }

    private boolean savedDefaultClimbMode;

    @BeforeEach
    void setUp() {
        savedDefaultClimbMode = GUIP.getMoveDefaultClimbMode();
        GUIP.setMoveDefaultClimbMode(true);
        setBoard("SLOPE_DOWN");
    }

    @AfterEach
    void tearDown() {
        GUIP.setMoveDefaultClimbMode(savedDefaultClimbMode);
    }

    private ProtoMek glider() {
        ProtoMek glider = new ProtoMek();
        glider.setIsGlider(true);
        return glider;
    }

    /** Places the unit airborne at 0101, then starts a new round the way the server does. */
    private MovePath newRoundPathFor(Entity entity, MoveStepType... steps) {
        getMovePathFor(entity, 1, EntityMovementMode.WIGE);
        entity.newRound(2);
        MovePath path = new MovePath(getGame(), entity);
        for (MoveStepType step : steps) {
            path.addStep(step);
        }
        return path;
    }

    @Test
    void newRoundStartsGliderWithKeepElevationOff() {
        ProtoMek glider = glider();
        glider.setClimbMode(true);
        newRoundPathFor(glider);

        assertFalse(glider.climbMode(), "a WiGE must start its movement following the terrain");
    }

    @Test
    void gliderFollowsTheSlopeDownAtOneMpPerHex() {
        MovePath path = newRoundPathFor(glider(), MoveStepType.FORWARDS, MoveStepType.FORWARDS,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS);

        assertTrue(path.isMoveLegal());
        assertMovePathElevations(path, 1, 1, 1, 1);
        assertEquals(4, path.getMpUsed(), "gliding down a slope costs 1 MP per hex");
    }

    @Test
    void wigeVehicleFollowsTheSlopeDownAtOneMpPerHex() {
        MovePath path = newRoundPathFor(new SupportTank(), MoveStepType.FORWARDS, MoveStepType.FORWARDS,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS);

        assertTrue(path.isMoveLegal());
        assertMovePathElevations(path, 1, 1, 1, 1);
        assertEquals(4, path.getMpUsed(), "flying down a slope costs 1 MP per hex");
    }

    /** Keep Elevation is still available when the player asks for it, at +2 MP per hex. */
    @Test
    void keepElevationStillHoldsAltitudeWhenChosen() {
        MovePath path = newRoundPathFor(new SupportTank(), MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertTrue(path.isMoveLegal());
        assertMovePathElevations(path, 1, 2, 3);
        assertEquals(6, path.getMpUsed(), "holding altitude over lower terrain costs 3 MP per hex");
    }

    @Test
    void defaultClimbModeIsOffForWiGE() {
        SupportTank wige = new SupportTank();
        wige.setMovementMode(EntityMovementMode.WIGE);

        assertFalse(ClimbingHelper.getDefaultClimbMode(wige));
    }

    /** Units that do not move as a WiGE keep following the Default Climb Mode setting. */
    @Test
    void defaultClimbModeFollowsTheSettingForOtherUnits() {
        SupportTank hover = new SupportTank();
        hover.setMovementMode(EntityMovementMode.HOVER);
        BipedMek mek = new BipedMek();

        assertTrue(ClimbingHelper.getDefaultClimbMode(hover));
        assertTrue(ClimbingHelper.getDefaultClimbMode(mek));

        GUIP.setMoveDefaultClimbMode(false);
        assertFalse(ClimbingHelper.getDefaultClimbMode(mek));
    }
}
