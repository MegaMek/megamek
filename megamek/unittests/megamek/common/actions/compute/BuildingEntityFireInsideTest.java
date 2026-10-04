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
package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.File;

import megamek.client.ui.Messages;
import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.AimingMode;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Issue #8132: an Advanced Building must not fire on a unit inside one of its own hexes, while a unit on its roof
 * remains a legal target (TO:AR p. 132, turreted weapons are rooftop equipment).
 */
class BuildingEntityFireInsideTest extends GameBoardTestCase {

    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final int BUILDING_HEIGHT = 2;

    static {
        initializeBoard("BUILDING_FIRE_INSIDE_BOARD", """
              size 16 17
              hex 0505 0 "" ""
              hex 0506 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private BuildingEntity building;
    private WeaponMounted laser;
    private Entity squad;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        Player defender = new Player(0, "Defender");
        Player attacker = new Player(1, "Attacker");
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());
        game = gameManager.getGame();
        game.addPlayer(0, defender);
        game.addPlayer(1, attacker);

        Board board = getBoard("BUILDING_FIRE_INSIDE_BOARD");
        game.setBoard(board);

        building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.getInternalBuilding().setBuildingHeight(BUILDING_HEIGHT);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 0, BasementType.UNKNOWN, false);
        building.setOwner(defender);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(1);
        laser = new WeaponMounted(building, new ISLaserMedium());
        laser.setMekTurretMounted(true);
        building.addEquipment(laser, 0, false);
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);

        squad = new MekFileParser(new File("testresources/megamek/common/units/Afreet Med BA (HH) (Sqd4).blk"))
              .getEntity();
        squad.setGame(game);
        squad.setOwner(attacker);
        squad.setId(2);
        squad.setDeployed(true);
        game.addEntity(squad);
        squad.setPosition(BUILDING_HEX);
    }

    private ToHitData buildingFiresAtSquad() {
        return ComputeToHit.toHitCalc(game, building.getId(), squad, building.getEquipmentNum(laser),
              Entity.LOC_NONE, AimingMode.NONE, false, false, null, null, false, false, null, false,
              WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);
    }

    private static boolean saysInsideOwnBuilding(ToHitData toHit) {
        String reason = Messages.getString("WeaponAttackAction.TargetInsideOwnBuilding");
        return toHit.getModifiers().stream().anyMatch(modifier -> reason.equals(modifier.description()));
    }

    @Test
    void cannotFireOnASquadInsideItsOwnHex() {
        squad.setElevation(0);

        ToHitData toHit = buildingFiresAtSquad();

        assertTrue(squad.isInBuilding(), "test setup: the squad stands inside the building");
        assertTrue(toHit.cannotSucceed());
        assertTrue(saysInsideOwnBuilding(toHit));
    }

    @Test
    void aSquadOnAnotherBoardAtTheSameCoordinatesIsNotInsideThisBuilding() {
        squad.setElevation(0);
        squad.setBoardId(building.getBoardId() + 1);

        assertFalse(building.isInsideThisBuilding(squad));
    }

    @Test
    void canFireOnASquadStandingOnItsRoof() {
        squad.setElevation(BUILDING_HEIGHT);

        ToHitData toHit = buildingFiresAtSquad();

        assertFalse(squad.isInBuilding(), "test setup: the squad stands on the roof");
        assertFalse(saysInsideOwnBuilding(toHit));
    }
}
