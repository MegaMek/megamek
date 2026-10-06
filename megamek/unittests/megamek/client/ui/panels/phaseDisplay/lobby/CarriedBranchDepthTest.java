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

package megamek.client.ui.panels.phaseDisplay.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the branch drawn before a unit that rides on another growing with how deeply it sits.
 *
 * <p>A flat mark on every carried row says only "this is carried", which is no help when a JumpShip holds a DropShip
 * holding Meks. The branch is indented and lengthened once per level so a stack reads as a tree.</p>
 */
class CarriedBranchDepthTest {

    private Game game;
    private Player owner;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        game = new Game();
        owner = new Player(0, "Owner");
        owner.setTeam(1);
        game.addPlayer(0, owner);
    }

    private Tank buildVehicle(boolean isTrailer) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(game.getNextEntityId());
        vehicle.setOwner(owner);
        vehicle.setWeight(50.0);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    @Test
    void aUnitRidingNothingGetsNoBranch() throws Exception {
        Tank loneUnit = buildVehicle(false);

        assertEquals("", LobbyMekCellFormatter.carriedBranch(loneUnit),
              "A unit that is not carried or towed is drawn flush");
    }

    @Test
    void theArmGrowsOneLengthPerLevel() throws Exception {
        Tank jumpShip = buildVehicle(false);
        Tank dropShip = buildVehicle(false);
        dropShip.setTransportId(jumpShip.getId());
        Tank mek = buildVehicle(false);
        mek.setTransportId(dropShip.getId());

        String dropShipBranch = LobbyMekCellFormatter.carriedBranch(dropShip);
        String mekBranch = LobbyMekCellFormatter.carriedBranch(mek);

        assertEquals(1, countArms(dropShipBranch), "One level down draws one arm");
        assertEquals(2, countArms(mekBranch), "Two levels down draws two");
        assertTrue(mekBranch.length() > dropShipBranch.length(),
              "and the deeper unit is indented further so the stack reads as a tree");
    }

    @Test
    void aTowedTrailerIsBranchedLikeACarriedUnit() throws Exception {
        Tank tractor = buildVehicle(false);
        Tank trailer = buildVehicle(true);
        tractor.towUnit(trailer.getId());

        assertEquals(1, countArms(LobbyMekCellFormatter.carriedBranch(trailer)),
              "A trailer rides on its tractor just as cargo rides in a bay");
    }

    @Test
    void aTrailerBehindACarriedTractorIsDrawnDeeper() throws Exception {
        Tank carrier = buildVehicle(false);
        Tank tractor = buildVehicle(false);
        tractor.setTransportId(carrier.getId());
        Tank trailer = buildVehicle(true);
        tractor.towUnit(trailer.getId());

        assertEquals(1, countArms(LobbyMekCellFormatter.carriedBranch(tractor)));
        assertEquals(2, countArms(LobbyMekCellFormatter.carriedBranch(trailer)),
              "The trailer is two levels in: towed by a tractor that is itself carried");
    }

    @Test
    void aLoadPointingAtItselfDoesNotSpin() throws Exception {
        Tank confusedUnit = buildVehicle(false);
        confusedUnit.setTransportId(confusedUnit.getId());

        assertEquals("", LobbyMekCellFormatter.carriedBranch(confusedUnit),
              "A unit carried by itself is treated as carried by nothing rather than looping");
    }

    /** Counts the horizontal arms in a branch, which is the depth it is drawn at. */
    private static int countArms(String branch) {
        return (int) branch.chars().filter(character -> character == '\u2500').count();
    }
}
