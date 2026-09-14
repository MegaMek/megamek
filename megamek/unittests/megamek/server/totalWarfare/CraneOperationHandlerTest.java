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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.bays.LightVehicleBay;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BipedMek;
import megamek.common.units.CraneOperation;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Drives the End Phase lifecycle of crane loading and unloading (TW p.90-91) through
 * {@link TWGameManager#checkCraneOperations()} and {@link CraneOperationHandler}.
 * <p>
 * Layout: a flat 7 by 7 board. The DropShip is centred at (3, 3) facing north; (3, 1) is next to its north hex.
 * </p>
 */
class CraneOperationHandlerTest extends GameBoardTestCase {

    private static final Coords DROPSHIP_CENTRE = new Coords(3, 3);
    private static final Coords BESIDE_DROPSHIP = new Coords(3, 1);
    private static final int FACING_SOUTH_WEST = 4;

    private TWGameManager gameManager;
    private Game game;
    private Dropship dropShip;

    @BeforeEach
    void setUpGame() {
        StringBuilder boardData = new StringBuilder("size 7 7\n");
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                boardData.append(String.format("hex %02d%02d 0 \"\" \"\"%n", x + 1, y + 1));
            }
        }
        boardData.append("end");
        initializeBoard("CRANE_HANDLER_BOARD", boardData.toString());

        gameManager = Mockito.spy(new TWGameManager());
        // Stub the calls that need a live server connection
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());

        game = gameManager.getGame();
        Board board = getBoard("CRANE_HANDLER_BOARD");
        game.setBoard(board);
        game.addPlayer(0, new Player(0, "Test"));

        dropShip = new Dropship();
        dropShip.setId(20);
        dropShip.setOwner(game.getPlayer(0));
        dropShip.addTransporter(new LightVehicleBay(2, 1, 1));
        game.addEntity(dropShip);
        dropShip.setDeployed(true);
        dropShip.setFacing(0);
        dropShip.setAltitude(0);
        dropShip.setPosition(DROPSHIP_CENTRE);
    }

    @Test
    @DisplayName("A VTOL is aboard at the end of the fourth turn after the declaring turn (TW p.90)")
    void vtolIsLoadedAfterFourTurns() {
        VTOL vtol = placeVtol();
        new CraneOperationHandler(gameManager).declareLoad(vtol, dropShip);
        assertNotNull(dropShip.getCraneOperations().findFor(vtol.getId()), "The declaration is recorded");

        gameManager.checkCraneOperations();
        CraneOperation operation = dropShip.getCraneOperations().findFor(vtol.getId());
        assertNotNull(operation, "Still waiting after the declaring End Phase");
        assertTrue(operation.isStarted(), "The declaring End Phase starts the loading");

        for (int turn = 1; turn <= 3; turn++) {
            gameManager.checkCraneOperations();
            assertEquals(Entity.NONE, vtol.getTransportId(), "Not aboard after turn " + turn);
        }
        gameManager.checkCraneOperations();

        assertEquals(dropShip.getId(), vtol.getTransportId(), "Aboard at the end of the fourth turn");
        assertNull(vtol.getPosition(), "A loaded unit is off the board");
        assertTrue(dropShip.getCraneOperations().isEmpty(), "The finished operation is removed");
    }

    @Test
    @DisplayName("Firing a weapon while waiting cancels crane loading")
    void firingCancelsLoading() throws LocationFullException {
        VTOL vtol = placeVtol();
        vtol.addEquipment(EquipmentType.get("ISMediumLaser"), Tank.LOC_FRONT);
        new CraneOperationHandler(gameManager).declareLoad(vtol, dropShip);
        gameManager.checkCraneOperations();

        vtol.getWeaponList().getFirst().setUsedThisRound(true);
        gameManager.checkCraneOperations();

        assertNull(dropShip.getCraneOperations().findFor(vtol.getId()), "The loading is cancelled");
        assertEquals(Entity.NONE, vtol.getTransportId(), "The VTOL is not loaded");
    }

    @Test
    @DisplayName("Moving while waiting cancels crane loading")
    void movingCancelsLoading() {
        VTOL vtol = placeVtol();
        new CraneOperationHandler(gameManager).declareLoad(vtol, dropShip);
        gameManager.checkCraneOperations();

        vtol.delta_distance = 2;
        vtol.setPosition(new Coords(1, 1));
        gameManager.checkCraneOperations();

        assertNull(dropShip.getCraneOperations().findFor(vtol.getId()), "The loading is cancelled");
    }

    @Test
    @DisplayName("The DropShip lifting off cancels crane loading")
    void liftOffCancelsLoading() {
        VTOL vtol = placeVtol();
        new CraneOperationHandler(gameManager).declareLoad(vtol, dropShip);
        gameManager.checkCraneOperations();

        dropShip.setAltitude(1);
        gameManager.checkCraneOperations();

        assertNull(dropShip.getCraneOperations().findFor(vtol.getId()), "The loading is cancelled");
    }

    @Test
    @DisplayName("A VTOL out of reach cannot be declared for crane loading")
    void declarationOutOfReachIsRejected() {
        VTOL vtol = placeVtol();
        vtol.setPosition(new Coords(0, 0));

        new CraneOperationHandler(gameManager).declareLoad(vtol, dropShip);

        assertTrue(dropShip.getCraneOperations().isEmpty(), "Nothing is recorded");
    }

    @Test
    @DisplayName("A carried VTOL is placed in its chosen hex and facing at the end of the third turn (TW p.91)")
    void vtolIsUnloadedAfterThreeTurns() {
        VTOL vtol = loadedVtol();
        new CraneOperationHandler(gameManager).declareUnload(dropShip, vtol, BESIDE_DROPSHIP, FACING_SOUTH_WEST);
        assertNotNull(dropShip.getCraneOperations().findFor(vtol.getId()), "The declaration is recorded");

        gameManager.checkCraneOperations();
        gameManager.checkCraneOperations();
        gameManager.checkCraneOperations();
        assertEquals(dropShip.getId(), vtol.getTransportId(), "Still aboard after the second turn");

        gameManager.checkCraneOperations();

        assertEquals(Entity.NONE, vtol.getTransportId(), "Unloaded at the end of the third turn");
        assertEquals(BESIDE_DROPSHIP, vtol.getPosition(), "Placed in the chosen hex");
        assertEquals(FACING_SOUTH_WEST, vtol.getFacing(), "Placed with the chosen facing");
        assertTrue(dropShip.getCraneOperations().isEmpty(), "The finished operation is removed");
    }

    @Test
    @DisplayName("An unloading whose hex is blocked waits and unloads once the hex clears")
    void blockedUnloadingWaits() {
        VTOL vtol = loadedVtol();
        new CraneOperationHandler(gameManager).declareUnload(dropShip, vtol, BESIDE_DROPSHIP, FACING_SOUTH_WEST);
        // A VTOL may share a hex with one friendly Mek, so it takes two to break the stacking limit
        BipedMek firstBlocker = createBlocker(40);
        BipedMek secondBlocker = createBlocker(41);

        gameManager.checkCraneOperations();
        gameManager.checkCraneOperations();
        gameManager.checkCraneOperations();
        firstBlocker.setPosition(BESIDE_DROPSHIP);
        secondBlocker.setPosition(BESIDE_DROPSHIP);
        gameManager.checkCraneOperations();

        assertEquals(dropShip.getId(), vtol.getTransportId(), "The VTOL stays aboard while the hex is blocked");
        assertNotNull(dropShip.getCraneOperations().findFor(vtol.getId()), "The unloading keeps waiting");

        secondBlocker.setPosition(new Coords(0, 6));
        gameManager.checkCraneOperations();

        assertFalse(vtol.getTransportId() == dropShip.getId(), "Unloaded once the hex is clear");
        assertEquals(BESIDE_DROPSHIP, vtol.getPosition(), "Placed in the chosen hex");
    }

    private BipedMek createBlocker(int id) {
        BipedMek blocker = new BipedMek();
        blocker.setId(id);
        blocker.setWeight(50);
        blocker.setOwner(game.getPlayer(0));
        game.addEntity(blocker);
        blocker.setDeployed(true);
        blocker.setElevation(0);
        return blocker;
    }

    private VTOL placeVtol() {
        VTOL vtol = new VTOL();
        vtol.setId(30);
        vtol.setWeight(20);
        vtol.setOwner(game.getPlayer(0));
        game.addEntity(vtol);
        vtol.setDeployed(true);
        vtol.setElevation(1);
        vtol.setPosition(BESIDE_DROPSHIP);
        return vtol;
    }

    private VTOL loadedVtol() {
        VTOL vtol = new VTOL();
        vtol.setId(30);
        vtol.setWeight(20);
        vtol.setOwner(game.getPlayer(0));
        game.addEntity(vtol);
        dropShip.load(vtol, false, 1);
        vtol.setTransportId(dropShip.getId());
        vtol.setPosition(null);
        return vtol;
    }
}
