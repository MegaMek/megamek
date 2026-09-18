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
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.GdxNativesLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The keyboard turn: an eased step of one hex side that queues, reverses and leaves the presets reachable. */
class BoardCameraRotationTest {
    private static final float TOLERANCE = 0.001f;

    @BeforeAll
    static void loadMathNatives() {
        GdxNativesLoader.load();
    }

    private static BoardCamera camera(boolean isometric) {
        BoardCamera view = new BoardCamera();
        view.resize(1100, 750);
        view.setIsometric(isometric);
        return view;
    }

    private static void finishTurn(BoardCamera view) {
        view.advance(BoardCamera.ROTATION_SECONDS);
    }

    @Test
    void aTurnEasesThroughTheStepInsteadOfJumping() {
        BoardCamera view = camera(false);
        view.rotateStep(1);
        assertEquals(0, view.azimuth(), TOLERANCE, "Nothing moves until a frame is played");
        assertTrue(view.isRotating());

        view.advance(BoardCamera.ROTATION_SECONDS / 2);
        assertEquals(BoardCamera.ROTATION_STEP / 2, view.azimuth(), TOLERANCE);
        assertTrue(view.isRotating());

        finishTurn(view);
        assertEquals(BoardCamera.ROTATION_STEP, view.azimuth());
        assertFalse(view.isRotating());
    }

    @Test
    void sixTurnsReturnExactlyToTheIsometricPreset() {
        BoardCamera view = camera(true);
        for (int turn = 0; turn < 6; turn++) {
            view.rotateStep(1);
            // An uneven frame time must not leave rounding drift behind.
            view.advance(0.07f);
            view.advance(0.11f);
            finishTurn(view);
        }
        assertTrue(view.isIsometric());
    }

    @Test
    void aLeftTurnWrapsBelowZero() {
        BoardCamera view = camera(false);
        view.rotateStep(-1);
        finishTurn(view);
        assertEquals(300, view.azimuth());
    }

    @Test
    void aSecondTapDuringATurnQueuesAnotherStep() {
        BoardCamera view = camera(false);
        view.rotateStep(1);
        view.advance(0.1f);
        view.rotateStep(1);
        finishTurn(view);
        assertEquals(2 * BoardCamera.ROTATION_STEP, view.azimuth());
    }

    @Test
    void anOppositeTapDuringATurnGoesBackToTheStart() {
        BoardCamera view = camera(true);
        view.rotateStep(1);
        view.advance(0.1f);
        float partWay = view.azimuth();
        view.rotateStep(-1);
        view.advance(BoardCamera.ROTATION_SECONDS / 2);
        assertTrue(view.azimuth() < partWay, "The camera must turn back, not carry on round the long way");
        finishTurn(view);
        assertTrue(view.isIsometric());
    }

    @Test
    void tiltingDoesNotInterruptATurnButAMouseOrbitDoes() {
        BoardCamera view = camera(false);
        view.rotateStep(1);
        view.advance(0.1f);
        view.tilt(20);
        assertTrue(view.isRotating());
        assertEquals(20, view.tilt(), TOLERANCE);

        view.orbit(5, 0);
        assertFalse(view.isRotating());
        float afterOrbit = view.azimuth();
        finishTurn(view);
        assertEquals(afterOrbit, view.azimuth(), "A cancelled turn must not resume");
    }

    @Test
    void choosingAPresetCancelsATurn() {
        BoardCamera view = camera(false);
        view.rotateStep(1);
        view.advance(0.1f);
        view.setIsometric(true);
        finishTurn(view);
        assertTrue(view.isIsometric());
    }
}
