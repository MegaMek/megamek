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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The damage that reaches conventional infantry inside a building when the building is attacked from outside. The
 * building type sets the share that gets through (TW p. 172), every platoon inside takes that share in full, and the
 * share is then converted by the row of the attacking weapon (TW p. 216). The expected values include the worked
 * examples on TW p. 172: a large pulse laser into a medium building kills 3 troopers, an AC/20 kills 1.
 */
class BuildingOccupantDamageHandlerTest extends GameBoardTestCase {

    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final int FULL_PLATOON = 28;

    static {
        initializeBoard("OCCUPANT_DAMAGE_BOARD", """
              size 16 17
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private Board board;
    private BuildingEntity building;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        board = getBoard("OCCUPANT_DAMAGE_BOARD");
        game.setBoard(board);
        building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        building.getInternalBuilding().setBuildingHeight(2);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 100, 0, BasementType.UNKNOWN, false);
        building.setOwner(game.getPlayer(0));
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(game.getNextEntityId());
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);
        // Only the damage each platoon is dealt is under test, not how the platoon records it
        doReturn(new Vector<Report>()).when(gameManager).damageEntity(any(Entity.class), any(HitData.class),
              anyInt());
    }

    private ConvInfantry platoonInside() {
        ConvInfantry platoon = new ConvInfantry();
        platoon.setOwner(game.getPlayer(0));
        platoon.setGame(game);
        platoon.setSquadSize(FULL_PLATOON);
        platoon.setSquadCount(1);
        platoon.initializeInternal(FULL_PLATOON, ConvInfantry.LOC_INFANTRY);
        platoon.setId(game.getNextEntityId());
        game.addEntity(platoon);
        platoon.setPosition(BUILDING_HEX);
        platoon.setElevation(0);
        platoon.setDeployed(true);
        return platoon;
    }

    private void attack(int damage, int damageClass) {
        gameManager.damageInfantryIn(building, damage, BUILDING_HEX, damageClass);
    }

    private void assertTroopersHit(ConvInfantry platoon, int troopers) {
        verify(gameManager).damageEntity(eq(platoon), any(HitData.class), eq(troopers));
    }

    @Test
    @DisplayName("TW p. 172 example: a large pulse laser into a medium building kills 3 troopers")
    void largePulseLaserKillsThree() {
        ConvInfantry platoon = platoonInside();

        attack(10, WeaponType.WEAPON_PULSE);

        assertTroopersHit(platoon, 3);
    }

    @Test
    @DisplayName("TW p. 172 example: an AC/20 into a medium building kills 1 trooper")
    void autocannonTwentyKillsOne() {
        ConvInfantry platoon = platoonInside();

        attack(20, WeaponType.WEAPON_DIRECT_FIRE);

        assertTroopersHit(platoon, 1);
    }

    @Test
    @DisplayName("Twelve LRM hits are one attack: 6 reach the platoon, 2 troopers at 1/5 (TW p. 216)")
    void lrmVolleyIsConvertedAsClusterMissiles() {
        ConvInfantry platoon = platoonInside();

        attack(12, WeaponType.WEAPON_CLUSTER_MISSILE);

        assertTroopersHit(platoon, 2);
    }

    @Test
    @DisplayName("Damage from other infantry is never reduced by the table (TW p. 216)")
    void infantryDamageIsPointForPoint() {
        ConvInfantry platoon = platoonInside();

        attack(4, WeaponType.WEAPON_INFANTRY_ORIGIN);

        assertTroopersHit(platoon, 2);
    }

    @Test
    @DisplayName("A machine gun counts as direct fire inside a building, not its burst dice (TW p. 216)")
    void burstFireCountsAsDirectFire() {
        ConvInfantry platoon = platoonInside();

        attack(2, WeaponType.WEAPON_BURST_2D6);

        assertTroopersHit(platoon, 1);
    }

    @Test
    @DisplayName("Mechanized infantry take double from a non-infantry weapon (TW p. 216)")
    void mechanizedTakesDouble() {
        ConvInfantry platoon = platoonInside();
        platoon.setMovementMode(EntityMovementMode.TRACKED);

        attack(20, WeaponType.WEAPON_DIRECT_FIRE);

        assertTroopersHit(platoon, 2);
    }

    @Test
    @DisplayName("Every platoon inside takes the full share: damage is applied equally to all (TW p. 172)")
    void everyPlatoonTakesTheFullShare() {
        ConvInfantry first = platoonInside();
        ConvInfantry second = platoonInside();

        attack(10, WeaponType.WEAPON_PULSE);

        assertTroopersHit(first, 3);
        assertTroopersHit(second, 3);
    }

    @Test
    @DisplayName("A platoon on another board is not hit by an attack on this board's building")
    void platoonOnAnotherBoardIsNotHit() {
        ConvInfantry onThisBoard = platoonInside();
        ConvInfantry onAnotherBoard = platoonInside();
        onAnotherBoard.setBoardId(board.getBoardId() + 1);

        attack(10, WeaponType.WEAPON_PULSE);

        assertTroopersHit(onThisBoard, 3);
        verify(gameManager, never()).damageEntity(eq(onAnotherBoard), any(HitData.class), anyInt());
    }

    @Test
    @DisplayName("A gun emplacement halves the attack before the share, rounding down (TO:AR p. 124)")
    void gunEmplacementScalesFirst() {
        BuildingEntity emplacement = new BuildingEntity(BuildingType.MEDIUM, IBuilding.GUN_EMPLACEMENT);

        assertEquals(2, emplacement.damageReachingInfantryInside(6),
              "6 halves to 3, and half of 3 rounds up to 2; halving the 3-point share instead would give 1");
        assertEquals(5, building.damageReachingInfantryInside(10));
    }
}
