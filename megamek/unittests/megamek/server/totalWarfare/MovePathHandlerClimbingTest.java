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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import megamek.common.CriticalSlot;
import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
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

        // Water under a bridge: Mek at water bottom (-2) starts a 4-level climb up onto the
        // bridge surface (+2). A 2-level partial climb stops it at elevation 0 — water surface
        // — while still clinging to the bridge side. Used by the partial-climb cleanup test.
        initializeBoard("BOARD_BRIDGE_OVER_DEEP_WATER", """
              size 1 2
              hex 0101 0 "water:2" ""
              hex 0102 0 "water:2;bridge:1;bridge_cf:100;bridge_elev:2" ""
              end""");

        // Cliff (level 3) above adjacent water:2 hex. Mek edge-climb-down 3 levels drops it
        // from absolute alt +3 to absolute alt 0 — which is the destination hex's water
        // SURFACE (elev 0 relative). Used by the edge-descent-into-water regression.
        initializeBoard("BOARD_CLIFF_ABOVE_DEEP_WATER", """
              size 1 2
              hex 0101 3 "" ""
              hex 0102 0 "water:2" ""
              end""");

        // Same cliff/water shape but with the water hex FIRST so a fresh Mek (placed at (0,0)
        // by getMovePathFor) is already in the water hex. Used by the continuation
        // climb-down-into-water regression where the Mek is mid-descent at water surface.
        initializeBoard("BOARD_DEEP_WATER_BELOW_CLIFF", """
              size 1 2
              hex 0101 0 "water:2" ""
              hex 0102 3 "" ""
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
        // Initialize armor on every location so submerged-Mek tests don't auto-doom the unit
        // (an unarmored cockpit/torso location takes critical-roll damage from water exposure
        // and can flag the entity as doomed before the climb branch ever runs).
        for (int loc = 0; loc < mek.locations(); loc++) {
            mek.initializeArmor(10, loc);
            if (mek.hasRearArmor(loc)) {
                mek.initializeRearArmor(5, loc);
            }
        }
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

    // -----------------------------------------------------------------------
    // Partial-climb out of deep water: a Mek climbing a tall bridge from the
    // water bottom (elevation -2) does so over multiple turns. A 2-level
    // partial leaves it at elevation 0 — water surface — still clinging to
    // the bridge side. The end-of-movement defensive cleanup used to wipe
    // climbing/dangling whenever the entity ended at elevation 0 outside a
    // building roof, which clobbered this legitimate mid-climb state and
    // prevented the continue-climbing dialog from firing on the next turn.
    // The fix preserves the flag when the entity is plausibly clinging to a
    // climbable feature in its facing hex.
    // -----------------------------------------------------------------------

    @Test
    void partialClimbAtWaterSurfacePreservesClimbingFlag() throws Exception {
        setBoard("BOARD_BRIDGE_OVER_DEEP_WATER");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        Mek mek = createClimbableMek();
        mek.setOwner(player);
        Coords startingPos = new Coords(0, 0);
        // Place the Mek at water bottom (-2) facing south toward the bridge hex (0,1).
        MovePath movePath = getMovePathFor(mek, -2, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Climb of 4 levels (-2 → +2) onto the bridge should compile as a legal "
                    + "multi-turn TacOps climb.");
        // Pre-condition: the FORWARDS step must have placed the Mek on the bridge surface
        // (elevation +2) and been marked as a climbing step. If this fails, the
        // Entity.calcElevation bridge fix isn't firing and the rest of the test is moot.
        assertEquals(2, movePath.getLastStep().getElevation(),
              "FORWARDS step must resolve to the bridge surface (+2); Entity.calcElevation must "
                    + "place the Mek on the tall bridge under TacOps Climbing.");
        assertTrue(movePath.getLastStep().isClimbing(),
              "FORWARDS step must be marked as a climbing step so processMovement runs the "
                    + "climb branch.");

        // Mimic the climbing dialog committing a 2-of-4 level partial climb. The dialog
        // pushes this via sendUpdateEntity before the path commits; in this server-side
        // test we set it directly on the entity.
        mek.setClimbingLevelsChosen(2);
        // All PSRs succeed.
        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        // Sanity: the climbing branch in processSteps MUST have run (one PSR call per
        // chosen level). If it didn't, the partial-climb logic never executed and the
        // assertions below are diagnosing the wrong thing.
        verify(gameManager, times(2)).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        assertEquals(startingPos, mek.getPosition(),
              "After a partial climb, the Mek clings to the SOURCE hex (the water hex) — "
                    + "the bridge hex isn't entered until the climb completes.");
        assertEquals(0, mek.getElevation(),
              "A 2-level partial climb from -2 leaves the Mek at water-surface elevation 0, "
                    + "clinging to the bridge side.");
        assertTrue(mek.isClimbing(),
              "The defensive end-of-movement cleanup MUST preserve climbing=true here — the "
                    + "Mek is mid-multi-turn climb, not stale. The facing hex has a bridge "
                    + "above the Mek; clearing the flag would prevent the continue-climbing "
                    + "dialog from firing next turn.");
    }

    // -----------------------------------------------------------------------
    // Bridge-over-water step cost: a Mek crossing onto a bridge surface (above
    // the water) should NOT pay the water-entry MP for the hex. The water
    // column ends at the hex surface; the Mek's destination elevation is the
    // bridge top, well above that. The water-cost block in calcMovementCostFor
    // used to apply even when the unit's destination was above the water,
    // padding the climb-continuation step's cost by 2-3 MP (the in-game
    // symptom was a continued bridge climb costing 7 MP instead of 5).
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Edge climb-down INTO water: Mek at the top of a 3-level cliff descends
    // off the edge into an adjacent water:2 hex. The descent ends at elev 0
    // (the water's surface) — that's NOT solid ground (floor is at -2), so
    // the climbing flag MUST be preserved. Pre-fix, the edge-climb-down
    // completion treated elev 0 as ground unconditionally and cleared the
    // climbing flag; the continue-climbing dialog would then not fire on the
    // next turn, leaving the player no way to drop the Mek the rest of the
    // way into the water. This is the exact scenario the in-game tester hit
    // while validating cliff-side dives.
    // -----------------------------------------------------------------------

    @Test
    void edgeClimbDownIntoWaterPreservesClimbingFlagAtWaterSurface() throws Exception {
        setBoard("BOARD_CLIFF_ABOVE_DEEP_WATER");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        Mek mek = createClimbableMek();
        mek.setOwner(player);
        // Mek starts at cliff top (0,0), elevation 0 (standing on the cliff hex floor at
        // absolute alt +3). Facing south toward the adjacent water hex (0,1).
        MovePath movePath = getMovePathFor(mek, 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Edge step from cliff top into the adjacent lower hex must compile (with leaping "
                    + "on, the leap rules cover it; the test exercises the SERVER-side edge "
                    + "climb-down handling once the path is committed).");
        // Pre-condition: edge climb-down — the client's edge-descent dialog pushes the chosen
        // level count via sendUpdateEntity before committing the path. The server distinguishes
        // climb-down from dangle by climbingLevelsChosen > 0.
        mek.setClimbingLevelsChosen(3);
        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        Coords waterHex = new Coords(0, 1);
        assertEquals(waterHex, mek.getPosition(),
              "Edge climb-down moves the Mek into the lower (water) hex.");
        assertEquals(0, mek.getElevation(),
              "A 3-level edge climb-down from cliff (absolute +3) into water:2 (level 0) ends "
                    + "at relative elevation 0 — the water surface.");
        assertTrue(mek.isClimbing(),
              "Climbing flag MUST stay set at water surface: floor=-2 is below hex level, so "
                    + "elev 0 isn't ground — the Mek is clinging at the surface. Without this the "
                    + "continue-climbing dialog won't fire next turn and the player can't pick Drop "
                    + "to sink into the water.");
        assertEquals(0, mek.getClimbingLevelsChosen(),
              "Chosen-levels must reset after the descent commits so next turn's dialog starts "
                    + "fresh.");
    }

    // -----------------------------------------------------------------------
    // Continuation climb-down past water surface to floor. After an edge
    // descent leaves the Mek clinging at water surface (elev 0 in water:2),
    // the player can pick Climb Down on the next turn to keep descending the
    // submerged cliff face. The descent extends to the actual hex floor
    // (water bottom for water hexes), not the hardcoded elev-0 ground stop
    // that used to wrongly clamp here.
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Continuation climb UP off a CLIFF FACE while clinging above water. This
    // is the user-reported "Round 11/12 cliff climb" bug: Mek partial-climbed
    // a cliff out of deep water, ended Round 11 clinging at elev 1 in the
    // water hex (climbing=true). On Round 12 the continue-climbing dialog
    // fires and the player picks the remaining levels — but the Mek doesn't
    // move. Root cause: MoveStep.compile used Entity.calcElevation for
    // continuation FORWARDS steps, and the water-emergence math in that
    // method adds the source hex's water depth to the resolved elevation
    // when the dest hex has no water. For a Mek clinging mid-cliff above
    // water that produced step.elevation = 2 (instead of 0 = cliff top),
    // which Entity.isElevationValid then rejected in a plain cliff hex (no
    // BUILDING/BRIDGE to legitimize the mid-air elevation), and the step
    // was stripped as MOVE_ILLEGAL. Fix: continuation FORWARDS climbs use
    // ClimbingHelper.getClimbDestinationElevation directly so they land on
    // the destination hex's top climbable surface (cliff top / bridge / roof)
    // without the water adjustment.
    // -----------------------------------------------------------------------

    @Test
    void continuationClimbUpCliffFromWaterSurfaceReachesTop() throws Exception {
        setBoard("BOARD_DEEP_WATER_BELOW_CLIFF");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        // Mek mid-climb at elev 1 in the water hex (clinging to the adjacent cliff face above
        // the water surface). 2 levels remaining to reach cliff top (cliff hex level 3,
        // currentAbsolute = 0+1 = 1, so totalLevelsRemaining = 3 − 1 = 2).
        Mek mek = createClimbableMek();
        mek.setOwner(player);
        mek.setClimbing(true);
        mek.setClimbingLevelsChosen(2);
        MovePath movePath = getMovePathFor(mek, 1, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Continuation FORWARDS step onto the adjacent cliff hex must compile legally. "
                    + "Pre-fix MoveStep.compile passed the step through Entity.calcElevation "
                    + "with calcElev=0, the water-emergence block added the source water depth "
                    + "(+2), and isElevationValid rejected the resulting step.elevation of 2 in "
                    + "a plain cliff destination hex.");
        // Pre-condition: step ends at cliff top (relative elev 0 in cliff hex).
        assertEquals(0, movePath.getLastStep().getElevation(),
              "Continuation climb step must resolve to relative elev 0 in the cliff hex (cliff "
                    + "top). The new ClimbingHelper.getClimbDestinationElevation path returns 0 "
                    + "for a bare cliff hex with no BUILDING/BRIDGE.");
        assertTrue(movePath.getLastStep().isClimbing(),
              "Continuation step must be marked as climbing so the server runs the climb branch.");

        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        Coords cliffHex = new Coords(0, 1);
        assertEquals(cliffHex, mek.getPosition(),
              "Completing the climb must move the Mek into the cliff hex (it was clinging at "
                    + "the source water hex during the climb).");
        assertEquals(0, mek.getElevation(),
              "The Mek must arrive at cliff top (relative elev 0 in the cliff hex = absolute "
                    + "altitude 3).");
        assertFalse(mek.isClimbing(),
              "Climb completed — climbing flag must clear.");
    }

    @Test
    void edgeClimbDownIntoWaterDescendsAllTheWayToFloor() throws Exception {
        // Regression for the in-game bridge-to-water scenario: Mek on a level-3 cliff steps
        // off the edge into adjacent water:2 hex and picks the full 5-level descent in the
        // edge-descent dialog. Server-side cap was previously `cliffTopAlt - destHex.getLevel()`
        // = 3 - 0 = 3, clamping the descent at the water surface even when the dialog correctly
        // reported a 5-level drop to the water floor and the player picked 5. The fix uses
        // `destHex.floor()` so the cap matches the dialog.
        setBoard("BOARD_CLIFF_ABOVE_DEEP_WATER");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        Mek mek = createClimbableMek();
        mek.setOwner(player);
        // Bump the test Mek's walk MP so the full 5-level descent fits in one turn
        // (5 × 2 MP/arm = 10 MP). The default 8 walk MP wouldn't cover it; the goal here
        // is to validate the server's drop cap, not the per-turn MP budget.
        mek.setOriginalWalkMP(12);
        MovePath movePath = getMovePathFor(mek, 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Edge step compiles legally so the test can exercise the server-side cap.");
        // Cliff top (abs +3) to water bottom (abs -2) = 5-level descent. Pre-fix the server
        // capped this at 3.
        mek.setClimbingLevelsChosen(5);
        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        Coords waterHex = new Coords(0, 1);
        assertEquals(waterHex, mek.getPosition(),
              "Edge climb-down moves the Mek into the lower (water) hex.");
        assertEquals(-2, mek.getElevation(),
              "Picking the full 5-level drop must descend all the way to the water FLOOR "
                    + "(-2), not stop at the surface (0). Pre-fix the server's totalDrop used "
                    + "destHex.getLevel() and clamped this to 3 levels.");
        assertFalse(mek.isClimbing(),
              "At the actual hex floor the descent is finished — climbing flag clears.");
        // 5 PSRs should have been rolled (one per descended level).
        verify(gameManager, times(5)).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());
    }

    @Test
    void continuationClimbDownPastWaterSurfaceReachesFloor() throws Exception {
        setBoard("BOARD_DEEP_WATER_BELOW_CLIFF");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        // Mek already mid-descent at water surface (elev 0 in the water:2 hex), facing the
        // cliff. Simulates "turn 2" of an edge-descent flow: the previous turn descended off
        // the cliff and stopped clinging at water surface with climbing=true preserved.
        Mek mek = createClimbableMek();
        mek.setOwner(player);
        mek.setClimbing(true);
        // Path: CLIMB_MODE_ON + 2 DOWN steps = controlled climb-down 2 levels.
        // (Distinguished server-side from dangle/drop by the CLIMB_MODE_ON marker.)
        // 2 levels at 2 MP/arm = 4 MP, well under the Mek's 8 walk MP.
        mek.setClimbingLevelsChosen(2);
        MovePath movePath = getMovePathFor(mek, 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.DOWN,
              MoveStepType.DOWN);
        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        // 2-level climb-down from water surface (elev 0) lands at water floor (elev -2).
        assertEquals(-2, mek.getElevation(),
              "Continuation climb-down must descend past elev 0 into the water column down to "
                    + "the hex floor (water:2 → floor at -2). The pre-fix hardcoded floor of 0 "
                    + "blocked this and clamped the Mek at the water surface.");
        assertFalse(mek.isClimbing(),
              "At the water floor the Mek has finished descending — climbing flag must clear "
                    + "via the entityHasReachedFloor check.");
        assertEquals(0, mek.getClimbingLevelsChosen(),
              "Chosen-levels must reset after the descent commits.");
    }

    // -----------------------------------------------------------------------
    // Matrix coverage: the climbing system supports CLIFFS, BRIDGES, and
    // BUILDINGS. Each feature must work for the major actions — fresh climb
    // up, continuation climb up, edge descent (climb-down / dangle / drop),
    // and cling. The tests above cover most cliff and bridge scenarios; the
    // three below fill the remaining gaps: fresh cliff climb up (basic
    // sanity), building continuation climb up, and building roof edge
    // dangle (verifies the server's edge-dangle branch handles building
    // roofs the same way it handles cliff and bridge edges).
    // -----------------------------------------------------------------------

    @Test
    void freshClimbUpCliffFromGroundReachesTop() throws Exception {
        setBoard("BOARD_DEEP_WATER_BELOW_CLIFF");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        // Mek standing on the water hex bottom (elev -2), fresh climb up the adjacent cliff
        // (level 3). Total climb = absolute -2 → +3 = 5 levels. Bumped walk MP so the full
        // 5-level climb (10 MP) fits in one turn for the test.
        Mek mek = createClimbableMek();
        mek.setOwner(player);
        mek.setOriginalWalkMP(12);
        MovePath movePath = getMovePathFor(mek, -2, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Fresh climb up a cliff from water bottom must compile as a legal climbing step.");
        assertTrue(movePath.getLastStep().isClimbing(),
              "Step must be marked climbing (delta of 5 exceeds maxElevationChange and TacOps "
                    + "climbing is enabled with a climbable Mek).");

        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        Coords cliffHex = new Coords(0, 1);
        assertEquals(cliffHex, mek.getPosition(),
              "Full fresh climb lands the Mek on the cliff hex.");
        assertEquals(0, mek.getElevation(),
              "Mek lands at cliff top (relative elev 0 in the level-3 cliff hex).");
        assertFalse(mek.isClimbing(),
              "Full climb completes — climbing flag clears.");
    }

    @Test
    void continuationClimbUpBuildingCompletes() throws Exception {
        setBoard("BOARD_5_FLOOR_BUILDING");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        // Mek already mid-climb at elev 3 on the side of a 5-floor building (source hex (0,0)
        // adjacent to the building hex (0,1)). 2 levels remaining to reach roof.
        Mek mek = createClimbableMek();
        mek.setOwner(player);
        mek.setOriginalWalkMP(12);
        mek.setClimbing(true);
        mek.setClimbingLevelsChosen(2);
        MovePath movePath = getMovePathFor(mek, 3, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Building continuation climb step must compile legally — the MoveStep continuation "
                    + "branch lands the Mek on the building roof via ClimbingHelper.");
        assertEquals(5, movePath.getLastStep().getElevation(),
              "Continuation step lands on the building roof (BLDG_ELEV = 5 in the building hex).");

        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(),
              any(Coords.class), any(Coords.class), any(PilotingRollData.class), anyBoolean());

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        Coords buildingHex = new Coords(0, 1);
        assertEquals(buildingHex, mek.getPosition(),
              "Full continuation climb enters the building hex.");
        assertEquals(5, mek.getElevation(),
              "Mek arrives at building roof (BLDG_ELEV).");
        assertFalse(mek.isClimbing(),
              "Climb completes — flag clears.");
    }

    @Test
    void edgeDangleOffBuildingRoofLandsAtDangleElevation() throws Exception {
        setBoard("BOARD_5_FLOOR_BUILDING");
        enableTacOpsClimbing();

        TWGameManager gameManager = spy(new TWGameManager());
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        // Mek on the roof of a 5-floor building. Edge dangle off the side into the adjacent
        // empty ground hex. Drop height = 5 (roof to ground) which clears the 3-level edge
        // threshold; dangle drops by DANGLE_LEVELS_PER_TURN (2) so Mek ends at elev 3 in the
        // adjacent hex dangling on the building wall. Tests the edge-dangle server branch for
        // a building edge (the matching cliff/bridge edges are covered above).
        Mek mek = createClimbableMek();
        mek.setOwner(player);
        // Place on roof: getMovePathFor's initializeUnit puts the Mek at (0,0); we want it on
        // the building hex (0,1) at elev 5. Easiest is to swap board orientation, but a simpler
        // alternative: explicitly position before building the path. We need the dangle path
        // (single FORWARDS from building hex (0,1) into adjacent (0,0)) so the Mek must start
        // on the building side. Place it manually after initializeUnit by reusing the path
        // entity but overriding position.
        MovePath movePath = getMovePathFor(mek, 5, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        // initializeUnit placed the Mek at (0,0) facing 3 (south, toward the building (0,1)).
        // For this test we need the opposite: Mek on the building (0,1) facing north toward the
        // empty hex (0,0). Override position/facing and rebuild the path. The empty hex (0,0)
        // is north of (0,1) (facing 0). Default facing is 3, so the FORWARDS step from (0,0)
        // already goes south to (0,1) — we want it the other way. Rebuild around the entity.
        mek.setPosition(new Coords(0, 1));
        mek.setFacing(0);
        mek.setElevation(5);
        // Build a fresh path: CLIMB_MODE_ON + FORWARDS from (0,1) facing 0 → (0,0).
        movePath = new MovePath(getGame(), mek);
        movePath.addStep(MoveStepType.CLIMB_MODE_ON);
        movePath.addStep(MoveStepType.FORWARDS);

        MovePathHandler handler = new MovePathHandler(gameManager, mek, movePath, null);
        handler.processMovement();

        Coords groundHex = new Coords(0, 0);
        assertEquals(groundHex, mek.getPosition(),
              "Edge dangle moves the Mek into the lower adjacent hex.");
        assertEquals(3, mek.getElevation(),
              "Roof at +5 minus DANGLE_LEVELS_PER_TURN (2) puts the Mek at relative elev 3 in "
                    + "the ground hex — dangling on the building wall.");
        assertTrue(mek.isDangling(),
              "Dangling flag must be set after an edge dangle that doesn't reach the floor.");
        // Note: Entity.isClimbing() returns (climbing || dangling) by design, so it's true here
        // even though the underlying climbing field is false. The dangling assertion above is
        // the meaningful one — we're testing the dangle path, not climbing.
    }

    @Test
    void continuationClimbOntoBridgeDoesNotChargeWaterEntryMP() {
        setBoard("BOARD_BRIDGE_OVER_DEEP_WATER");
        enableTacOpsClimbing();
        Player player = new Player(0, "Test");
        getGame().addPlayer(0, player);

        // Mek that's already mid-climb at water-surface (elevation 0) in the SOURCE hex,
        // facing the adjacent bridge hex. This is the state the partial-climb branch
        // leaves the Mek in after turn 1 of a 4-level climb out of deep water.
        Mek mek = createClimbableMek();
        mek.setOwner(player);
        mek.setClimbing(true);
        MovePath movePath = getMovePathFor(mek, 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Continuing the climb onto the adjacent bridge hex must compile as a legal step.");
        MoveStep climbStep = movePath.getLastStep();
        assertTrue(climbStep.isClimbing(),
              "Inheriting climbing=true from the prev step means this continuation FORWARDS is "
                    + "a climb step.");
        // Cost breakdown: 1 (base entry) + 4 (climb 2 levels × 2 MP/arm) = 5.
        // No water-entry MP because the Mek ends on the bridge surface (elev +2), above the
        // water column. Pre-fix the water cost was added (1 + 2 + 4 = 7 with PLAYTEST2,
        // 1 + 3 + 4 = 8 without).
        assertEquals(5, climbStep.getMp(),
              "FORWARDS onto bridge surface must cost 5 MP (1 base + 4 climb), not 7 — the "
                    + "Mek isn't wading through the water on this step.");
    }
}
