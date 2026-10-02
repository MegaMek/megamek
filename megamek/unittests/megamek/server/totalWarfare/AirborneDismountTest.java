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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.units.AirborneDismountRules;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.InfantryCompartment;
import megamek.common.units.SupportTank;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Leaving a VTOL or WiGE that has not landed (issue #9107, TW p.225 Dismounting From VTOLs as replaced by errata
 * v12.0): only infantry with Jump or VTOL MP may leave; jump infantry land on the ground or roof, VTOL infantry stay at
 * the carrier's elevation. Tested through the move step legality the server checks and through
 * {@link TWGameManager#unloadUnit}.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class AirborneDismountTest extends GameBoardTestCase {

    private static final int WIGE_ID = 5;
    private static final Coords CARRIER_HEX = new Coords(0, 0);

    static {
        initializeBoard("DISMOUNT_CLEAR", """
              size 1 1
              hex 0101 0 "" ""
              end""");

        initializeBoard("DISMOUNT_BUILDING", """
              size 1 1
              hex 0101 0 "bldg_elev:1;building:2:0;bldg_cf:40" ""
              end""");
    }

    private TWGameManager gameManager;

    /** Dice that always roll the middle: every d6 is a 4. */
    private static final class MiddleDice extends MMRandom {
        @Override
        public int randomInt(int maxValue) {
            return maxValue / 2;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    @BeforeEach
    void setUp() throws IOException {
        Compute.setRNG(new MiddleDice());
        gameManager = new TWGameManager();
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        if (getGame().getPlayer(0) == null) {
            getGame().addPlayer(0, new Player(0, "Test"));
        }
    }

    /** A WiGE support vehicle with a troop compartment, deployed in hex 0101 at the given elevation. */
    private SupportTank wigeCarrier(int elevation) {
        SupportTank wige = new SupportTank();
        wige.setChassis("Test");
        wige.setModel("WiGE Transport");
        wige.setMovementMode(EntityMovementMode.WIGE);
        wige.setWeight(30.0);
        wige.setOriginalWalkMP(8);
        wige.autoSetInternal();
        for (int location = 0; location < wige.locations(); location++) {
            wige.initializeArmor(20, location);
        }
        wige.setCrew(new Crew(CrewType.SINGLE));
        wige.setOwner(getGame().getPlayer(0));
        wige.addTransporter(new InfantryCompartment(20.0));
        wige.setId(WIGE_ID);
        getGame().addEntity(wige);
        wige.setPosition(CARRIER_HEX);
        wige.setElevation(elevation);
        wige.setDeployed(true);
        return wige;
    }

    /** A conventional infantry platoon with the given movement mode, loaded aboard the carrier. */
    private ConvInfantry loadedInfantry(SupportTank carrier, EntityMovementMode movementMode, int id) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setChassis("Test");
        infantry.setModel(movementMode.name());
        infantry.setMovementMode(movementMode);
        infantry.setOwner(getGame().getPlayer(0));
        infantry.setId(id);
        getGame().addEntity(infantry);
        carrier.load(infantry, false);
        infantry.setTransportId(carrier.getId());
        return infantry;
    }

    private boolean unloadStepIsLegal(SupportTank carrier, ConvInfantry passenger) {
        MovePath movePath = new MovePath(getGame(), carrier);
        movePath.addStep(MoveStepType.UNLOAD, passenger);
        movePath.compile(getGame(), carrier, false);
        return movePath.getLastStep().getMovementType(true) != EntityMovementType.MOVE_ILLEGAL;
    }

    private Hex carrierHex() {
        return getGame().getBoard().getHex(CARRIER_HEX);
    }

    @Test
    void footInfantryCannotLeaveAnAirborneWiGE() {
        setBoard("DISMOUNT_CLEAR");
        SupportTank wige = wigeCarrier(1);
        ConvInfantry footInfantry = loadedInfantry(wige, EntityMovementMode.INF_LEG, 6);

        assertFalse(unloadStepIsLegal(wige, footInfantry), "only jump or VTOL infantry may leave an airborne WiGE");
    }

    @Test
    void footInfantryCanLeaveALandedWiGE() {
        setBoard("DISMOUNT_CLEAR");
        SupportTank wige = wigeCarrier(0);
        ConvInfantry footInfantry = loadedInfantry(wige, EntityMovementMode.INF_LEG, 6);

        assertTrue(unloadStepIsLegal(wige, footInfantry), "any infantry may leave a landed WiGE");
    }

    @Test
    void jumpAndVtolInfantryCanLeaveAnAirborneWiGE() {
        setBoard("DISMOUNT_CLEAR");
        SupportTank wige = wigeCarrier(1);
        ConvInfantry jumpInfantry = loadedInfantry(wige, EntityMovementMode.INF_JUMP, 6);
        ConvInfantry vtolInfantry = loadedInfantry(wige, EntityMovementMode.VTOL, 7);

        assertTrue(unloadStepIsLegal(wige, jumpInfantry), "jump infantry may leave an airborne WiGE");
        assertTrue(unloadStepIsLegal(wige, vtolInfantry), "VTOL infantry may leave an airborne WiGE");
    }

    @Test
    void vtolInfantryStayAtTheElevationOfTheWiGE() {
        setBoard("DISMOUNT_CLEAR");
        SupportTank wige = wigeCarrier(1);
        ConvInfantry vtolInfantry = loadedInfantry(wige, EntityMovementMode.VTOL, 7);

        assertTrue(gameManager.unloadUnit(wige, vtolInfantry, CARRIER_HEX, 0, wige.getElevation()));

        assertEquals(1, vtolInfantry.getElevation(), "VTOL infantry are placed at the carrier's elevation");
    }

    @Test
    void jumpInfantryLeavingAWiGEAboveABuildingLandOnTheRoof() {
        setBoard("DISMOUNT_BUILDING");
        SupportTank wige = wigeCarrier(2);
        ConvInfantry jumpInfantry = loadedInfantry(wige, EntityMovementMode.INF_JUMP, 6);

        assertTrue(gameManager.unloadUnit(wige, jumpInfantry, CARRIER_HEX, 0, wige.getElevation()));

        assertEquals(1, jumpInfantry.getElevation(), "jump infantry land on the roof, not in the air");
        assertEquals(EntityMovementType.MOVE_JUMP, jumpInfantry.moved, "attacks count the jump (+1)");
    }

    @Test
    void jumpInfantryLeavingAnAirborneWiGELandOnTheGround() {
        setBoard("DISMOUNT_CLEAR");
        SupportTank wige = wigeCarrier(1);
        ConvInfantry jumpInfantry = loadedInfantry(wige, EntityMovementMode.INF_JUMP, 6);

        assertTrue(gameManager.unloadUnit(wige, jumpInfantry, CARRIER_HEX, 0, wige.getElevation()));

        assertEquals(0, jumpInfantry.getElevation(), "jump infantry are placed on the ground");
    }

    @Test
    void aWiGEOnTheRoofHasLanded() {
        setBoard("DISMOUNT_BUILDING");
        SupportTank wige = wigeCarrier(1);

        assertFalse(AirborneDismountRules.isCarrierAirborne(wige, carrierHex(), 1), "resting on the roof is landed");
        assertTrue(AirborneDismountRules.isCarrierAirborne(wige, carrierHex(), 2), "above the roof is airborne");
    }

    @Test
    void onlyJumpAndVtolInfantryMayLeaveAnAirborneWiGE() {
        setBoard("DISMOUNT_CLEAR");
        SupportTank wige = wigeCarrier(1);
        ConvInfantry footInfantry = loadedInfantry(wige, EntityMovementMode.INF_LEG, 6);
        ConvInfantry jumpInfantry = loadedInfantry(wige, EntityMovementMode.INF_JUMP, 7);

        assertFalse(AirborneDismountRules.canDismountFromAirborneCarrier(getGame(), wige, footInfantry));
        assertTrue(AirborneDismountRules.canDismountFromAirborneCarrier(getGame(), wige, jumpInfantry));
        assertFalse(AirborneDismountRules.canDismountFromAirborneCarrier(getGame(), wige, new SupportTank()),
              "a vehicle cannot leave an airborne WiGE");
    }
}
