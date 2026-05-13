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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import megamek.common.CriticalSlot;
import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.Test;

/**
 * Server-side regression tests for TacOps Climbing fall/abort handling in
 * {@link MovePathHandler}. Lives in this package so it can construct the
 * package-private {@code MovePathHandler} directly.
 */
public class MovePathHandlerClimbingTest extends GameBoardTestCase {

    static {
        initializeBoard("BOARD_5_FLOOR_BUILDING", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "bldg_elev:5;building:2:80;bldg_cf:80" ""
              end""");
    }

    private Mek createClimbableMek() {
        BipedMek mek = new BipedMek();
        mek.setChassis("Test");
        mek.setModel("Climber");
        mek.setWeight(50.0);
        mek.setOriginalWalkMP(8);
        mek.setOriginalJumpMP(0);
        mek.autoSetInternal();
        mek.setCrew(new Crew(CrewType.SINGLE));
        setupArmActuators(mek, Mek.LOC_LEFT_ARM);
        setupArmActuators(mek, Mek.LOC_RIGHT_ARM);
        return mek;
    }

    private void setupArmActuators(Mek mek, int location) {
        mek.setCritical(location, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_SHOULDER));
        mek.setCritical(location, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_ARM));
        mek.setCritical(location, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_ARM));
        mek.setCritical(location, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HAND));
    }

    private void enableTacOpsClimbing() {
        IOption climbing = getGame().getOptions()
              .getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CLIMBING);
        climbing.setValue(true);
    }

    // -----------------------------------------------------------------------
    // Failed-first-PSR-from-elevation-0: when the very first climbing PSR
    // fails, the Mek slips before getting off the ground — no fall, no
    // damage, but climbing must abort for the turn and the entity must stay
    // in the source hex at ground level. QA observation (Agrotera on a
    // 12-level climb): the round report correctly said "fails to find a
    // handhold" — but the entity ended up at the building roof (elevation
    // 12) anyway. Root cause: step processing earlier in the loop moved the
    // entity to the climb step's destination, and the fail branch only
    // reset local control variables (curPos/curVTOLElevation), never
    // entity.setPosition/setElevation. Fix adds the entity reverts inside
    // the fail block.
    // -----------------------------------------------------------------------

    @Test
    void failedFirstClimbingPsrLeavesEntityAtSourceHexGroundLevel() throws Exception {
        setBoard("BOARD_5_FLOOR_BUILDING");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        // ServerFactory wires the Server.getServerInstance() singleton that
        // gameManager.entityUpdate -> send() requires; without it processMovement
        // NPEs on the first packet send.
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        Mek mek = createClimbableMek();
        mek.setOwner(player);
        Coords startingPos = new Coords(0, 0);
        // getMovePathFor's initializeUnit places the entity at (0,0) facing 3
        // (south, toward the building hex) and registers it in the game.
        MovePath movePath = getMovePathFor(mek, 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Climbing onto a 5-floor building should compile as legal — the test exercises "
                    + "the SERVER-side failed-PSR handling, not legality.");
        assertEquals(startingPos, mek.getPosition(),
              "Pre-condition: entity starts at (0,0) before processMovement runs.");
        assertEquals(0, mek.getElevation(),
              "Pre-condition: entity starts at elevation 0 before processMovement runs.");

        // Force every doSkillCheckWhileMoving invocation to report failure (>0).
        // The first climbing PSR fires from elevation 0; under the bug, the
        // entity would persist at the building roof. Under the fix, processMovement
        // reverts to (0,0) elevation 0.
        doReturn(1).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        assertEquals(startingPos, mek.getPosition(),
              "After a failed first climbing PSR from elevation 0, the entity MUST be at "
                    + "the source hex — not at the climb destination. The fail branch must "
                    + "call entity.setPosition(lastPos), not just reset local variables.");
        assertEquals(0, mek.getElevation(),
              "After a failed first climbing PSR from elevation 0, the entity MUST be at "
                    + "ground level — not at the building roof. The fail branch must call "
                    + "entity.setElevation(0).");
        assertFalse(mek.isClimbing(),
              "Climbing state must be cleared when the first PSR fails — the attempt "
                    + "aborted, the unit is back on the ground.");
        assertEquals(0, mek.getClimbingLevelsChosen(),
              "Chosen-levels must reset when the climb aborts, so a subsequent attempt "
                    + "doesn't inherit a stale player choice.");
    }
}
