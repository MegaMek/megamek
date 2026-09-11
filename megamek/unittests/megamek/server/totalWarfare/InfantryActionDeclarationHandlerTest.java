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

import java.util.List;

import megamek.common.InfantryActionDeclaration;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Declaring an infantry action (TO:AR pp. 169 to 172): the attacker commits units or withdraws the force, the
 * defender commits infantry and crew, and each declaration lands on the right side.
 */
@DisplayName("Infantry action declarations")
class InfantryActionDeclarationHandlerTest {

    private static final Coords HEX_A = new Coords(5, 5);
    private static final Coords HEX_B = new Coords(6, 5);
    private static final int CREW = 4;

    private Game game;
    private TWGameManager gameManager;
    private InfantryActionTracker tracker;
    private InfantryActionDeclarationHandler handler;
    private Player attackingPlayer;
    private Player defendingPlayer;
    private BuildingEntity building;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        game = new Game();
        attackingPlayer = new Player(0, "Attacker");
        defendingPlayer = new Player(1, "Defender");
        attackingPlayer.setTeam(Player.TEAM_NONE);
        defendingPlayer.setTeam(Player.TEAM_NONE);
        game.addPlayer(0, attackingPlayer);
        game.addPlayer(1, defendingPlayer);
        game.setBoard(new Board(16, 17));
        game.setPhase(GamePhase.PREEND_DECLARATIONS);
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        gameManager.setGame(game);
        tracker = new InfantryActionTracker();
        handler = new InfantryActionDeclarationHandler(gameManager, tracker);

