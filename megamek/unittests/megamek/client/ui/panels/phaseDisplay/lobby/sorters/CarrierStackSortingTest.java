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

package megamek.client.ui.panels.phaseDisplay.lobby.sorters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.client.Client;
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
 * Covers a carrier and everything riding on it staying together under any lobby sorter.
 *
 * <p>A load used to be sorted on its own merits, so it drifted away from what carried it: by tonnage a 10 ton
 * carriage sorted nowhere near the 75 ton tractor pulling it, and a DropShip nowhere near its JumpShip. Only the
 * transport sorter grouped carried units, and nothing grouped trains at all.</p>
 *
 * <p>The player's choice is now applied to the outermost carrier and the stack follows it: carriers before their
 * loads at any depth, and trailers behind their tractor in hitch order, which is the order they occupy hexes in.</p>
 */
class CarrierStackSortingTest {

    private static final double TRACTOR_TONS = 75.0;
    private static final double CARRIAGE_TONS = 10.0;

    private Game game;
    private Player owner;
    private Client client;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        game = new Game();
        owner = new Player(0, "Owner");
        owner.setTeam(1);
        game.addPlayer(0, owner);

        client = mock(Client.class);
        when(client.getGame()).thenReturn(game);
        when(client.getLocalPlayer()).thenReturn(owner);
    }

    private Tank buildVehicle(boolean isTrailer) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(game.getNextEntityId());
        vehicle.setOwner(owner);
        vehicle.setWeight(isTrailer ? CARRIAGE_TONS : TRACTOR_TONS);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    /** Sorts as the lobby table does, through the wrapper that keeps trains together. */
    private List<Entity> sorted(Entity... units) {
        return sortedWith(new PlayerTransportIDSorter(client), units);
    }

    /** Sorts with a specific sorter, still through the train-grouping wrapper the lobby applies. */
    private List<Entity> sortedWith(MekTableSorter baseSorter, Entity... units) {
        List<Entity> ordered = new ArrayList<>(List.of(units));
        ordered.sort(MekTableSorter.keepingCarriedUnitsTogether(baseSorter));
        return ordered;
    }

    @Test
    void trailersFollowTheirTractorInHitchOrder() throws Exception {
        Tank tractor = buildVehicle(false);
        Tank firstTrailer = buildVehicle(true);
        Tank secondTrailer = buildVehicle(true);
        tractor.towUnit(firstTrailer.getId());
        tractor.towUnit(secondTrailer.getId());

        assertEquals(List.of(tractor, firstTrailer, secondTrailer),
              sorted(secondTrailer, tractor, firstTrailer),
              "The tractor leads and its trailers follow front to back, whatever order they came in");
    }

    @Test
    void aTrainStaysTogetherWhenOtherUnitsSitBetweenItsIds() throws Exception {
        Tank tractor = buildVehicle(false);
        Tank otherUnit = buildVehicle(false);
        Tank trailer = buildVehicle(true);
        tractor.towUnit(trailer.getId());

        // By raw id the order would be tractor, otherUnit, trailer. The train must not be split by the unit
        // that happens to sit between them.
        assertEquals(List.of(tractor, trailer, otherUnit),
              sorted(otherUnit, trailer, tractor),
              "A unit with an id between the tractor and its trailer does not break up the train");
    }

    @Test
    void separateTrainsStayInTractorOrder() throws Exception {
        Tank firstTractor = buildVehicle(false);
        Tank firstTrailer = buildVehicle(true);
        Tank secondTractor = buildVehicle(false);
        Tank secondTrailer = buildVehicle(true);
        firstTractor.towUnit(firstTrailer.getId());
        secondTractor.towUnit(secondTrailer.getId());

        assertEquals(List.of(firstTractor, firstTrailer, secondTractor, secondTrailer),
              sorted(secondTrailer, firstTrailer, secondTractor, firstTractor),
              "Each train is a block, and the blocks follow their tractors' order");
    }

    @Test
    void tonnageSortingKeepsACarriageWithItsTractor() throws Exception {
        // The reported case: a 75 ton Long Tom with two 10 ton carriages, sorted by tonnage descending, put the
        // carriages at the bottom of the list with unrelated units in between.
        Tank tractor = buildVehicle(false);
        Tank ammoCarriage = buildVehicle(true);
        Tank supportCarriage = buildVehicle(true);
        tractor.towUnit(ammoCarriage.getId());
        tractor.towUnit(supportCarriage.getId());

        Tank mediumTank = buildVehicle(false);
        mediumTank.setWeight(50.0);

        List<Entity> ordered = sortedWith(new PlayerTonnageSorter(client, Sorting.DESCENDING),
              supportCarriage, mediumTank, ammoCarriage, tractor);

        assertEquals(List.of(tractor, ammoCarriage, supportCarriage, mediumTank), ordered,
              "The carriages ride with their tractor instead of sinking to the light end of the list");
    }

    @Test
    void everySorterKeepsTheTrainTogether() throws Exception {
        Tank tractor = buildVehicle(false);
        Tank trailer = buildVehicle(true);
        tractor.towUnit(trailer.getId());
        Tank otherUnit = buildVehicle(false);
        otherUnit.setWeight(50.0);

        List<MekTableSorter> sorters = List.of(
              new PlayerTransportIDSorter(client),
              new PlayerTonnageSorter(client, Sorting.ASCENDING),
              new PlayerTonnageSorter(client, Sorting.DESCENDING),
              new NameSorter(Sorting.ASCENDING),
              new IDSorter(Sorting.ASCENDING),
              new TypeSorter(Sorting.ASCENDING));

        for (MekTableSorter baseSorter : sorters) {
            List<Entity> ordered = sortedWith(baseSorter, trailer, otherUnit, tractor);
            int tractorIndex = ordered.indexOf(tractor);

            assertEquals(tractorIndex + 1, ordered.indexOf(trailer),
                  "The trailer sits directly behind its tractor under " + baseSorter.getDisplayName());
        }
    }

    @Test
    void aCarriedUnitStaysWithItsCarrier() throws Exception {
        Tank carrier = buildVehicle(false);
        Tank cargo = buildVehicle(false);
        cargo.setWeight(50.0);
        cargo.setTransportId(carrier.getId());

        Tank otherUnit = buildVehicle(false);
        otherUnit.setWeight(60.0);

        List<Entity> ordered = sortedWith(new PlayerTonnageSorter(client, Sorting.DESCENDING),
              cargo, otherUnit, carrier);

        assertEquals(List.of(carrier, cargo, otherUnit), ordered,
              "A carried unit rides with its carrier instead of sorting on its own tonnage");
    }

    @Test
    void nestedCarriersKeepTheWholeStackInOrder() throws Exception {
        // A JumpShip carrying a DropShip carrying two Meks: everything hangs off its own carrier, at any depth.
        Tank jumpShip = buildVehicle(false);
        jumpShip.setWeight(100.0);
        Tank dropShip = buildVehicle(false);
        dropShip.setWeight(80.0);
        dropShip.setTransportId(jumpShip.getId());
        Tank firstMek = buildVehicle(false);
        firstMek.setWeight(55.0);
        firstMek.setTransportId(dropShip.getId());
        Tank secondMek = buildVehicle(false);
        secondMek.setWeight(45.0);
        secondMek.setTransportId(dropShip.getId());

        Tank looseTank = buildVehicle(false);
        looseTank.setWeight(90.0);

        List<Entity> ordered = sortedWith(new PlayerTonnageSorter(client, Sorting.DESCENDING),
              secondMek, looseTank, firstMek, dropShip, jumpShip);

        assertEquals(List.of(jumpShip, dropShip, firstMek, secondMek, looseTank), ordered,
              "The stack holds together and the 90 ton loose tank does not wedge into the middle of it");
    }

    @Test
    void aCarriedTrainKeepsItsHitchOrder() throws Exception {
        // A train loaded into a carrier: the stack nests and the trailers still follow the tractor front to back.
        Tank carrier = buildVehicle(false);
        carrier.setWeight(100.0);
        Tank tractor = buildVehicle(false);
        tractor.setTransportId(carrier.getId());
        Tank firstTrailer = buildVehicle(true);
        Tank secondTrailer = buildVehicle(true);
        tractor.towUnit(firstTrailer.getId());
        tractor.towUnit(secondTrailer.getId());

        List<Entity> ordered = sortedWith(new PlayerTonnageSorter(client, Sorting.DESCENDING),
              secondTrailer, firstTrailer, carrier, tractor);

        assertEquals(List.of(carrier, tractor, firstTrailer, secondTrailer), ordered,
              "A train inside a carrier keeps both the nesting and the hitch order");
    }

    @Test
    void looseUnitsStillSortById() throws Exception {
        Tank first = buildVehicle(false);
        Tank second = buildVehicle(false);
        Tank third = buildVehicle(false);

        assertEquals(List.of(first, second, third), sorted(third, first, second),
              "Units in no train are unaffected by the train rule");
    }

    @Test
    void aDetachedTrailerReturnsToItsOwnPlace() throws Exception {
        Tank tractor = buildVehicle(false);
        Tank otherUnit = buildVehicle(false);
        Tank trailer = buildVehicle(true);
        tractor.towUnit(trailer.getId());
        tractor.disconnectUnit(trailer.getId());

        assertEquals(List.of(tractor, otherUnit, trailer), sorted(trailer, otherUnit, tractor),
              "Once detached it sorts by its own id again");
    }
}
