/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.server.totalWarfare.InfantryActionTracker.InfantryAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InfantryActionTracker} focusing on tracking
 * infantry vs. infantry actions in buildings, ships, and aerospace units (TOAR p. 167-174).
 */
public class InfantryActionTrackerTest {

    private InfantryActionTracker tracker;
    private megamek.common.game.Game game;
    private Player player1;
    private Player player2;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        tracker = new InfantryActionTracker();
        game = new megamek.common.game.Game();
        player1 = new Player(0, "Player 1");
        player2 = new Player(1, "Player 2");
        game.addPlayer(0, player1);
        game.addPlayer(1, player2);
    }

    /**
     * Test adding a new combat with real BuildingEntity.
     */
    @Test
    void testAddCombat_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);
        Infantry defender = createInfantry(player2, 2);

        tracker.addCombat(building.getId(), attacker, defender);

        assertTrue(tracker.hasCombat(building.getId()));
        InfantryAction combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertEquals(building.getId(), combat.targetId);
        assertEquals(1, combat.attackerIds.size());
        assertEquals(1, combat.defenderIds.size());
        assertTrue(combat.attackerIds.contains(attacker.getId()));
        assertTrue(combat.defenderIds.contains(defender.getId()));
    }

    /**
     * Test combat with BuildingEntity as defender.
     */
    @Test
    void testAddCombat_BuildingAsDefender() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);

        // Building can be the defender (building has crew)
        tracker.addCombat(building.getId(), attacker, building);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertEquals(1, combat.attackerIds.size());
        assertEquals(1, combat.defenderIds.size());
        assertTrue(combat.attackerIds.contains(attacker.getId()));
        assertTrue(combat.defenderIds.contains(building.getId()));
    }

    /**
     * Test adding multiple attackers to same building.
     */
    @Test
    void testAddCombat_MultipleAttackers_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender = createInfantry(player2, 3);

        tracker.addCombat(building.getId(), attacker1, defender);
        tracker.addCombat(building.getId(), attacker2, null);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertEquals(2, combat.attackerIds.size());
        assertEquals(1, combat.defenderIds.size());
        assertTrue(combat.attackerIds.contains(attacker1.getId()));
        assertTrue(combat.attackerIds.contains(attacker2.getId()));
    }

    /**
     * Test adding reinforcements to existing combat.
     */
    @Test
    void testAddReinforcement_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);
        Infantry defender = createInfantry(player2, 2);
        Infantry reinforcement = createInfantry(player2, 3);

        tracker.addCombat(building.getId(), attacker, defender);
        boolean added = tracker.addReinforcement(building.getId(), reinforcement, false);

        assertTrue(added);
        InfantryAction combat = tracker.getCombat(building.getId());
        assertEquals(1, combat.attackerIds.size());
        assertEquals(2, combat.defenderIds.size());
        assertTrue(combat.defenderIds.contains(reinforcement.getId()));
    }

    /**
     * Test removing an entity from combat.
     */
    @Test
    void testRemoveEntity_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);
        Infantry defender = createInfantry(player2, 2);

        tracker.addCombat(building.getId(), attacker, defender);
        tracker.removeEntity(building.getId(), attacker.getId());

        // Combat should be removed since attacker is gone
        assertFalse(tracker.hasCombat(building.getId()));
    }

    /**
     * Test withdrawing all attackers.
     */
    @Test
    void testWithdrawAttackers_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender = createInfantry(player2, 3);

        tracker.addCombat(building.getId(), attacker1, defender);
        tracker.addCombat(building.getId(), attacker2, null);

        var withdrawn = tracker.withdrawAttackers(building.getId());

        assertEquals(2, withdrawn.size());
        assertTrue(withdrawn.contains(attacker1.getId()));
        assertTrue(withdrawn.contains(attacker2.getId()));
        assertFalse(tracker.hasCombat(building.getId()));
    }

    /**
     * Test partial control flag.
     */
    @Test
    void testPartialControl_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);
        Infantry defender = createInfantry(player2, 2);

        tracker.addCombat(building.getId(), attacker, defender);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertFalse(combat.hasPartialControl);

        // Achieve partial control
        combat.hasPartialControl = true;
        assertTrue(combat.hasPartialControl);
    }

    /**
     * Test turn counter increment.
     */
    @Test
    void testIncrementTurnCounters_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);
        Infantry defender = createInfantry(player2, 2);

        tracker.addCombat(building.getId(), attacker, defender);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertEquals(0, combat.turnCount);

        tracker.incrementAllTurnCounters();
        assertEquals(1, combat.turnCount);

        tracker.incrementAllTurnCounters();
        assertEquals(2, combat.turnCount);
    }

    /**
     * Test combat activity detection.
     */
    @Test
    void testIsActive_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);
        Infantry defender = createInfantry(player2, 2);

        tracker.addCombat(building.getId(), attacker, defender);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertTrue(combat.isActive());

        // Remove all attackers
        combat.attackerIds.clear();
        assertFalse(combat.isActive());
    }

    /**
     * Test entity state tracking.
     */
    @Test
    void testEntityStateTracking_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);
        Infantry defender = createInfantry(player2, 2);

        tracker.addCombat(building.getId(), attacker, defender);

        // Check attacker state
        assertEquals(building.getId(), attacker.getInfantryCombatTargetId());
        assertTrue(attacker.isInfantryCombatAttacker());
        assertEquals(0, attacker.getInfantryCombatTurnCount());

        // Check defender state
        assertEquals(building.getId(), defender.getInfantryCombatTargetId());
        assertFalse(defender.isInfantryCombatAttacker());
        assertEquals(0, defender.getInfantryCombatTurnCount());
    }

    /**
     * Test clearing all combats.
     */
    @Test
    void testClearAll_RealBuildingEntity() {
        AbstractBuildingEntity building1 = createBuilding(player2, 100);
        AbstractBuildingEntity building2 = createBuilding(player2, 101);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender1 = createInfantry(player2, 3);
        Infantry defender2 = createInfantry(player2, 4);

        tracker.addCombat(building1.getId(), attacker1, defender1);
        tracker.addCombat(building2.getId(), attacker2, defender2);

        assertEquals(2, tracker.getActiveCombatCount());

        tracker.clearAll();

        assertEquals(0, tracker.getActiveCombatCount());
        assertFalse(tracker.hasCombat(building1.getId()));
        assertFalse(tracker.hasCombat(building2.getId()));
    }

    /**
     * Test getting all combats.
     */
    @Test
    void testGetAllCombats_RealBuildingEntity() {
        AbstractBuildingEntity building1 = createBuilding(player2, 100);
        AbstractBuildingEntity building2 = createBuilding(player2, 101);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender1 = createInfantry(player2, 3);
        Infantry defender2 = createInfantry(player2, 4);

        tracker.addCombat(building1.getId(), attacker1, defender1);
        tracker.addCombat(building2.getId(), attacker2, defender2);

        var allCombats = tracker.getAllCombats();
        assertEquals(2, allCombats.size());
        assertTrue(allCombats.containsKey(building1.getId()));
        assertTrue(allCombats.containsKey(building2.getId()));
    }

    /**
     * Test removing combat.
     */
    @Test
    void testRemoveCombat_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);
        Infantry defender = createInfantry(player2, 2);

        tracker.addCombat(building.getId(), attacker, defender);
        assertTrue(tracker.hasCombat(building.getId()));

        InfantryAction removed = tracker.removeCombat(building.getId());
        assertNotNull(removed);
        assertEquals(building.getId(), removed.targetId);
        assertFalse(tracker.hasCombat(building.getId()));
    }

    /**
     * Test total combatants calculation.
     */
    @Test
    void testGetTotalCombatants_RealBuildingEntity() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender = createInfantry(player2, 3);

        tracker.addCombat(building.getId(), attacker1, defender);
        tracker.addCombat(building.getId(), attacker2, null);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertEquals(3, combat.getTotalCombatants());
    }

    // ==================== Combat Ending Tests ====================

    /**
     * Test combat ends when all defenders are eliminated.
     * Verifies that when the last defender (including building crew) is removed,
     * the combat is properly cleaned up and removed from tracking.
     */
    @Test
    void testCombatEnds_WhenAllDefendersEliminated() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender1 = createInfantry(player2, 3);
        Infantry defender2 = createInfantry(player2, 4);

        // Set up combat with 2 attackers vs 2 defenders
        tracker.addCombat(building.getId(), attacker1, defender1);
        tracker.addCombat(building.getId(), attacker2, defender2);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertTrue(combat.isActive());
        assertEquals(2, combat.attackerIds.size());
        assertEquals(2, combat.defenderIds.size());

        // Eliminate first defender
        tracker.removeEntity(building.getId(), defender1.getId());
        combat = tracker.getCombat(building.getId());
        assertNotNull(combat, "Combat should still exist with one defender remaining");
        assertTrue(combat.isActive(), "Combat should still be active");
        assertEquals(2, combat.attackerIds.size());
        assertEquals(1, combat.defenderIds.size());

        // Eliminate last defender - combat should end
        tracker.removeEntity(building.getId(), defender2.getId());
        combat = tracker.getCombat(building.getId());
        assertNull(combat, "Combat should be removed when all defenders are eliminated");
        assertFalse(tracker.hasCombat(building.getId()), "Tracker should not have this combat anymore");
    }

    /**
     * Test combat ends when building (as defender) crew is defeated.
     * Verifies that when a building's crew is eliminated and there are no
     * other defenders, the combat properly ends.
     */
    @Test
    void testCombatEnds_WhenBuildingCrewDefeated() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker = createInfantry(player1, 1);

        // Building itself is the defender (has crew)
        tracker.addCombat(building.getId(), attacker, building);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertTrue(combat.isActive());
        assertEquals(1, combat.attackerIds.size());
        assertEquals(1, combat.defenderIds.size());
        assertTrue(combat.defenderIds.contains(building.getId()));

        // Building crew is eliminated
        tracker.removeEntity(building.getId(), building.getId());

        combat = tracker.getCombat(building.getId());
        assertNull(combat, "Combat should end when building crew is defeated");
        assertFalse(tracker.hasCombat(building.getId()));
    }

    /**
     * Test combat ends when all attackers are eliminated.
     * Verifies that when the last attacker is removed (killed or withdrawn),
     * the combat is properly cleaned up.
     */
    @Test
    void testCombatEnds_WhenAllAttackersEliminated() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender = createInfantry(player2, 3);

        // Set up combat with 2 attackers vs 1 defender
        tracker.addCombat(building.getId(), attacker1, defender);
        tracker.addCombat(building.getId(), attacker2, null);

        InfantryAction combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertTrue(combat.isActive());
        assertEquals(2, combat.attackerIds.size());
        assertEquals(1, combat.defenderIds.size());

        // Eliminate first attacker
        tracker.removeEntity(building.getId(), attacker1.getId());
        combat = tracker.getCombat(building.getId());
        assertNotNull(combat, "Combat should still exist with one attacker remaining");
        assertTrue(combat.isActive(), "Combat should still be active");
        assertEquals(1, combat.attackerIds.size());
        assertEquals(1, combat.defenderIds.size());

        // Eliminate last attacker - combat should end
        tracker.removeEntity(building.getId(), attacker2.getId());
        combat = tracker.getCombat(building.getId());
        assertNull(combat, "Combat should be removed when all attackers are eliminated");
        assertFalse(tracker.hasCombat(building.getId()), "Tracker should not have this combat anymore");
    }

    /**
     * Test combat ends when attackers withdraw.
     * Verifies that withdrawing all attackers properly ends the combat.
     */
    @Test
    void testCombatEnds_WhenAttackersWithdraw() {
        AbstractBuildingEntity building = createBuilding(player2, 100);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender = createInfantry(player2, 3);

        // Set up combat
        tracker.addCombat(building.getId(), attacker1, defender);
        tracker.addCombat(building.getId(), attacker2, null);

        assertTrue(tracker.hasCombat(building.getId()));
        InfantryAction combat = tracker.getCombat(building.getId());
        assertTrue(combat.isActive());

        // All attackers withdraw
        var withdrawn = tracker.withdrawAttackers(building.getId());

        assertEquals(2, withdrawn.size(), "Both attackers should be withdrawn");
        assertFalse(tracker.hasCombat(building.getId()), "Combat should end when all attackers withdraw");
        assertNull(tracker.getCombat(building.getId()), "Combat should be removed from tracker");
    }

    /**
     * Test multiple combats can end independently.
     * Verifies that ending one combat doesn't affect other ongoing combats.
     */
    @Test
    void testMultipleCombats_EndIndependently() {
        AbstractBuildingEntity building1 = createBuilding(player2, 100);
        AbstractBuildingEntity building2 = createBuilding(player2, 101);
        Infantry attacker1 = createInfantry(player1, 1);
        Infantry attacker2 = createInfantry(player1, 2);
        Infantry defender1 = createInfantry(player2, 3);
        Infantry defender2 = createInfantry(player2, 4);

        // Set up two separate combats
        tracker.addCombat(building1.getId(), attacker1, defender1);
        tracker.addCombat(building2.getId(), attacker2, defender2);

        assertEquals(2, tracker.getActiveCombatCount());

        // End first combat by eliminating all defenders
        tracker.removeEntity(building1.getId(), defender1.getId());

        assertFalse(tracker.hasCombat(building1.getId()), "First combat should have ended");
        assertTrue(tracker.hasCombat(building2.getId()), "Second combat should still be active");
        assertEquals(1, tracker.getActiveCombatCount());

        // End second combat
        tracker.removeEntity(building2.getId(), defender2.getId());

        assertFalse(tracker.hasCombat(building2.getId()), "Second combat should have ended");
        assertEquals(0, tracker.getActiveCombatCount());
    }

    // ==================== Helper Methods ====================

    private Infantry createInfantry(Player owner, int id) {
        Infantry infantry = new Infantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setId(id);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, Infantry.LOC_INFANTRY);
        return infantry;
    }

    private AbstractBuildingEntity createBuilding(Player owner, int id) {
        AbstractBuildingEntity building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.setOwner(owner);
        building.setId(id);
        building.setPosition(new Coords(5, 5));
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().addHex(
            new CubeCoords(0, 0, 0),
            50, 10, BasementType.UNKNOWN, false
        );
        return building;
    }
}
