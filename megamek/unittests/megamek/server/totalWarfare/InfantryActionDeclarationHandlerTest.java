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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import megamek.common.Player;
import megamek.common.actions.InitiateInfantryCombatAction;
import megamek.common.actions.ReinforceInfantryCombatAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
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
 * Starting and joining an infantry action (TO:AR pp. 169 to 172): everyone the initiator commits attacks, every
 * enemy in any hex of the building defends, and a unit joins the side it belongs to.
 */
@DisplayName("Infantry action declarations")
class InfantryActionDeclarationHandlerTest {

    private static final Coords HEX_A = new Coords(5, 5);
    private static final Coords HEX_B = new Coords(6, 5);

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

        // A two-hex emplacement with a crew, anchored on hex A and reaching hex B
        building = new BuildingEntity(BuildingType.MEDIUM, 3);
        building.setOwner(defendingPlayer);
        building.setGame(game);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 20, BasementType.NONE, false);
        building.getInternalBuilding().addHex(new CubeCoords(1, -1, 0), 40, 20, BasementType.NONE, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.addEquipment(new WeaponMounted(building, new ISLaserMedium()), 0, false);
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

    @Test
    @DisplayName("The initiator and the units it commits all attack, and a committed unit's turn is spent")
    void committedUnitsAttackTogether() {
        ConvInfantry initiator = platoon(attackingPlayer, 1, HEX_A);
        ConvInfantry committed = platoon(attackingPlayer, 2, HEX_A);
        platoon(defendingPlayer, 3, HEX_A);

        handler.process(new InitiateInfantryCombatAction(initiator.getId(), building.getId(),
              List.of(committed.getId())));

        var combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertEquals(List.of(initiator.getId(), committed.getId()), combat.attackerIds);
        assertTrue(committed.isInfantryCombatAttacker());
        assertTrue(committed.isDone(), "the committed unit has no declaration left to make");
    }

    @Test
    @DisplayName("Enemy infantry in any hex of the building defends, with the crew")
    void defendersComeFromEveryHexOfTheBuilding() {
        ConvInfantry initiator = platoon(attackingPlayer, 1, HEX_A);
        ConvInfantry inTheOtherHex = platoon(defendingPlayer, 2, HEX_B);

        handler.process(new InitiateInfantryCombatAction(initiator.getId(), building.getId()));

        var combat = tracker.getCombat(building.getId());
        assertNotNull(combat);
        assertEquals(List.of(building.getId(), inTheOtherHex.getId()), combat.defenderIds);
    }

    @Test
    @DisplayName("A defender's unit that enters later joins the defenders, not the attackers")
    void defendingPlayersUnitJoinsTheDefenders() {
        ConvInfantry initiator = platoon(attackingPlayer, 1, HEX_A);
        platoon(defendingPlayer, 2, HEX_A);
        handler.process(new InitiateInfantryCombatAction(initiator.getId(), building.getId()));
        ConvInfantry lateDefender = platoon(defendingPlayer, 3, HEX_A);

        handler.process(new ReinforceInfantryCombatAction(lateDefender.getId(), building.getId()));

        var combat = tracker.getCombat(building.getId());
        assertTrue(combat.defenderIds.contains(lateDefender.getId()));
        assertFalse(combat.attackerIds.contains(lateDefender.getId()));
        assertFalse(lateDefender.isInfantryCombatAttacker());
    }

    @Test
    @DisplayName("An attacker's unit that enters later joins the attackers")
    void attackingPlayersUnitJoinsTheAttackers() {
        ConvInfantry initiator = platoon(attackingPlayer, 1, HEX_A);
        platoon(defendingPlayer, 2, HEX_A);
        handler.process(new InitiateInfantryCombatAction(initiator.getId(), building.getId()));
        ConvInfantry lateAttacker = platoon(attackingPlayer, 3, HEX_A);

        handler.process(new ReinforceInfantryCombatAction(lateAttacker.getId(), building.getId()));

        assertTrue(tracker.getCombat(building.getId()).attackerIds.contains(lateAttacker.getId()));
    }

    @Test
    @DisplayName("A committed unit that is not inside the building is refused")
    void committedUnitOutsideIsRefused() {
        ConvInfantry initiator = platoon(attackingPlayer, 1, HEX_A);
        ConvInfantry outside = platoon(attackingPlayer, 2, new Coords(9, 9));
        platoon(defendingPlayer, 3, HEX_A);

        handler.process(new InitiateInfantryCombatAction(initiator.getId(), building.getId(),
              List.of(outside.getId())));

        assertEquals(List.of(initiator.getId()), tracker.getCombat(building.getId()).attackerIds);
        assertEquals(Entity.NONE, outside.getInfantryCombatTargetId());
    }
}
