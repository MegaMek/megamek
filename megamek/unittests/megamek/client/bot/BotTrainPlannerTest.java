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

package megamek.client.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.icons.Camouflage;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers how a bot decides to hitch up the trailers it owns.
 *
 * <p>A bot cannot use the lobby's "Connect as Train" action, so a trailer handed to one has no engine and no tractor
 * and can never move. The planner works out the pairing so the bot can send the same build request a human client
 * sends; the server still validates it.</p>
 */
class BotTrainPlannerTest {

    private static final double TRACTOR_TONS = 75.0;
    private static final double CARRIAGE_TONS = 10.0;
    private static final int BOT_ID = 0;
    private static final int HUMAN_ID = 1;

    private Game game;
    private Player botPlayer;
    private Player humanPlayer;
    private int nextId = 1;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        botPlayer = new Player(BOT_ID, "Princess");
        botPlayer.setTeam(1);
        botPlayer.setBot(true);
        humanPlayer = new Player(HUMAN_ID, "Human");
        humanPlayer.setTeam(1);

        game = new Game();
        game.setPhase(GamePhase.LOUNGE);
        game.addPlayer(BOT_ID, botPlayer);
        game.addPlayer(HUMAN_ID, humanPlayer);
    }

    private Tank buildNamedVehicle(String chassis, String model, double tonnage, boolean isTrailer) throws Exception {
        Tank vehicle = buildVehicle(tonnage, isTrailer, botPlayer);
        vehicle.setChassis(chassis);
        vehicle.setModel(model);
        return vehicle;
    }

    private Tank buildVehicle(double tonnage, boolean isTrailer, Player vehicleOwner) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(nextId++);
        vehicle.setOwner(vehicleOwner);
        vehicle.setWeight(tonnage);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    private Tank botTractor() throws Exception {
        return buildVehicle(TRACTOR_TONS, false, botPlayer);
    }

    private Tank botCarriage() throws Exception {
        return buildVehicle(CARRIAGE_TONS, true, botPlayer);
    }

    private int newForce(String name) {
        return game.getForces().addTopLevelForce(new Force(name, -1, new Camouflage(), botPlayer), botPlayer);
    }

    private Map<Integer, List<Integer>> planForBot() {
        return BotTrainPlanner.planTrains(game, BOT_ID);
    }

    @Test
    void aLooseTrailerIsPlannedBehindTheTractor() throws Exception {
        Tank tractor = botTractor();
        Tank carriage = botCarriage();

        Map<Integer, List<Integer>> plan = planForBot();

        assertEquals(Map.of(tractor.getId(), List.of(carriage.getId())), plan,
              "The bot works out that its trailer belongs behind its tractor");
    }

    @Test
    void trailersGoToTheTractorInTheirOwnForce() throws Exception {
        int batteryA = newForce("Battery A");
        int batteryB = newForce("Battery B");

        Tank tractorA = botTractor();
        Tank tractorB = botTractor();
        Tank carriageB = botCarriage();
        Tank carriageA = botCarriage();

        game.getForces().addEntity(tractorA, batteryA);
        game.getForces().addEntity(carriageA, batteryA);
        game.getForces().addEntity(tractorB, batteryB);
        game.getForces().addEntity(carriageB, batteryB);

        Map<Integer, List<Integer>> plan = planForBot();

        assertEquals(List.of(carriageA.getId()), plan.get(tractorA.getId()),
              "Battery A keeps its own carriage even though B's came first in the list");
        assertEquals(List.of(carriageB.getId()), plan.get(tractorB.getId()),
              "Battery B keeps its own carriage");
    }

    @Test
    void aTrailerWithNoForceStillFindsATractor() throws Exception {
        Tank tractor = botTractor();
        Tank carriage = botCarriage();

        assertEquals(List.of(carriage.getId()), planForBot().get(tractor.getId()),
              "A hand-built list with no forces still ends up usable");
    }

    @Test
    void anotherPlayersUnitsAreNotPlanned() throws Exception {
        buildVehicle(TRACTOR_TONS, false, humanPlayer);
        buildVehicle(CARRIAGE_TONS, true, humanPlayer);

        assertTrue(planForBot().isEmpty(), "A human connects their own trains; the bot plans nothing for them");
    }

    @Test
    void towingCapacityIsRespected() throws Exception {
        Tank tractor = botTractor();
        // Six ten ton carriages exceed the tractor's own seventy-five tons.
        for (int index = 0; index < 6; index++) {
            botCarriage();
        }

        List<Integer> train = planForBot().get(tractor.getId());

        double towedWeight = 0;
        for (int trailerId : train) {
            towedWeight += game.getEntity(trailerId).getWeight();
        }
        assertTrue(towedWeight <= TRACTOR_TONS,
              "The plan stays inside the tractor's towing capacity, planned " + towedWeight + " tons");
        assertEquals(7, train.size() + 1, "Seven carriages would not fit, so one is left out");
    }

    @Test
    void aTractorThatAlreadyTowsIsLeftAlone() throws Exception {
        Tank tractor = botTractor();
        Tank firstCarriage = botCarriage();
        tractor.towUnit(firstCarriage.getId());
        botCarriage();

        assertFalse(planForBot().containsKey(tractor.getId()),
              "A build request covers a whole train, and the server refuses one whose tractor already tows");
    }

    @Test
    void anAlreadyHitchedTrailerIsNeverReplanned() throws Exception {
        Tank tractor = botTractor();
        Tank carriage = botCarriage();
        tractor.towUnit(carriage.getId());

        assertTrue(planForBot().isEmpty(),
              "Replanning after the trains are built produces nothing, so a train built by hand is not disturbed");
    }

    @Test
    void carriagesGoToTheirOwnGunCarriageWithNoForcesSet() throws Exception {
        // Every Mobile Long Tom variant shares one chassis, so only the model tells the batteries apart.
        String chassis = "Mobile Long Tom Artillery";
        Tank olderGun = buildNamedVehicle(chassis, "LT-MOB-25", TRACTOR_TONS, false);
        Tank newerGun = buildNamedVehicle(chassis, "LT-MOB-25F", TRACTOR_TONS, false);
        Tank newerAmmo = buildNamedVehicle(chassis, "LT-MOB-25F (Ammunition Carriage)", CARRIAGE_TONS, true);
        Tank olderAmmo = buildNamedVehicle(chassis, "LT-MOB-25 (Ammunition Carriage)", CARRIAGE_TONS, true);

        Map<Integer, List<Integer>> plan = planForBot();

        assertEquals(List.of(olderAmmo.getId()), plan.get(olderGun.getId()),
              "The LT-MOB-25 takes its own carriage, not the one listed first");
        assertEquals(List.of(newerAmmo.getId()), plan.get(newerGun.getId()),
              "The LT-MOB-25F takes its own carriage");
    }

    @Test
    void aModelPrefixAloneDoesNotClaimAVariantsCarriage() throws Exception {
        // "LT-MOB-25F (Ammunition Carriage)" starts with "LT-MOB-25", so a bare prefix test would mis-assign it.
        String chassis = "Mobile Long Tom Artillery";
        Tank olderGun = buildNamedVehicle(chassis, "LT-MOB-25", TRACTOR_TONS, false);
        Tank newerAmmo = buildNamedVehicle(chassis, "LT-MOB-25F (Ammunition Carriage)", CARRIAGE_TONS, true);

        // With no better home it still gets towed, but only on the last pass, not as a design match.
        assertEquals(List.of(newerAmmo.getId()), planForBot().get(olderGun.getId()),
              "A lone tractor still takes it rather than leaving it immobile");

        Tank newerGun = buildNamedVehicle(chassis, "LT-MOB-25F", TRACTOR_TONS, false);

        assertEquals(List.of(newerAmmo.getId()), planForBot().get(newerGun.getId()),
              "Once its own gun carriage is present, the F carriage goes there instead");
    }

    @Test
    void aForceBeatsADesignMatch() throws Exception {
        String chassis = "Mobile Long Tom Artillery";
        int battery = newForce("Mixed Battery");

        Tank olderGun = buildNamedVehicle(chassis, "LT-MOB-25", TRACTOR_TONS, false);
        Tank newerGun = buildNamedVehicle(chassis, "LT-MOB-25F", TRACTOR_TONS, false);
        Tank newerAmmo = buildNamedVehicle(chassis, "LT-MOB-25F (Ammunition Carriage)", CARRIAGE_TONS, true);

        // The player put the F carriage in the older gun's force on purpose.
        game.getForces().addEntity(olderGun, battery);
        game.getForces().addEntity(newerAmmo, battery);

        Map<Integer, List<Integer>> plan = planForBot();

        assertEquals(List.of(newerAmmo.getId()), plan.get(olderGun.getId()),
              "An explicit force wins over the design match");
        assertFalse(plan.containsKey(newerGun.getId()), "so its own gun carriage gets nothing");
    }

    @Test
    void trailersWithNoTractorAreLeftLoose() throws Exception {
        botCarriage();

        assertTrue(planForBot().isEmpty(), "Nothing to hitch them to, so no train is planned");
    }
}
