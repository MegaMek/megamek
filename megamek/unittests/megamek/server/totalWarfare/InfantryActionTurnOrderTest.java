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

import java.util.List;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The declaration phase asks the attacker first, whatever initiative said (TO:AR pp. 169 to 172).
 */
@DisplayName("Infantry action declaration turn order")
class InfantryActionTurnOrderTest {

    private static final Coords BUILDING_HEX = new Coords(5, 5);

    private Game game;
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

        building = new BuildingEntity(BuildingType.MEDIUM, 3);
        building.setOwner(defendingPlayer);
        building.setGame(game);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 20, BasementType.NONE, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.addEquipment(new WeaponMounted(building, new ISLaserMedium()), 0, false);
        building.getCrew().setSize(4);
        building.getCrew().setCurrentSize(4);
        building.setId(0);
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
    }

    private void platoon(Player owner, int id, Coords position) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        infantry.setId(id);
        game.addEntity(infantry);
        infantry.setPosition(position);
    }

    private List<Integer> turnPlayers() {
        return game.getTurnsList().stream().map(GameTurn::playerId).toList();
    }

    @Test
    @DisplayName("The player with infantry inside an enemy building declares before the building's owner")
    void attackerDeclaresFirst() {
        platoon(attackingPlayer, 1, BUILDING_HEX);
        game.setTurnVector(List.of(new GameTurn(defendingPlayer.getId()), new GameTurn(attackingPlayer.getId())));

        assertTrue(InfantryActionTurnOrder.putAttackersFirst(game), "the order changed, so the clients need it");

        assertEquals(List.of(attackingPlayer.getId(), defendingPlayer.getId()), turnPlayers());
    }

    @Test
    @DisplayName("With nobody attacking, initiative order stands")
    void initiativeOrderStandsWithoutAnAttacker() {
        platoon(attackingPlayer, 1, new Coords(9, 9));
        game.setTurnVector(List.of(new GameTurn(defendingPlayer.getId()), new GameTurn(attackingPlayer.getId())));

        assertFalse(InfantryActionTurnOrder.putAttackersFirst(game), "nothing changed, nothing to send");

        assertEquals(List.of(defendingPlayer.getId(), attackingPlayer.getId()), turnPlayers());
    }
}
