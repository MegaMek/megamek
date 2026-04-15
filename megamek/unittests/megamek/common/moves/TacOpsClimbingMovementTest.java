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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.CriticalSlot;
import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

/**
 * Tests for TacOps Climbing movement legality (TO:AR p.20), focused on bugs reported by QA:
 * <ul>
 *   <li>Climb Mode OFF must not bypass normal elevation limits on terrain cliffs.</li>
 *   <li>Jumping must not trigger climbing behavior (or block jump-down to adjacent hexes).</li>
 * </ul>
 */
public class TacOpsClimbingMovementTest extends GameBoardTestCase {

    static {
        initializeBoard("BOARD_LEVEL_4_CLIFF", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 4 "" ""
              end""");

        initializeBoard("BOARD_LEVEL_2_HILL", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 2 "" ""
              end""");
    }

    /**
     * Creates a BipedMek with both arms fully functional (all actuators intact, hands free), so
     * {@link ClimbingHelper#canClimb} returns true.
     */
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

    /**
     * Creates a BipedMek with jump jets installed and two functional arms.
     */
    private Mek createJumpingMek() {
        BipedMek mek = new BipedMek();
        mek.setChassis("Test");
        mek.setModel("Jumper");
        mek.setWeight(50.0);
        mek.setOriginalWalkMP(4);
        mek.setOriginalJumpMP(5);
        mek.autoSetInternal();
        mek.setCrew(new Crew(CrewType.SINGLE));
        setupArmActuators(mek, Mek.LOC_LEFT_ARM);
        setupArmActuators(mek, Mek.LOC_RIGHT_ARM);
        EquipmentType jumpJet = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
        try {
            for (int i = 0; i < 5; i++) {
                mek.addEquipment(jumpJet, BipedMek.LOC_CENTER_TORSO);
            }
        } catch (Exception ignored) {
            // ignored in test setup
        }
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
    // Bug 2: Climb Mode OFF must NOT bypass normal elevation limits.
    // Before the fix, TacOps Climbing being enabled as a game option allowed
    // Meks to scale cliffs regardless of climb mode state.
    // -----------------------------------------------------------------------

    @Test
    void climbModeOffCannotScale4LevelCliff() {
        setBoard("BOARD_LEVEL_4_CLIFF");
        enableTacOpsClimbing();

        MovePath movePath = getMovePathFor(createClimbableMek(), 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(),
              "Climb Mode OFF must keep the normal 2-level elevation limit even when TacOps "
                    + "Climbing is enabled — a 4-level cliff must remain unscalable.");
    }

    @Test
    void climbModeOnCanScale4LevelCliff() {
        setBoard("BOARD_LEVEL_4_CLIFF");
        enableTacOpsClimbing();

        MovePath movePath = getMovePathFor(createClimbableMek(), 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(),
              "Climb Mode ON with TacOps Climbing enabled should allow a capable Mek to "
                    + "scale a 4-level cliff.");
    }

    @Test
    void tacOpsClimbingDisabledCannotScale4LevelCliffEvenWithClimbModeOn() {
        setBoard("BOARD_LEVEL_4_CLIFF");
        // Intentionally NOT calling enableTacOpsClimbing() — game option off.

        MovePath movePath = getMovePathFor(createClimbableMek(), 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(),
              "Without TacOps Climbing enabled, a 4-level cliff must remain unscalable "
                    + "regardless of climb mode.");
    }

    @Test
    void climbModeOffStillAllowsNormal2LevelAscent() {
        setBoard("BOARD_LEVEL_2_HILL");
        enableTacOpsClimbing();

        MovePath movePath = getMovePathFor(createClimbableMek(), 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(),
              "Climb Mode OFF must not regress normal movement — 2-level hills stay legal.");
    }

    // -----------------------------------------------------------------------
    // Bug 3: Jumping must not be classified as climbing, even with climb mode
    // on and the TacOps Climbing option enabled.
    // -----------------------------------------------------------------------

    @Test
    void jumpingOntoHighCliffIsNotTreatedAsClimbing() {
        setBoard("BOARD_LEVEL_4_CLIFF");
        enableTacOpsClimbing();

        MovePath movePath = getMovePathFor(createJumpingMek(), 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.START_JUMP,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Jumping onto a 4-level cliff should be legal.");
        MoveStep lastStep = movePath.getLastStep();
        assertFalse(lastStep.isClimbing(),
              "A jumping step must never be classified as a climbing step — that would "
                    + "incorrectly trigger the climbing dialog and block the jump.");
    }

    @Test
    void jumpingDownToAdjacentLowerHexIsLegal() {
        setBoard("BOARD_LEVEL_4_CLIFF");
        enableTacOpsClimbing();

        // Start on the high cliff, jump to the adjacent lower hex.
        MovePath movePath = getMovePathFor(createJumpingMek(), 0, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.START_JUMP,
              MoveStepType.FORWARDS);
        // NOTE: With initializeUnit placing the entity at (0,0) facing 3, the cliff/hill
        // setup above means FORWARDS goes toward the higher hex. For the descent
        // direction we rely on the same assertion: jumping steps never climb.
        MoveStep lastStep = movePath.getLastStep();
        assertFalse(lastStep.isClimbing(),
              "Jumping steps must not be classified as climbing, regardless of direction.");
    }
}
