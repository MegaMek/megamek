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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.MekWarrior;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * An ejected crew inside an enemy building is taken prisoner by the building's crew in the End Phase (TO:AR p. 165,
 * applied to every ejected crew), and takes no part in infantry vs. infantry actions.
 */
@DisplayName("Ejected crews inside enemy buildings")
class EjectedCrewCaptureHandlerTest {

    private static final Coords HEX_A = new Coords(5, 5);
    private static final Coords HEX_B = new Coords(6, 5);
    private static final Coords OUTSIDE = new Coords(10, 10);
    private static final int BUILDING_CREW = 4;
    private static final int VEHICLE_CREW = 3;

    private Game game;
    private TWGameManager gameManager;
    private EjectedCrewCaptureHandler handler;
    private Player ejectedPlayer;
    private Player buildingPlayer;
    private BuildingEntity building;
    private int nextId = 1;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        ejectedPlayer = new Player(0, "Ejected");
        buildingPlayer = new Player(1, "Garrison");
        ejectedPlayer.setTeam(Player.TEAM_NONE);
        buildingPlayer.setTeam(Player.TEAM_NONE);
        game.addPlayer(0, ejectedPlayer);
        game.addPlayer(1, buildingPlayer);
        game.setBoard(new Board(16, 17));
        game.setPhase(GamePhase.END);
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        gameManager.setGame(game);
        handler = new EjectedCrewCaptureHandler(gameManager);

