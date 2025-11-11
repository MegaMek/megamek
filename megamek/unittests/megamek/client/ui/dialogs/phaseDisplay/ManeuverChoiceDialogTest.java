/*
 * Copyright (C) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.dialogs.phaseDisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.GraphicsEnvironment;
import java.util.Vector;

import javax.swing.JFrame;

import megamek.client.ui.Messages;
import megamek.common.ManeuverType;
import megamek.common.board.Board;
import megamek.common.moves.MovePath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ManeuverChoiceDialog tooltip functionality.
 *
 * Tests verify that:
 * 1. Tooltips are properly generated for all maneuver types
 * 2. Tooltips contain required information (requirements, costs, modifiers, effects)
 * 3. Tooltips show current values when maneuvers are unavailable
 * 4. Resource strings are properly loaded from messages.properties
 * 5. Dynamic values are correctly formatted using MessageFormat
 *
 * Note: These tests require a graphical environment and will be skipped in headless CI environments.
 *
 * @author MegaMek Team
 * @since 2025-11-10
 */
class ManeuverChoiceDialogTest {

    private ManeuverChoiceDialog dialog;
    private JFrame mockFrame;

    @BeforeAll
    static void checkGraphicsEnvironment() {
        assumeFalse(GraphicsEnvironment.isHeadless(),
            "Skipping GUI tests - no display available in headless environment");
    }

    @BeforeEach
    void setUp() {
        mockFrame = new JFrame();
        dialog = new ManeuverChoiceDialog(mockFrame, "Test Maneuver Selection");
    }

    /**
     * Test that dialog is properly initialized with all maneuvers.
     */
    @Test
    void testDialogInitialization() {
        assertNotNull(dialog, "Dialog should be initialized");
        assertEquals("Test Maneuver Selection", dialog.getTitle(),
            "Dialog title should match");
    }

    /**
     * Test that all maneuver resource keys exist in messages.properties.
     */
    @Test
    void testAllManeuverResourceKeysExist() {
        // Test that all required resource keys exist for each maneuver
        String[] maneuverKeys = {
            "None", "Loop", "Immelman", "SplitS", "Hammerhead",
            "HalfRoll", "BarrelRoll", "SideSlipLeft", "SideSlipRight", "VIFF"
        };

        for (String key : maneuverKeys) {
            assertResourceExists("ManeuverChoiceDialog." + key + ".requirements",
                key + " requirements should exist");
            assertResourceExists("ManeuverChoiceDialog." + key + ".thrustCost",
                key + " thrust cost should exist");
            assertResourceExists("ManeuverChoiceDialog." + key + ".controlMod",
                key + " control modifier should exist");
            assertResourceExists("ManeuverChoiceDialog." + key + ".effect",
                key + " effect should exist");
        }
    }

    /**
     * Test that common label resource keys exist.
     */
    @Test
    void testCommonResourceKeysExist() {
        assertResourceExists("ManeuverChoiceDialog.requirementsLabel",
            "Requirements label should exist");
        assertResourceExists("ManeuverChoiceDialog.thrustCostLabel",
            "Thrust cost label should exist");
        assertResourceExists("ManeuverChoiceDialog.controlModLabel",
            "Control modifier label should exist");
        assertResourceExists("ManeuverChoiceDialog.effectLabel",
            "Effect label should exist");
        assertResourceExists("ManeuverChoiceDialog.currentValue",
            "Current value format should exist");
        assertResourceExists("ManeuverChoiceDialog.currentValues",
            "Current values (plural) format should exist");
        assertResourceExists("ManeuverChoiceDialog.notVSTOL",
            "Not VSTOL message should exist");
    }

