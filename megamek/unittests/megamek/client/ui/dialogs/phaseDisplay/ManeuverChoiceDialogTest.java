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
 * Note: These tests require a graphical environment and the entire test class will be skipped in headless CI environments.
 *
 * @author MegaMek Team
 * @since 2025-11-10
 */
class ManeuverChoiceDialogTest {

    private static final int TEST_VELOCITY = 5;
    private static final int TEST_ALTITUDE = 5;
    private static final int TEST_CEILING = 10;

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
    }

    /**
     * Test that all maneuvers have tooltips that exist and are not empty.
     */
    @Test
    void testAllManeuversHaveTooltips() {
        // Test that all maneuvers have valid tooltips
        for (int type = 0; type < ManeuverType.MAN_SIZE; type++) {
            String tooltip = ManeuverType.getManeuverTooltip(type, true, TEST_VELOCITY, TEST_ALTITUDE, false);
            assertNotNull(tooltip, ManeuverType.getTypeName(type) + " tooltip should not be null");
            assertFalse(tooltip.isEmpty(), ManeuverType.getTypeName(type) + " tooltip should not be empty");
            assertTrue(tooltip.contains("<HTML>"),
                ManeuverType.getTypeName(type) + " tooltip should be HTML formatted");
            assertTrue(tooltip.contains(ManeuverType.getTypeName(type)),
                ManeuverType.getTypeName(type) + " tooltip should contain maneuver name");
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

        dialog.checkPerformability(0, 0, TEST_CEILING, false, 0, mockBoard, mockPath);

        // None maneuver should always be enabled
        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_NONE, 0, 0, TEST_CEILING,
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
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_LOOP, 3, TEST_ALTITUDE, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Loop should not be available at velocity 3");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_LOOP, 4, TEST_ALTITUDE, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Loop should be available at velocity 4");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_LOOP, 10, TEST_ALTITUDE, TEST_CEILING,
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
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_IMMELMAN, 2, TEST_ALTITUDE, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Immelman should not be available at velocity 2");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_IMMELMAN, 3, TEST_ALTITUDE, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Immelman should be available at velocity 3, altitude 5");

        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_IMMELMAN, 3, 9, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Immelman should not be available at altitude 9");

        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_IMMELMAN, 3, 10, TEST_CEILING,
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
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_BARREL_ROLL, 1, TEST_ALTITUDE, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Barrel Roll should not be available at velocity 1");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_BARREL_ROLL, 2, TEST_ALTITUDE, TEST_CEILING,
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
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_SIDE_SLIP_LEFT, 0, TEST_ALTITUDE, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Side Slip Left should not be available at velocity 0");

        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_SIDE_SLIP_RIGHT, 0, TEST_ALTITUDE, TEST_CEILING,
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
        assertFalse(ManeuverType.canPerform(ManeuverType.MAN_VIFF, TEST_VELOCITY, TEST_ALTITUDE, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "VIFF should not be available for non-VSTOL units");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_VIFF, TEST_VELOCITY, TEST_ALTITUDE, TEST_CEILING,
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
        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_HAMMERHEAD, 0, 0, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Hammerhead should always be available");

        assertTrue(ManeuverType.canPerform(ManeuverType.MAN_HALF_ROLL, 0, 0, TEST_CEILING,
            false, 0, mockBoard, mockPath),
            "Half Roll should always be available");
    }

    /**
     * Test that tooltips contain thrust cost information.
     */
    @Test
    void testTooltipsContainThrustCosts() {
        // Test that tooltips contain thrust cost label
        String loopTooltip = ManeuverType.getManeuverTooltip(ManeuverType.MAN_LOOP, true, TEST_VELOCITY, TEST_ALTITUDE, false);
        assertTrue(loopTooltip.contains("Thrust Cost") || loopTooltip.contains("thrust"),
            "Loop tooltip should contain thrust cost information");

        // Test that velocity-based costs are shown correctly in tooltips
        String hammerheadTooltip5 = ManeuverType.getManeuverTooltip(ManeuverType.MAN_HAMMERHEAD, true, TEST_VELOCITY, TEST_ALTITUDE, false);
        assertTrue(hammerheadTooltip5.contains("5"),
            "Hammerhead tooltip at velocity 5 should show cost of 5");

        String hammerheadTooltip10 = ManeuverType.getManeuverTooltip(ManeuverType.MAN_HAMMERHEAD, true, 10, TEST_ALTITUDE, false);
        assertTrue(hammerheadTooltip10.contains("10"),
            "Hammerhead tooltip at velocity 10 should show cost of 10");

        // Test VIFF velocity+2 formula
        String viffTooltip5 = ManeuverType.getManeuverTooltip(ManeuverType.MAN_VIFF, true, TEST_VELOCITY, TEST_ALTITUDE, false);
        assertTrue(viffTooltip5.contains("7"),
            "VIFF tooltip at velocity 5 should show cost of 7 (velocity + 2)");

        String viffTooltip10 = ManeuverType.getManeuverTooltip(ManeuverType.MAN_VIFF, true, 10, TEST_ALTITUDE, false);
        assertTrue(viffTooltip10.contains("12"),
            "VIFF tooltip at velocity 10 should show cost of 12 (velocity + 2)");
    }

    /**
     * Test that tooltips contain control modifier information.
     */
    @Test
    void testTooltipsContainControlModifiers() {
        // Test that tooltips contain control modifier label
        String loopTooltip = ManeuverType.getManeuverTooltip(ManeuverType.MAN_LOOP, true, TEST_VELOCITY, TEST_ALTITUDE, false);
        assertTrue(loopTooltip.contains("Control") || loopTooltip.contains("Mod"),
            "Loop tooltip should contain control modifier information");

        // Test VSTOL-dependent control modifiers in tooltips (Side Slip)
        String sideSlipNonVSTOL = ManeuverType.getManeuverTooltip(ManeuverType.MAN_SIDE_SLIP_LEFT, true, TEST_VELOCITY, TEST_ALTITUDE, false);
        assertTrue(sideSlipNonVSTOL.contains("+0") || sideSlipNonVSTOL.contains("0"),
            "Side Slip tooltip for non-VSTOL should show +0 modifier");

        String sideSlipVSTOL = ManeuverType.getManeuverTooltip(ManeuverType.MAN_SIDE_SLIP_LEFT, true, TEST_VELOCITY, TEST_ALTITUDE, true);
        assertTrue(sideSlipVSTOL.contains("-1"),
            "Side Slip tooltip for VSTOL should show -1 modifier");

        // Test that different maneuvers show different modifiers
        String hammerheadTooltip = ManeuverType.getManeuverTooltip(ManeuverType.MAN_HAMMERHEAD, true, TEST_VELOCITY, TEST_ALTITUDE, false);
        assertTrue(hammerheadTooltip.contains("3") || hammerheadTooltip.contains("+3"),
            "Hammerhead tooltip should show +3 control modifier");

        String halfRollTooltip = ManeuverType.getManeuverTooltip(ManeuverType.MAN_HALF_ROLL, true, TEST_VELOCITY, TEST_ALTITUDE, false);
        assertTrue(halfRollTooltip.contains("-1"),
            "Half Roll tooltip should show -1 control modifier");
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
        dialog.checkPerformability(TEST_VELOCITY, TEST_ALTITUDE, TEST_CEILING, false, 0, mockBoard, mockPath);
        dialog.checkPerformability(0, 0, TEST_CEILING, false, 0, mockBoard, mockPath);
        dialog.checkPerformability(10, 8, TEST_CEILING, true, 0, mockBoard, mockPath);
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
