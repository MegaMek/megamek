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

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.RulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A unit the rules system gives no movement turn must not stay selectable, or picking it spends a turn that belongs to
 * another unit of the same player (issue 9550: under Core Rules an immobile vehicle cost the player one move a round).
 */
class ImmobileUnitMovementTurnTest {

    private RulesManager previousRulesManager;
    private TWGameManager gameManager;
    private Server server;
    private Game game;
    private Player player;
    private Player opponent;
    private Tank immobileTank;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void buildGame() throws IOException {
        previousRulesManager = Game.rulesManager;
        gameManager = new TWGameManager();
        server = ServerFactory.createServer(gameManager);
        game = gameManager.getGame();
        game.setBoard(new Board(16, 17));

        player = new Player(0, "Player");
        player.setTeam(1);
        opponent = new Player(1, "Opponent");
        opponent.setTeam(2);
        game.addPlayer(player.getId(), player);
        game.addPlayer(opponent.getId(), opponent);
        game.setupTeams();

        addUnit(new BipedMek(), player, new Coords(2, 2));
        addUnit(new BipedMek(), player, new Coords(3, 2));
        addUnit(new BipedMek(), opponent, new Coords(10, 10));

        immobileTank = new Tank();
        addUnit(immobileTank, player, new Coords(4, 2));
        immobileTank.immobilize();
        immobileTank.applyDamage();
    }

    @AfterEach
    void tearDown() {
        server.die();
        Game.rulesManager = previousRulesManager;
    }

    private void addUnit(Entity entity, Player owner, Coords position) {
        entity.setChassis("Test");
        entity.setModel(owner.getName() + " " + game.getNextEntityId());
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setId(game.getNextEntityId());
        entity.setOwner(owner);
        entity.setDeployed(true);
        entity.setPosition(position);
        game.addEntity(entity);
    }

    private void startMovementPhase() {
        game.setPhase(GamePhase.MOVEMENT);
        gameManager.resetEntityPhase(GamePhase.MOVEMENT);
        gameManager.setIneligible(GamePhase.MOVEMENT);
        gameManager.determineTurnOrder(GamePhase.MOVEMENT);
    }

    private int turnsFor(Player owner) {
        int turns = 0;
        for (GameTurn turn : game.getTurnsList()) {
            if (turn.playerId() == owner.getId()) {
                turns++;
            }
        }
        return turns;
    }

    private int selectableUnitsOf(Player owner) {
        int selectable = 0;
        for (Entity entity : game.getEntitiesVector()) {
            if ((entity.getOwnerId() == owner.getId()) && entity.isSelectableThisTurn()) {
                selectable++;
            }
        }
        return selectable;
    }

    @Test
    @DisplayName("Core Rules: an immobile vehicle gets no movement turn and cannot be selected")
    void coreRulesImmobileVehicleIsDone() {
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        assertTrue(immobileTank.isImmobile(), "the test needs an immobile vehicle");

        startMovementPhase();

        assertEquals(2, turnsFor(player), "only the two Meks get a movement turn");
        assertTrue(immobileTank.isDone(), "a unit without a turn must be done, or it can take another unit's turn");
        assertEquals(turnsFor(player), selectableUnitsOf(player),
              "the player must have exactly one turn for each unit they can select");
    }

    @Test
    @DisplayName("Total Warfare: an immobile vehicle keeps its movement turn")
    void totalWarfareImmobileVehicleKeepsItsTurn() {
        game.initializeRulesManager(OptionsConstants.RULES_TW);
        assertTrue(immobileTank.isImmobile(), "the test needs an immobile vehicle");

        startMovementPhase();

        assertEquals(3, turnsFor(player), "the immobile vehicle still gets a turn");
        assertFalse(immobileTank.isDone());
        assertEquals(turnsFor(player), selectableUnitsOf(player),
              "the player must have exactly one turn for each unit they can select");
    }
}