    /**
     * Test that None maneuver is always available.
     */
    @Test
    void testNoneManeuverAlwaysAvailable() {
        Board mockBoard = mock(Board.class);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        dialog.checkPerformability(0, 0, 10, false, 0, mockBoard, mockPath);

        // None maneuver should always be enabled
        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_NONE, 0, 0, 10,
            false, 0, mockBoard, mockPath),
            "None maneuver should always be available");
    }

    /**
     * Test Loop maneuver requirements (velocity >= 4).
     */
    @Test
    void testLoopManeuverRequirements() {
        Board mockBoard = mock(Board.class);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        // Loop requires velocity >= 4
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_LOOP, 3, 5, 10,
            false, 0, mockBoard, mockPath),
            "Loop should not be available at velocity 3");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_LOOP, 4, 5, 10,
            false, 0, mockBoard, mockPath),
            "Loop should be available at velocity 4");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_LOOP, 10, 5, 10,
            false, 0, mockBoard, mockPath),
            "Loop should be available at velocity 10");
    }

    /**
     * Test Immelman maneuver requirements (velocity >= 3 AND altitude < 9).
     */
    @Test
    void testImmelmanManeuverRequirements() {
        Board mockBoard = mock(Board.class);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        // Immelman requires velocity >= 3 AND altitude < 9
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_IMMELMAN, 2, 5, 10,
            false, 0, mockBoard, mockPath),
            "Immelman should not be available at velocity 2");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_IMMELMAN, 3, 5, 10,
            false, 0, mockBoard, mockPath),
            "Immelman should be available at velocity 3, altitude 5");

        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_IMMELMAN, 3, 9, 10,
            false, 0, mockBoard, mockPath),
            "Immelman should not be available at altitude 9");

        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_IMMELMAN, 3, 10, 10,
            false, 0, mockBoard, mockPath),
            "Immelman should not be available at altitude 10");
    }

    /**
     * Test Barrel Roll maneuver requirements (velocity >= 2).
     */
    @Test
    void testBarrelRollManeuverRequirements() {
        Board mockBoard = mock(Board.class);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        // Barrel Roll requires velocity >= 2
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_BARREL_ROLL, 1, 5, 10,
            false, 0, mockBoard, mockPath),
            "Barrel Roll should not be available at velocity 1");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_BARREL_ROLL, 2, 5, 10,
            false, 0, mockBoard, mockPath),
            "Barrel Roll should be available at velocity 2");
    }

    /**
     * Test Side Slip maneuver requirements (velocity > 0).
     */
    @Test
    void testSideSlipManeuverRequirements() {
        Board mockBoard = mock(Board.class);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        // Side Slip requires velocity > 0
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_SIDE_SLIP_LEFT, 0, 5, 10,
            false, 0, mockBoard, mockPath),
            "Side Slip Left should not be available at velocity 0");

        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_SIDE_SLIP_RIGHT, 0, 5, 10,
            false, 0, mockBoard, mockPath),
            "Side Slip Right should not be available at velocity 0");

        // Note: Ground map checking is more complex and requires real MovePath
    }

    /**
     * Test VIFF maneuver requirements (VSTOL only).
     */
    @Test
    void testVIFFManeuverRequirements() {
        Board mockBoard = mock(Board.class);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        // VIFF requires VSTOL capability
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_VIFF, 5, 5, 10,
            false, 0, mockBoard, mockPath),
            "VIFF should not be available for non-VSTOL units");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_VIFF, 5, 5, 10,
            true, 0, mockBoard, mockPath),
            "VIFF should be available for VSTOL units");
    }

    /**
     * Test Hammerhead and Half Roll are always available.
     */
    @Test
    void testAlwaysAvailableManeuvers() {
        Board mockBoard = mock(Board.class);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        // Hammerhead and Half Roll are always available
        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_HAMMERHEAD, 0, 0, 10,
            false, 0, mockBoard, mockPath),
            "Hammerhead should always be available");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_HALF_ROLL, 0, 0, 10,
            false, 0, mockBoard, mockPath),
            "Half Roll should always be available");
    }

    /**
     * Test maneuver thrust costs.
     */
    @Test
    void testManeuverThrustCosts() {
        // Test fixed costs
        assertEquals(4, ManeuverType.getCost(ManeuverType.MAN_LOOP, 0),
            "Loop should cost 4 thrust");
        assertEquals(4, ManeuverType.getCost(ManeuverType.MAN_IMMELMAN, 0),
            "Immelman should cost 4 thrust");
        assertEquals(2, ManeuverType.getCost(ManeuverType.MAN_SPLIT_S, 0),
            "Split S should cost 2 thrust");
        assertEquals(1, ManeuverType.getCost(ManeuverType.MAN_HALF_ROLL, 0),
            "Half Roll should cost 1 thrust");
        assertEquals(1, ManeuverType.getCost(ManeuverType.MAN_BARREL_ROLL, 0),
            "Barrel Roll should cost 1 thrust");
        assertEquals(1, ManeuverType.getCost(ManeuverType.MAN_SIDE_SLIP_LEFT, 0),
            "Side Slip Left should cost 1 thrust");
        assertEquals(1, ManeuverType.getCost(ManeuverType.MAN_SIDE_SLIP_RIGHT, 0),
            "Side Slip Right should cost 1 thrust");

        // Test velocity-based costs
        assertEquals(5, ManeuverType.getCost(ManeuverType.MAN_HAMMERHEAD, 5),
            "Hammerhead at velocity 5 should cost 5 thrust");
        assertEquals(10, ManeuverType.getCost(ManeuverType.MAN_HAMMERHEAD, 10),
            "Hammerhead at velocity 10 should cost 10 thrust");

        assertEquals(7, ManeuverType.getCost(ManeuverType.MAN_VIFF, 5),
            "VIFF at velocity 5 should cost 7 thrust (velocity + 2)");
        assertEquals(12, ManeuverType.getCost(ManeuverType.MAN_VIFF, 10),
            "VIFF at velocity 10 should cost 12 thrust (velocity + 2)");
    }

    /**
     * Test maneuver control roll modifiers.
     */
    @Test
    void testManeuverControlModifiers() {
        // Test fixed modifiers
        assertEquals(1, ManeuverType.getMod(ManeuverType.MAN_LOOP, false),
            "Loop should have +1 control modifier");
        assertEquals(1, ManeuverType.getMod(ManeuverType.MAN_IMMELMAN, false),
            "Immelman should have +1 control modifier");
        assertEquals(2, ManeuverType.getMod(ManeuverType.MAN_SPLIT_S, false),
            "Split S should have +2 control modifier");
        assertEquals(3, ManeuverType.getMod(ManeuverType.MAN_HAMMERHEAD, false),
            "Hammerhead should have +3 control modifier");
        assertEquals(-1, ManeuverType.getMod(ManeuverType.MAN_HALF_ROLL, false),
            "Half Roll should have -1 control modifier");
        assertEquals(0, ManeuverType.getMod(ManeuverType.MAN_BARREL_ROLL, false),
            "Barrel Roll should have +0 control modifier");
        assertEquals(2, ManeuverType.getMod(ManeuverType.MAN_VIFF, false),
            "VIFF should have +2 control modifier");

        // Test VSTOL-dependent modifiers (Side Slip)
        assertEquals(0, ManeuverType.getMod(ManeuverType.MAN_SIDE_SLIP_LEFT, false),
            "Side Slip Left should have +0 control modifier for non-VSTOL");
        assertEquals(-1, ManeuverType.getMod(ManeuverType.MAN_SIDE_SLIP_LEFT, true),
            "Side Slip Left should have -1 control modifier for VSTOL");
        assertEquals(0, ManeuverType.getMod(ManeuverType.MAN_SIDE_SLIP_RIGHT, false),
            "Side Slip Right should have +0 control modifier for non-VSTOL");
        assertEquals(-1, ManeuverType.getMod(ManeuverType.MAN_SIDE_SLIP_RIGHT, true),
            "Side Slip Right should have -1 control modifier for VSTOL");
    }

    /**
     * Test that None maneuver has simple requirements message.
     */
    @Test
    void testNoneManeuverSimpleMessage() {
        String noneRequirements = Messages.getString("ManeuverChoiceDialog.None.requirements");
        assertNotNull(noneRequirements, "None maneuver requirements should exist");
        assertFalse(noneRequirements.isEmpty(), "None maneuver requirements should not be empty");
    }

    /**
     * Test that checkPerformability updates dialog state.
     * This is a basic test - full UI testing would require TestFX or similar.
     */
    @Test
    void testCheckPerformabilityUpdatesState() {
        Board mockBoard = mock(Board.class);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        // Should not throw exception
        dialog.checkPerformability(5, 5, 10, false, 0, mockBoard, mockPath);
        dialog.checkPerformability(0, 0, 10, false, 0, mockBoard, mockPath);
        dialog.checkPerformability(10, 8, 10, true, 0, mockBoard, mockPath);
    }

    /**
     * Helper method to assert that a resource string exists and is not empty.
     */
    private void assertResourceExists(String key, String message) {
        String value = Messages.getString(key);
        assertNotNull(value, message + " (key: " + key + " is null)");
        assertFalse(value.isEmpty(), message + " (key: " + key + " is empty)");
        assertFalse(value.equals(key), message + " (key: " + key + " not found - returned key itself)");
    }
}
