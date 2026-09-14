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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.bays.LightVehicleBay;
import megamek.common.bays.MekBay;
import megamek.common.board.Coords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the rules for loading and unloading VTOLs, fighters and small craft with the cranes of a grounded DropShip (TW
 * p.90-91).
 * <p>
 * Layout: a flat 7 by 7 board, except one hex raised to level 3. The DropShip is centred at (3, 3) facing north, so it
 * covers that hex and the six around it; (3, 1) is next to its north hex and (3, 0) is one hex further out.
 * </p>
 */
class CraneRulesTest extends GameBoardTestCase {

    private static final Coords DROPSHIP_CENTRE = new Coords(3, 3);
    private static final Coords BESIDE_DROPSHIP = new Coords(3, 1);
    private static final Coords TOO_FAR = new Coords(3, 0);
    private static final Coords RAISED_HEX = new Coords(5, 1);

    static {
        StringBuilder board = new StringBuilder("size 7 7\n");
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                int level = ((x == RAISED_HEX.getX()) && (y == RAISED_HEX.getY())) ? 3 : 0;
                board.append(String.format("hex %02d%02d %d \"\" \"\"%n", x + 1, y + 1, level));
            }
        }
        board.append("end");
        initializeBoard("CRANE_BOARD", board.toString());
    }

    @Test
    @DisplayName("VTOLs, fighters and small craft are crane-only; DropShips and ground units are not (TW p.90)")
    void identifiesCraneOnlyUnits() {
        assertTrue(CraneRules.isCraneOnlyUnit(new VTOL()), "VTOL");
        assertTrue(CraneRules.isCraneOnlyUnit(new AeroSpaceFighter()), "Aerospace fighter");
        assertTrue(CraneRules.isCraneOnlyUnit(new ConvFighter()), "Conventional fighter");
        assertTrue(CraneRules.isCraneOnlyUnit(new SmallCraft()), "Small craft");
        assertFalse(CraneRules.isCraneOnlyUnit(new Dropship()), "A DropShip is the carrier, not cargo");
        assertFalse(CraneRules.isCraneOnlyUnit(new BipedMek()), "Meks mount under their own power");
        assertFalse(CraneRules.isCraneOnlyUnit(new Tank()), "Ground vehicles mount under their own power");
        assertFalse(CraneRules.isCraneOnlyUnit(new ConvInfantry()), "Infantry mount under their own power");
    }

    @Test
    @DisplayName("A VTOL next to a friendly grounded DropShip with room can be loaded by crane")
    void vtolBesideDropShipIsInReach() {
        Dropship dropShip = placeDropShip(0, true);
        VTOL vtol = placeVtol(0, BESIDE_DROPSHIP, 1);

        assertTrue(CraneRules.carriersInReach(vtol, getGame()).contains(dropShip), "The DropShip should be in reach");
        assertNull(CraneRules.loadByCraneIllegalReason(vtol, dropShip, true, getGame()), "The declaration is legal");
    }

    @Test
    @DisplayName("A VTOL one hex too far away is out of the cranes' reach")
    void vtolTooFarAwayIsOutOfReach() {
        Dropship dropShip = placeDropShip(0, true);
        VTOL vtol = placeVtol(0, TOO_FAR, 1);

        assertTrue(CraneRules.carriersInReach(vtol, getGame()).isEmpty(), "Nothing should be in reach");
        assertNotNull(CraneRules.loadByCraneIllegalReason(vtol, dropShip, true, getGame()), "The declaration fails");
    }

    @Test
    @DisplayName("A VTOL more than two levels above the DropShip's hex is out of reach (TW p.90)")
    void vtolTooHighIsOutOfReach() {
        Dropship dropShip = placeDropShip(0, true);
        // RAISED_HEX (5, 1) is level 3 and next to the DropShip's north-east hex; elevation 1 puts the VTOL at level 4
        VTOL vtol = placeVtol(0, RAISED_HEX, 1);

        assertFalse(CraneRules.isInReach(vtol, RAISED_HEX, dropShip, getGame()), "Four levels up is too high");
    }

    @Test
    @DisplayName("Crane loading needs a friendly carrier with a suitable bay that is on the ground")
    void carrierMustBeFriendlyGroundedAndHaveBay() {
        Dropship enemyDropShip = placeDropShip(1, true);
        VTOL vtol = placeVtol(0, BESIDE_DROPSHIP, 1);
        assertTrue(CraneRules.carriersInReach(vtol, getGame()).isEmpty(), "An enemy DropShip is not a carrier");

        enemyDropShip.setOwner(getGame().getPlayer(0));
        enemyDropShip.getTransports().clear();
        assertTrue(CraneRules.carriersInReach(vtol, getGame()).isEmpty(), "A DropShip without a bay cannot load it");

        enemyDropShip.addTransporter(new LightVehicleBay(2, 1, 1));
        enemyDropShip.setAltitude(1);
        assertTrue(CraneRules.carriersInReach(vtol, getGame()).isEmpty(), "An airborne DropShip cannot use cranes");
    }

    @Test
    @DisplayName("Crane loading must be the unit's only action")
    void loadingMustBeTheOnlyAction() {
        Dropship dropShip = placeDropShip(0, true);
        VTOL vtol = placeVtol(0, BESIDE_DROPSHIP, 1);

        assertNotNull(CraneRules.loadByCraneIllegalReason(vtol, dropShip, false, getGame()),
              "A unit that already moved cannot also be loaded by crane");
    }

    @Test
    @DisplayName("Unloading hexes are the clear hexes around the DropShip, and a unit standing in one removes it")
    void unloadHexesAreClearHexesAroundTheDropShip() {
        Dropship dropShip = placeDropShip(0, true);
        VTOL vtol = loadedVtol(dropShip);

        int clearHexCount = CraneRules.unloadPositions(dropShip, vtol, getGame()).size();
        assertTrue(CraneRules.unloadPositions(dropShip, vtol, getGame()).contains(BESIDE_DROPSHIP),
              "The hex beside the DropShip is a legal unloading hex");
        assertFalse(CraneRules.unloadPositions(dropShip, vtol, getGame()).contains(DROPSHIP_CENTRE),
              "The DropShip's own hex is not an unloading hex");

        // A VTOL may share a hex with one friendly Mek, so it takes two to break the stacking limit
        for (int blockerId = 40; blockerId <= 41; blockerId++) {
            BipedMek blocker = new BipedMek();
            blocker.setId(blockerId);
            blocker.setWeight(50);
            blocker.setOwner(getGame().getPlayer(0));
            getGame().addEntity(blocker);
            blocker.setDeployed(true);
            blocker.setElevation(0);
            blocker.setPosition(BESIDE_DROPSHIP);
        }

        assertEquals(clearHexCount - 1, CraneRules.unloadPositions(dropShip, vtol, getGame()).size(),
              "Two Meks standing beside the DropShip block that hex for the VTOL");
    }

    @Test
    @DisplayName("Only carried crane-only units that are not already being unloaded are offered")
    void craneUnloadableUnitsSkipsUnitsAlreadyInProgress() {
        Dropship dropShip = placeDropShip(0, true);
        VTOL vtol = loadedVtol(dropShip);

        assertTrue(CraneRules.craneUnloadableUnits(dropShip).contains(vtol), "The carried VTOL can be unloaded");
        assertNull(CraneRules.unloadByCraneIllegalReason(dropShip, vtol, BESIDE_DROPSHIP, true, getGame()),
              "Unloading it beside the DropShip is legal");

        dropShip.getCraneOperations().add(CraneOperation.unload(vtol.getId(), BESIDE_DROPSHIP, 0));
        assertTrue(CraneRules.craneUnloadableUnits(dropShip).isEmpty(), "A VTOL already being unloaded is not offered");
        assertEquals(dropShip, CraneRules.findCarrierWorkingOn(vtol.getId(), getGame()),
              "The DropShip is the carrier working on the VTOL");
    }

    private Dropship placeDropShip(int ownerId, boolean hasVehicleBay) {
        setBoard("CRANE_BOARD");
        addPlayers();
        Dropship dropShip = new Dropship();
        dropShip.setId(20);
        dropShip.setOwner(getGame().getPlayer(ownerId));
        if (hasVehicleBay) {
            dropShip.addTransporter(new LightVehicleBay(2, 1, 1));
            dropShip.addTransporter(new MekBay(2, 1, 2));
        }
        getGame().addEntity(dropShip);
        dropShip.setDeployed(true);
        dropShip.setFacing(0);
        dropShip.setAltitude(0);
        dropShip.setPosition(DROPSHIP_CENTRE);
        return dropShip;
    }

    private void addPlayers() {
        if (getGame().getPlayer(0) != null) {
            return;
        }
        Player friendly = new Player(0, "Friendly");
        friendly.setTeam(1);
        getGame().addPlayer(0, friendly);
        Player enemy = new Player(1, "Enemy");
        enemy.setTeam(2);
        getGame().addPlayer(1, enemy);
    }

    private VTOL placeVtol(int ownerId, Coords position, int elevation) {
        VTOL vtol = new VTOL();
        vtol.setId(30);
        vtol.setWeight(20);
        vtol.setOwner(getGame().getPlayer(ownerId));
        getGame().addEntity(vtol);
        vtol.setDeployed(true);
        vtol.setElevation(elevation);
        vtol.setPosition(position);
        return vtol;
    }

    private VTOL loadedVtol(Dropship dropShip) {
        VTOL vtol = new VTOL();
        vtol.setId(30);
        vtol.setWeight(20);
        vtol.setOwner(getGame().getPlayer(0));
        getGame().addEntity(vtol);
        dropShip.load(vtol, false, 1);
        vtol.setTransportId(dropShip.getId());
        vtol.setPosition(null);
        return vtol;
    }
}