        // A two-hex emplacement with four crew, anchored on hex A and reaching hex B
        building = new BuildingEntity(BuildingType.MEDIUM, 3);
        building.setOwner(defendingPlayer);
        building.setGame(game);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 20, BasementType.NONE, false);
        building.getInternalBuilding().addHex(new CubeCoords(1, -1, 0), 40, 20, BasementType.NONE, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.addEquipment(new WeaponMounted(building, new ISLaserMedium()), 0, false);
        building.getCrew().setSize(CREW);
        building.getCrew().setCurrentSize(CREW);
        building.setId(0);
        game.addEntity(building);
        building.setPosition(HEX_A);
    }

    private ConvInfantry platoon(Player owner, int id, Coords position) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        infantry.setId(id);
        game.addEntity(infantry);
        infantry.setPosition(position);
        return infantry;
    }

    private void attackerDeclares(List<Integer> unitIds, boolean withdraw) {
        game.setTurnVector(List.of(new GameTurn(attackingPlayer.getId())));
        game.setTurnIndex(0, Player.PLAYER_NONE);
        handler.declare(InfantryActionDeclaration.attacking(attackingPlayer.getId(), building.getId(), unitIds,
              withdraw), attackingPlayer.getId());
    }

    private void defenderDeclares(List<Integer> unitIds, int crew) {
        game.setTurnVector(List.of(new GameTurn(defendingPlayer.getId())));
        game.setTurnIndex(0, Player.PLAYER_NONE);
        handler.declare(InfantryActionDeclaration.defending(defendingPlayer.getId(), building.getId(), unitIds,
              crew), defendingPlayer.getId());
    }

    private List<Integer> turnPlayers() {
        return game.getTurnsList().stream().map(GameTurn::playerId).toList();
    }

    @Test
    @DisplayName("The attacker's committed units all attack together")
    void committedUnitsAttackTogether() {
        ConvInfantry first = platoon(attackingPlayer, 1, HEX_A);
        ConvInfantry second = platoon(attackingPlayer, 2, HEX_B);

        attackerDeclares(List.of(first.getId(), second.getId()), false);

        var combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertEquals(List.of(first.getId(), second.getId()), combat.attackerIds);
        assertEquals(List.of(building.getId()), combat.defenderIds, "the crewed building defends by itself");
        // The clients learn the action is running from the units' state, so every unit that joined is sent
        Mockito.verify(gameManager).entityUpdate(first.getId());
        Mockito.verify(gameManager).entityUpdate(second.getId());
        Mockito.verify(gameManager).entityUpdate(building.getId());
    }

    @Test
    @DisplayName("Infantry the defender committed before the attack defends alongside the building")
    void defenderCommittedBeforeTheAttackDefends() {
        ConvInfantry attacker = platoon(attackingPlayer, 1, HEX_A);
        ConvInfantry defender = platoon(defendingPlayer, 2, HEX_B);
        ConvInfantry heldBack = platoon(defendingPlayer, 3, HEX_A);

        defenderDeclares(List.of(defender.getId()), 2);
        attackerDeclares(List.of(attacker.getId()), false);

        var combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertEquals(List.of(building.getId(), defender.getId()), combat.defenderIds);
        assertEquals(Entity.NONE, heldBack.getInfantryCombatTargetId(), "the unit held back stays out");
        assertEquals(2, building.getCommittedCrew());
        assertEquals(3, building.getCrew().getHits(), "two of four crew committed is 50 percent, three crew hits");
        Mockito.verify(gameManager, Mockito.atLeastOnce()).entityUpdate(defender.getId());
    }

    @Test
    @DisplayName("A defender's unit that enters later joins the defenders")
    void defendingPlayersUnitJoinsTheDefenders() {
        ConvInfantry attacker = platoon(attackingPlayer, 1, HEX_A);
        attackerDeclares(List.of(attacker.getId()), false);
        ConvInfantry lateDefender = platoon(defendingPlayer, 3, HEX_A);

        defenderDeclares(List.of(lateDefender.getId()), 0);

        var combat = tracker.getCombat(building.getId());
        assertTrue(combat.defenderIds.contains(lateDefender.getId()));
        assertFalse(combat.attackerIds.contains(lateDefender.getId()));
        assertFalse(lateDefender.isInfantryCombatAttacker());
    }

    @Test
    @DisplayName("Withdrawing flags the whole attacking force")
    void withdrawalFlagsTheWholeForce() {
        ConvInfantry first = platoon(attackingPlayer, 1, HEX_A);
        ConvInfantry second = platoon(attackingPlayer, 2, HEX_B);
        attackerDeclares(List.of(first.getId(), second.getId()), false);

        attackerDeclares(List.of(), true);

        assertTrue(first.isInfantryCombatWantsWithdrawal());
        assertTrue(second.isInfantryCombatWantsWithdrawal());
        assertNotNull(tracker.getCombat(building.getId()), "the action still rolls once more");
    }

    @Test
    @DisplayName("A committed unit outside the building is refused")
    void committedUnitOutsideIsRefused() {
        ConvInfantry inside = platoon(attackingPlayer, 1, HEX_A);
        ConvInfantry outside = platoon(attackingPlayer, 2, new Coords(9, 9));

        attackerDeclares(List.of(inside.getId(), outside.getId()), false);

        assertEquals(List.of(inside.getId()), tracker.getCombat(building.getId()).attackerIds);
        assertEquals(Entity.NONE, outside.getInfantryCombatTargetId());
    }

    @Test
    @DisplayName("A defence that no attack answered stands down at the End Phase")
    void unansweredDefenceStandsDown() {
        ConvInfantry defender = platoon(defendingPlayer, 2, HEX_A);
        defenderDeclares(List.of(defender.getId()), 3);
        assertEquals(building.getId(), defender.getInfantryCombatTargetId());

        handler.clearUnansweredDefences();

        assertEquals(Entity.NONE, defender.getInfantryCombatTargetId());
        assertEquals(0, building.getCommittedCrew());
        assertEquals(0, building.getCrew().getHits(), "no commitment, no penalty");
        assertNull(tracker.getCombat(building.getId()));
    }

    @Test
    @DisplayName("An attack on a building with no crew and no defending infantry still starts, so it can fall")
    void undefendedBuildingIsStillAttacked() {
        building.getCrew().setSize(0);
        building.getCrew().setCurrentSize(0);
        ConvInfantry attacker = platoon(attackingPlayer, 1, HEX_A);

        attackerDeclares(List.of(attacker.getId()), false);

        var combat = tracker.getCombat(building.getId());
        assertNotNull(combat, "the action exists; the End Phase finds nobody defending and the building falls");
        assertEquals(List.of(building.getId()), combat.defenderIds);
    }

    @Test
    @DisplayName("With the house rule on, a defender withdrawal flags the defending infantry; off, it is ignored")
    void defenderWithdrawalNeedsTheOption() {
        ConvInfantry attacker = platoon(attackingPlayer, 1, HEX_A);
        ConvInfantry defender = platoon(defendingPlayer, 2, HEX_B);
        attackerDeclares(List.of(attacker.getId()), false);
        defenderDeclares(List.of(defender.getId()), 0);

        game.setTurnVector(List.of(new GameTurn(defendingPlayer.getId())));
        game.setTurnIndex(0, Player.PLAYER_NONE);
        handler.declare(InfantryActionDeclaration.defending(defendingPlayer.getId(), building.getId(), List.of(), 0,
              true), defendingPlayer.getId());
        assertFalse(defender.isInfantryCombatWantsWithdrawal(), "the option is off by default");

        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_INFANTRY_ACTION_DEFENDER_WITHDRAWAL).setValue(true);
        handler.declare(InfantryActionDeclaration.defending(defendingPlayer.getId(), building.getId(), List.of(), 0,
              true), defendingPlayer.getId());
        assertTrue(defender.isInfantryCombatWantsWithdrawal(), "with the option on, the defence withdraws");
        assertNotNull(tracker.getCombat(building.getId()), "the action still rolls once more");
    }

    @Test
    @DisplayName("A declaration made when it is not the player's turn is refused")
    void outOfTurnIsRefused() {
        ConvInfantry attacker = platoon(attackingPlayer, 1, HEX_A);
        game.setTurnVector(List.of(new GameTurn(defendingPlayer.getId())));
        game.setTurnIndex(0, Player.PLAYER_NONE);

        handler.declare(InfantryActionDeclaration.attacking(attackingPlayer.getId(), building.getId(),
              List.of(attacker.getId()), false), attackingPlayer.getId());

        assertNull(tracker.getCombat(building.getId()));
    }

    @Test
    @DisplayName("When the defender's turn has already passed, an attack gives them a turn to answer it")
    void attackGivesTheDefenderATurnToAnswer() {
        ConvInfantry attacker = platoon(attackingPlayer, 1, HEX_A);
        // Initiative put the defender first; their turn is over and only the attacker's remains
        game.setTurnVector(List.of(new GameTurn(defendingPlayer.getId()), new GameTurn(attackingPlayer.getId())));
        game.setTurnIndex(1, defendingPlayer.getId());

        handler.declare(InfantryActionDeclaration.attacking(attackingPlayer.getId(), building.getId(),
              List.of(attacker.getId()), false), attackingPlayer.getId());

        assertNotNull(tracker.getCombat(building.getId()));
        assertEquals(List.of(defendingPlayer.getId(), attackingPlayer.getId(), defendingPlayer.getId()),
              turnPlayers(), "a turn for the defender follows the attacker's");
    }

    @Test
    @DisplayName("No turn is added for a defender who still has one, or who has nothing left to commit")
    void noExtraTurnWhenNotNeeded() {
        ConvInfantry attacker = platoon(attackingPlayer, 1, HEX_A);
        game.setTurnVector(List.of(new GameTurn(attackingPlayer.getId()), new GameTurn(defendingPlayer.getId())));
        game.setTurnIndex(0, Player.PLAYER_NONE);

        handler.declare(InfantryActionDeclaration.attacking(attackingPlayer.getId(), building.getId(),
              List.of(attacker.getId()), false), attackingPlayer.getId());
        assertEquals(List.of(attackingPlayer.getId(), defendingPlayer.getId()), turnPlayers(),
              "the defender's own turn is still to come");

        building.commitCrew(CREW);
        game.setTurnVector(List.of(new GameTurn(attackingPlayer.getId())));
        game.setTurnIndex(0, Player.PLAYER_NONE);
        handler.declare(InfantryActionDeclaration.attacking(attackingPlayer.getId(), building.getId(),
              List.of(), false), attackingPlayer.getId());
        assertEquals(List.of(attackingPlayer.getId()), turnPlayers(), "every crew member is already committed");
    }

    @Test
    @DisplayName("A declaration from the wrong connection is refused")
    void wrongConnectionIsRefused() {
        ConvInfantry attacker = platoon(attackingPlayer, 1, HEX_A);

        handler.declare(InfantryActionDeclaration.attacking(attackingPlayer.getId(), building.getId(),
              List.of(attacker.getId()), false), defendingPlayer.getId());

        assertNull(tracker.getCombat(building.getId()));
    }
}