        // A two-hex emplacement with four crew, anchored on hex A and reaching hex B
        building = new BuildingEntity(BuildingType.MEDIUM, 3);
        building.setOwner(buildingPlayer);
        building.setGame(game);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 20, BasementType.NONE, false);
        building.getInternalBuilding().addHex(new CubeCoords(1, -1, 0), 40, 20, BasementType.NONE, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.getCrew().setSize(BUILDING_CREW);
        building.getCrew().setCurrentSize(BUILDING_CREW);
        building.setId(0);
        game.addEntity(building);
        building.setPosition(HEX_A);
        building.setDeployed(true);
    }

    private MekWarrior mekWarrior(Player owner, Coords position) {
        MekWarrior pilot = new MekWarrior(new Crew(CrewType.SINGLE), owner, game);
        return place(pilot, position);
    }

    private EjectedCrew vehicleCrew(Player owner, Coords position) {
        Crew crew = new Crew(CrewType.CREW);
        crew.setSize(VEHICLE_CREW);
        EjectedCrew ejectedCrew = new EjectedCrew(crew, owner, game);
        return place(ejectedCrew, position);
    }

    private <T extends Entity> T place(T unit, Coords position) {
        unit.setGame(game);
        unit.setDeployed(true);
        unit.setId(nextId++);
        game.addEntity(unit);
        unit.setPosition(position);
        return unit;
    }

    private ConvInfantry platoon(Player owner, Coords position) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(owner);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        return place(infantry, position);
    }

    @Test
    @DisplayName("A vehicle crew inside a crewed enemy building is captured and leaves play")
    void vehicleCrewIsCaptured() {
        EjectedCrew ejectedCrew = vehicleCrew(ejectedPlayer, HEX_A);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertTrue(ejectedCrew.isCaptured());
        assertNull(game.getEntity(ejectedCrew.getId()), "a captured crew is removed from the game");
    }

    @Test
    @DisplayName("A vehicle crew in the building's second hex is captured too")
    void vehicleCrewInSecondaryHexIsCaptured() {
        EjectedCrew ejectedCrew = vehicleCrew(ejectedPlayer, HEX_B);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertTrue(ejectedCrew.isCaptured());
    }

    @Test
    @DisplayName("An enemy gun emplacement captures a vehicle crew in its hex")
    void gunEmplacementCapturesVehicleCrew() {
        GunEmplacement emplacement = new GunEmplacement();
        emplacement.setOwner(buildingPlayer);
        place(emplacement, OUTSIDE);
        EjectedCrew ejectedCrew = vehicleCrew(ejectedPlayer, OUTSIDE);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertTrue(ejectedCrew.isCaptured());
    }

    @Test
    @DisplayName("A vehicle crew above the roof is not captured")
    void vehicleCrewAboveTheRoofStaysFree() {
        EjectedCrew ejectedCrew = vehicleCrew(ejectedPlayer, HEX_A);
        ejectedCrew.setElevation(5);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertFalse(ejectedCrew.isCaptured());
    }

    @Test
    @DisplayName("A building with no crew left captures nobody")
    void crewlessBuildingCapturesNobody() {
        building.getCrew().setCurrentSize(0);
        EjectedCrew ejectedCrew = vehicleCrew(ejectedPlayer, HEX_B);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertFalse(ejectedCrew.isCaptured());
        assertNotNull(game.getEntity(ejectedCrew.getId()));
    }

    @Test
    @DisplayName("A vehicle crew outside the building stays free")
    void crewOutsideStaysFree() {
        EjectedCrew ejectedCrew = vehicleCrew(ejectedPlayer, OUTSIDE);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertFalse(ejectedCrew.isCaptured());
        assertEquals(OUTSIDE, ejectedCrew.getPosition());
    }

    @Test
    @DisplayName("A building does not capture its own side's ejected crew")
    void ownCrewStaysFree() {
        EjectedCrew ejectedCrew = vehicleCrew(buildingPlayer, HEX_B);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertFalse(ejectedCrew.isCaptured());
    }

    @Test
    @DisplayName("A MekWarrior is left to the general pick-up, which asks for its captor")
    void mekWarriorIsLeftToTheGeneralPickUp() {
        MekWarrior pilot = mekWarrior(ejectedPlayer, HEX_A);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertFalse(pilot.isCaptured());
        assertSame(building, handler.findCapturingBuilding(pilot));
    }

    @Test
    @DisplayName("An ordinary platoon inside the building is left to the infantry action rules")
    void platoonIsNotCaptured() {
        ConvInfantry infantry = platoon(ejectedPlayer, HEX_A);

        handler.captureOtherCrewsInsideEnemyBuildings();

        assertFalse(infantry.isCaptured());
        assertNotNull(game.getEntity(infantry.getId()));
    }

    @Test
    @DisplayName("Ejected crews give neither side an infantry action to declare")
    void ejectedCrewsGiveNoStake() {
        MekWarrior pilot = mekWarrior(ejectedPlayer, HEX_A);
        EjectedCrew ejectedCrew = vehicleCrew(ejectedPlayer, HEX_B);

        assertTrue(InfantryActionStrengths.stakes(game, ejectedPlayer).isEmpty(), "no attack to declare");
        assertTrue(InfantryActionStrengths.stakes(game, buildingPlayer).isEmpty(), "no defence to declare");
        assertFalse(InfantryActionStrengths.hasStake(game, pilot));
        assertFalse(InfantryActionStrengths.hasStake(game, ejectedCrew));
        assertFalse(InfantryActionStrengths.fightsInInfantryActions(pilot));
        assertFalse(InfantryActionStrengths.fightsInInfantryActions(ejectedCrew));
    }

    @Test
    @DisplayName("A platoon inside the building still gives both sides a stake")
    void platoonStillGivesAStake() {
        platoon(ejectedPlayer, HEX_A);

        assertEquals(1, InfantryActionStrengths.stakes(game, ejectedPlayer).size());
        assertEquals(1, InfantryActionStrengths.stakes(game, buildingPlayer).size());
    }

    @Test
    @DisplayName("The defender's own ejected crew cannot be committed to the defence")
    void ownEjectedCrewIsNotOfferedAsADefender() {
        mekWarrior(buildingPlayer, HEX_A);

        assertTrue(InfantryActionStrengths.unengagedFriendlyInfantryInside(game, buildingPlayer, building)
              .isEmpty());
    }
}
