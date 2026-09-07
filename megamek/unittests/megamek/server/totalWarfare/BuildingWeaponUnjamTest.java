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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.actions.RepairWeaponMalfunctionAction;
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
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The gunners of an Advanced Building can spend the turn clearing a weapon jammed by a Weapon Malfunction critical
 * hit (TO:AR p. 119), the way a vehicle crew does (TW p. 195). The server honours the action only while the gunners
 * are able to act.
 */
class BuildingWeaponUnjamTest extends GameBoardTestCase {

    private static final Coords BUILDING_HEX = new Coords(5, 5);

    static {
        initializeBoard("BUILDING_UNJAM_BOARD", """
              size 16 17
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private BuildingEntity building;
    private WeaponMounted laser;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        Player player = new Player(0, "Test");
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());
        game = gameManager.getGame();
        game.addPlayer(0, player);

        Board board = getBoard("BUILDING_UNJAM_BOARD");
        game.setBoard(board);

        building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.GUN_EMPLACEMENT);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 0, BasementType.UNKNOWN, false);
        building.setOwner(game.getPlayer(0));
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(game.getNextEntityId());
        laser = new WeaponMounted(building, new ISLaserMedium());
        building.addEquipment(laser, 0, false);
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);
        jam(laser);
    }

    /** A jam, like its repair, takes effect at the next phase change. */
    private static void jam(WeaponMounted weapon) {
        weapon.setJammed(true);
        weapon.newPhase(GamePhase.FIRING);
    }

    private void orderRepair() {
        game.addAction(new RepairWeaponMalfunctionAction(building.getId(), building.getEquipmentNum(laser)));
        gameManager.resolveAllButWeaponAttacks();
    }

    @Test
    void jammedBuildingWeaponCanBeCleared() {
        assertTrue(building.canUnjamWeapon(), "the firing display must offer Clear Weapon Jam");

        orderRepair();

        assertFalse(laser.jammedThisPhase(), "the gunners clear the jam at the phase change");
        assertTrue(laser.isJammed(), "the weapon stays jammed for the rest of this phase");
    }

    @Test
    void stunnedGunnersCannotClearAJam() {
        building.stunGunners();

        assertFalse(building.canUnjamWeapon(), "the firing display must not offer Clear Weapon Jam");
        orderRepair();

        assertTrue(laser.jammedThisPhase(), "the server refuses the action while the gunners are stunned");
    }

    @Test
    void deadGunnersCannotClearAJam() {
        building.killGunnersAt(BUILDING_HEX);

        assertFalse(building.canUnjamWeapon());
        orderRepair();

        assertTrue(laser.jammedThisPhase());
    }

    @Test
    void nothingToClearOffersNothing() {
        laser.resetJam();

        assertFalse(building.canUnjamWeapon());
    }

    @Test
    void mekCannotRepairAWeaponMalfunction() throws Exception {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(game.getPlayer(0));
        WeaponMounted mekLaser = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMediumLaser"),
              Mek.LOC_RIGHT_ARM);
        game.addEntity(mek);
        jam(mekLaser);

        game.addAction(new RepairWeaponMalfunctionAction(mek.getId(), mek.getEquipmentNum(mekLaser)));
        gameManager.resolveAllButWeaponAttacks();

        assertTrue(mekLaser.jammedThisPhase(), "only vehicles and buildings can clear a weapon malfunction");
    }
}
