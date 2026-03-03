/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import megamek.common.compute.Compute;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Integration tests for infantry vs. infantry action resolution (TOAR p. 167-174).
 * Tests the ACTUAL production code path through TWGameManager.resolveInfantryActions()
 * to verify actions properly end when entities are eliminated.
 */
public class InfantryActionResolutionTest {

    private TestableGameManager gameManager;
    private Game game;
    private Player player1;
    private Player player2;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        player1 = new Player(0, "Player 1");
        player2 = new Player(1, "Player 2");
        // Players default to TEAM_UNASSIGNED which makes them enemies
        player1.setTeam(Player.TEAM_NONE);
        player2.setTeam(Player.TEAM_NONE);
        game.addPlayer(0, player1);
        game.addPlayer(1, player2);

        // Set up a simple board
        Board board = new Board(16, 17);
        game.setBoard(board);

        gameManager = new TestableGameManager();
        gameManager.setGame(game);
    }

    /**
     * Test combat ends when all defenders are eliminated by external means.
     * This simulates ALL defenders (building crew + infantry) being killed by weapons fire.
     * TODO: Update when player choice for crew participation is implemented
     */
    @Test
    void testCombatEnds_WhenDefendersKilledExternally() {
        // Create building WITH crew (crew always defends)
        AbstractBuildingEntity building = createBuildingWithCrew(player2, new Coords(5, 5), 10);
        Infantry attacker = createInfantry(player1, new Coords(5, 5), 28);
        Infantry defender1 = createInfantry(player2, new Coords(5, 5), 28);
        Infantry defender2 = createInfantry(player2, new Coords(5, 5), 28);

        // Add to game - building needs special initialization
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);  // Must set ID before adding to game
        game.addEntity(building);
        game.addEntity(attacker);
        game.addEntity(defender1);
        game.addEntity(defender2);

        // Set up combat matching production flow: building crew + infantry defenders
        InfantryActionTracker tracker = gameManager.getInfantryCombatTracker();
        tracker.addCombat(building.getId(), attacker, building);  // Building crew first
        tracker.addReinforcement(building.getId(), defender1, false);
        tracker.addReinforcement(building.getId(), defender2, false);

        // Verify combat is active
        assertTrue(tracker.hasCombat(building.getId()));
        var combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertEquals(1, combat.attackerIds.size());
        assertEquals(3, combat.defenderIds.size());  // Building + 2 infantry

        // Simulate ALL defenders killed externally (building crew eliminated + infantry destroyed)
        building.getCrew().setCurrentSize(0);  // Kill building crew
        defender1.setDestroyed(true);
        defender2.setDestroyed(true);

        // Resolve combat - should detect all defenders dead and end combat
        gameManager.resolveInfantryActions();

        // Verify combat ended
        assertFalse(tracker.hasCombat(building.getId()),
            "Combat should end when all defenders (crew + infantry) are eliminated");

        // Verify entity states cleared
        assertEquals(Entity.NONE, attacker.getInfantryCombatTargetId(),
            "Attacker combat state should be cleared");
    }

    /**
     * Test combat ends when all attackers are eliminated by external means.
     * Building crew is always a defender per current implementation.
     * TODO: Update when player choice for crew participation is implemented
     */
    @Test
    void testCombatEnds_WhenAttackersKilledExternally() {
        // Create building WITH crew (crew always defends)
        AbstractBuildingEntity building = createBuildingWithCrew(player2, new Coords(5, 5), 10);
        Infantry attacker1 = createInfantry(player1, new Coords(5, 5), 28);
        Infantry attacker2 = createInfantry(player1, new Coords(5, 5), 28);
        Infantry defender = createInfantry(player2, new Coords(5, 5), 28);

        // Add to game - building needs special initialization
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);  // Must set ID before adding to game
        game.addEntity(building);
        game.addEntity(attacker1);
        game.addEntity(attacker2);
        game.addEntity(defender);

        // Set up combat matching production flow: building crew + infantry defender
        InfantryActionTracker tracker = gameManager.getInfantryCombatTracker();
        tracker.addCombat(building.getId(), attacker1, building);  // Building crew first
        tracker.addReinforcement(building.getId(), attacker2, true);  // Second attacker
        tracker.addReinforcement(building.getId(), defender, false);  // Infantry defender

        // Verify combat is active
        assertTrue(tracker.hasCombat(building.getId()));

        // Simulate attackers being killed externally
        attacker1.setDestroyed(true);
        attacker2.setDestroyed(true);

        // Resolve combat - should detect all attackers dead and end combat
        gameManager.resolveInfantryActions();

        // Verify combat ended
        assertFalse(tracker.hasCombat(building.getId()),
            "Combat should end when all attackers are destroyed");
        assertEquals(Entity.NONE, building.getInfantryCombatTargetId(),
            "Building crew combat state should be cleared");
        assertEquals(Entity.NONE, defender.getInfantryCombatTargetId(),
            "Defender combat state should be cleared");
    }

    /**
     * Test combat ends when building crew is reduced to zero.
     */
    @Test
    void testCombatEnds_WhenBuildingCrewDefeated() {
        // Create building with crew and attacker
        AbstractBuildingEntity building = createBuildingWithCrew(player2, new Coords(5, 5), 10);
        Infantry attacker = createInfantry(player1, new Coords(5, 5), 28);

        // Add to game - building needs special initialization
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);  // Must set ID before adding to game
        game.addEntity(building);
        game.addEntity(attacker);

        // Set up combat with building as defender
        InfantryActionTracker tracker = gameManager.getInfantryCombatTracker();
        tracker.addCombat(building.getId(), attacker, building);

        // Verify combat is active
        assertTrue(tracker.hasCombat(building.getId()));

        // Reduce building crew to zero (simulate casualties)
        building.getCrew().setCurrentSize(0);

        // Resolve combat - should detect building crew defeated and end combat
        gameManager.resolveInfantryActions();

        // Verify combat ended
        assertFalse(tracker.hasCombat(building.getId()),
            "Combat should end when building crew is defeated");
    }

    /**
     * Test combat continues if only some defenders eliminated.
     * Per production code, building crew is ALWAYS a defender (compromise until player choice implemented).
     * TODO: Update when player choice for crew participation is implemented
     *
     * NOTE: Uses high strength units (100+ troopers) so that casualties from dice rolls
     * won't eliminate remaining combatants, allowing us to test the "some defenders remain" logic.
     */
    @Test
    void testCombatContinues_WhenSomeDefendersRemain() {
        // Create building WITH crew (crew always defends - see TWGameManager line 15282-15288)
        AbstractBuildingEntity building = createBuildingWithCrew(player2, new Coords(5, 5), 50);
        Infantry attacker = createInfantry(player1, new Coords(5, 5), 100);
        Infantry defender1 = createInfantry(player2, new Coords(5, 5), 100);
        Infantry defender2 = createInfantry(player2, new Coords(5, 5), 100);

        // Add to game - building needs special initialization
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);  // Must set ID before adding to game
        game.addEntity(building);
        game.addEntity(attacker);
        game.addEntity(defender1);
        game.addEntity(defender2);

        // Set up combat matching production flow: building crew + infantry defenders
        InfantryActionTracker tracker = gameManager.getInfantryCombatTracker();
        tracker.addCombat(building.getId(), attacker, building);  // Building crew is first defender
        tracker.addReinforcement(building.getId(), defender1, false);
        tracker.addReinforcement(building.getId(), defender2, false);

        // Kill only one infantry defender (building crew + defender2 remain)
        defender1.setDestroyed(true);

        // Mock dice roll to ensure consistent result that doesn't end combat
        // Roll 7 gives standard casualties at 2:3 ratio without ending combat
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.d6(2)).thenReturn(7);

            // Resolve combat - should continue with building crew + remaining defender
            gameManager.resolveInfantryActions();

            // Verify combat still active
            assertTrue(tracker.hasCombat(building.getId()),
                "Combat should continue when defenders remain (building crew + defender2)");

            // Verify combat state still set
            assertNotEquals(Entity.NONE, attacker.getInfantryCombatTargetId(),
                "Attacker should still be in combat");
            assertNotEquals(Entity.NONE, building.getInfantryCombatTargetId(),
                "Building crew should still be defending");
            assertNotEquals(Entity.NONE, defender2.getInfantryCombatTargetId(),
                "Remaining defender should still be in combat");
        }
    }

    /**
     * Test combat continues if only some attackers eliminated.
     * Building crew is always a defender per current implementation.
     * TODO: Update when player choice for crew participation is implemented
     *
     * NOTE: Uses high strength units (100+ troopers) so that casualties from dice rolls
     * won't eliminate remaining combatants, allowing us to test the "some attackers remain" logic.
     */
    @Test
    void testCombatContinues_WhenSomeAttackersRemain() {
        // Create building WITH crew (crew always defends)
        AbstractBuildingEntity building = createBuildingWithCrew(player2, new Coords(5, 5), 50);
        Infantry attacker1 = createInfantry(player1, new Coords(5, 5), 100);
        Infantry attacker2 = createInfantry(player1, new Coords(5, 5), 100);
        Infantry defender = createInfantry(player2, new Coords(5, 5), 100);

        // Add to game - building needs special initialization
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);  // Must set ID before adding to game
        game.addEntity(building);
        game.addEntity(attacker1);
        game.addEntity(attacker2);
        game.addEntity(defender);

        // Set up combat matching production flow: building crew + infantry defender
        InfantryActionTracker tracker = gameManager.getInfantryCombatTracker();
        tracker.addCombat(building.getId(), attacker1, building);  // Building crew is first defender
        tracker.addReinforcement(building.getId(), attacker2, true);  // Second attacker
        tracker.addReinforcement(building.getId(), defender, false);  // Infantry defender

        // Kill only one attacker (attacker2 remains)
        attacker1.setDestroyed(true);

        // Mock dice roll to ensure consistent result that doesn't end combat
        // Roll 7 gives standard casualties without ending combat
        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.d6(2)).thenReturn(7);

            // Resolve combat - should continue with remaining attacker vs building crew + defender
            gameManager.resolveInfantryActions();

            // Verify combat still active
            assertTrue(tracker.hasCombat(building.getId()),
                "Combat should continue when attackers remain");
            assertNotEquals(Entity.NONE, attacker2.getInfantryCombatTargetId(),
                "Remaining attacker should still be in combat");
            assertNotEquals(Entity.NONE, building.getInfantryCombatTargetId(),
                "Building crew should still be defending");
            assertNotEquals(Entity.NONE, defender.getInfantryCombatTargetId(),
                "Infantry defender should still be in combat");
        }
    }

    // ==================== Helper Methods ====================

    private Infantry createInfantry(Player owner, Coords position, int strength) {
        Infantry infantry = new Infantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setPosition(position);
        infantry.setSquadSize(strength);
        infantry.setSquadCount(1);
        infantry.initializeInternal(strength, Infantry.LOC_INFANTRY);
        return infantry;
    }

    private AbstractBuildingEntity createBuilding(Player owner, Coords position) {
        AbstractBuildingEntity building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.setOwner(owner);
        building.setGame(game);
        building.setPosition(position);
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().addHex(
            new CubeCoords(position.getX(), -position.getX() - position.getY(), position.getY()),
            50, 10, BasementType.UNKNOWN, false
        );
        return building;
    }

    private AbstractBuildingEntity createBuildingWithCrew(Player owner, Coords position, int crewSize) {
        AbstractBuildingEntity building = createBuilding(owner, position);
        building.getCrew().setCurrentSize(crewSize);
        return building;
    }

    /**
     * Testable subclass of TWGameManager that exposes package-private methods for testing.
     * Uses reflection to access the private infantryActionTracker field.
     */
    private static class TestableGameManager extends TWGameManager {
        public InfantryActionTracker getInfantryCombatTracker() {
            try {
                var field = TWGameManager.class.getDeclaredField("infantryActionTracker");
                field.setAccessible(true);
                return (InfantryActionTracker) field.get(this);
            } catch (Exception e) {
                throw new RuntimeException("Failed to access infantryActionTracker", e);
            }
        }

        // resolveInfantryActions is package-private, so accessible from test
    }
}
