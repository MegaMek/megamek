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

package megamek.client.ui.panels.phaseDisplay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay.DeploymentPosition;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the deployment decisions {@link DeploymentHelper} makes without asking the player anything. Space maps have
 * no elevations, and walk-on deployment reaching the elevation lookup on one crashed the client (#9015).
 */
@DisplayName("DeploymentHelper Unit Tests")
class DeploymentHelperTest {

    private static final int LOBBY_ALTITUDE = 5;
    private static final int LOBBY_VELOCITY = 3;
    private static final int LOBBY_FACING = 2;
    private static final Coords DEPLOY_COORDS = new Coords(3, 4);

    private ClientGUI mockClientGUI;
    private DeploymentHelper deploymentHelper;
    private Board spaceBoard;
    private Board groundBoard;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        mockClientGUI = mock(ClientGUI.class);
        deploymentHelper = new DeploymentHelper(mockClientGUI);
        spaceBoard = mock(Board.class);
        when(spaceBoard.isSpace()).thenReturn(true);
        groundBoard = mock(Board.class);
        when(groundBoard.isSpace()).thenReturn(false);
    }

    private AeroSpaceFighter fighterAsSetUpInLobby() {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setAltitude(LOBBY_ALTITUDE);
        fighter.setFacing(LOBBY_FACING);
        fighter.setCurrentVelocity(LOBBY_VELOCITY);
        fighter.setNextVelocity(LOBBY_VELOCITY);
        return fighter;
    }

    @Test
    @DisplayName("#9015: a fighter deploying onto a space map keeps its altitude and facing instead of crashing")
    void spaceMapDeploymentKeepsAltitudeAndFacingWithoutAskingForAnElevation() {
        AeroSpaceFighter fighter = fighterAsSetUpInLobby();

        DeploymentPosition deploymentPosition = assertDoesNotThrow(() ->
              deploymentHelper.determineDeploymentPosition(fighter, DEPLOY_COORDS, spaceBoard, new HashSet<>(), null));

        assertNotNull(deploymentPosition, "A space map deployment must not be treated as cancelled");
        assertEquals(LOBBY_ALTITUDE, deploymentPosition.elevation());
        assertEquals(LOBBY_FACING, deploymentPosition.facing());
        verifyNoInteractions(mockClientGUI);
    }

    @Test
    @DisplayName("#9015: a ground unit on a space map keeps its elevation instead of crashing")
    void spaceMapDeploymentKeepsElevationOfNonAerospaceUnit() {
        BipedMek mek = new BipedMek();
        mek.setElevation(0);
        mek.setFacing(LOBBY_FACING);

        DeploymentPosition deploymentPosition = assertDoesNotThrow(() ->
              deploymentHelper.determineDeploymentPosition(mek, DEPLOY_COORDS, spaceBoard, new HashSet<>(), null));

        assertNotNull(deploymentPosition);
        assertEquals(0, deploymentPosition.elevation());
        assertEquals(LOBBY_FACING, deploymentPosition.facing());
    }

    @Test
    @DisplayName("Altitude 0 on a space map never lands the fighter, so it keeps its lobby velocity")
    void altitudeZeroOnSpaceMapDoesNotLandTheFighter() {
        AeroSpaceFighter fighter = fighterAsSetUpInLobby();

        deploymentHelper.applyDeploymentElevation(fighter, spaceBoard, 0);

        assertEquals(LOBBY_VELOCITY, fighter.getCurrentVelocity());
        assertEquals(LOBBY_VELOCITY, fighter.getNextVelocity());
        assertEquals(EntityMovementMode.AERODYNE, fighter.getMovementMode());
    }

    @Test
    @DisplayName("Altitude 0 on a ground map still lands the fighter")
    void altitudeZeroOnGroundMapLandsTheFighter() {
        AeroSpaceFighter fighter = fighterAsSetUpInLobby();

        deploymentHelper.applyDeploymentElevation(fighter, groundBoard, 0);

        assertEquals(0, fighter.getCurrentVelocity());
        assertEquals(EntityMovementMode.WHEELED, fighter.getMovementMode());
        assertFalse(fighter.isAirborne());
    }

    @Test
    @DisplayName("A chosen altitude above 0 keeps the fighter airborne at that altitude")
    void chosenAltitudeKeepsTheFighterAirborne() {
        AeroSpaceFighter fighter = fighterAsSetUpInLobby();

        deploymentHelper.applyDeploymentElevation(fighter, groundBoard, 7);

        assertEquals(7, fighter.getAltitude());
        assertEquals(LOBBY_VELOCITY, fighter.getCurrentVelocity());
        assertTrue(fighter.isAirborne());
    }

    @Test
    @DisplayName("A ground unit gets the chosen elevation and is never treated as an aerospace unit")
    void groundUnitGetsTheChosenElevation() {
        BipedMek mek = new BipedMek();

        deploymentHelper.applyDeploymentElevation(mek, groundBoard, 2);

        assertEquals(2, mek.getElevation());
    }
}
