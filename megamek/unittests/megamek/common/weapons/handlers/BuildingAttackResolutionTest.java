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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.lrm.LRMHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * An attack on a building hex is one attack, however many hits it lands: the building takes its total damage, and the
 * share that reaches the infantry inside (TW p. 172) is worked out on that total and converted by the attacking
 * weapon's row of the damage table for infantry (TW p. 216). Each grouping of a cluster weapon counts separately only
 * for the building's absorption of damage aimed at a non-infantry unit inside (TW p. 171).
 */
class BuildingAttackResolutionTest extends GameBoardTestCase {

    private static final Coords ATTACKER_HEX = new Coords(5, 1);
    private static final Coords BUILDING_HEX = new Coords(5, 5);

    static {
        initializeBoard("BUILDING_ATTACK_BOARD", """
              size 16 17
              hex 0501 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private Board board;
    private BuildingEntity building;
    private BipedMek attacker;
    private Mounted<?> lrm;

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

        board = getBoard("BUILDING_ATTACK_BOARD");
        game.setBoard(board);

        building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 100, 0, BasementType.UNKNOWN, false);
        building.setOwner(game.getPlayer(0));
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(game.getNextEntityId());
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);

        attacker = new BipedMek();
        attacker.setGame(game);
        attacker.setId(game.getNextEntityId());
        attacker.setChassis("Test Mek");
        attacker.setModel("Attacker");
        attacker.setCrew(new Crew(CrewType.SINGLE));
        attacker.setOwner(game.getPlayer(0));
        attacker.setWeight(50.0);
        lrm = attacker.addEquipment(EquipmentType.get("ISLRM20"), Mek.LOC_RIGHT_TORSO);
        game.addEntity(attacker);
        attacker.setDeployed(true);
        attacker.setPosition(ATTACKER_HEX);

        // The damage itself is not under test; only what attack reaches the building and the infantry inside
        doReturn(new Vector<Report>()).when(gameManager)
              .damageBuilding(any(IBuilding.class), anyInt(), any(String.class), any(Coords.class), anyInt(),
                    any(Entity.class), anyBoolean());
        doReturn(new Vector<Report>()).when(gameManager)
              .damageInfantryIn(any(IBuilding.class), anyInt(), any(Coords.class), anyInt());
    }

    /**
     * Adjacent to a building every missile hits (TW p. 171), so a full LRM-20 run through the real handler lands 20
     * damage on the building as one attack, and the infantry inside are dealt their share of all 20 at once.
     */
    @Test
    void fullLrmVolleyIsOneAttack() throws Exception {
        attacker.setPosition(BUILDING_HEX.translated(0));
        attacker.setFacing(3);
        Mounted<?> ammo = attacker.addEquipment(EquipmentType.get("IS Ammo LRM-20"), Mek.LOC_LEFT_TORSO);
        lrm.setLinked(ammo);
        int targetId = HexTarget.locationToId(BoardLocation.of(BUILDING_HEX, board.getBoardId()));
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), Targetable.TYPE_BUILDING, targetId,
              attacker.getEquipmentNum(lrm));
        LRMHandler handler = new LRMHandler(new ToHitData(2, "test"), attack, game, gameManager);

        handler.handle(GamePhase.FIRING, new Vector<>());

        verify(gameManager, times(1)).damageBuilding(eq(building), eq(20), eq(" absorbs "), eq(BUILDING_HEX), eq(0),
              eq(attacker), eq(false));
        verify(gameManager, times(1)).damageInfantryIn(eq(building), eq(20), eq(BUILDING_HEX),
              eq(WeaponType.WEAPON_CLUSTER_MISSILE));
    }

    /** A missed volley at a unit inside a building damages the building instead (TW p. 171), as one attack. */
    @Test
    void missedVolleyAtAUnitInsideIsOneAttackOnTheBuilding() throws Exception {
        BipedMek inside = new BipedMek();
        inside.setGame(game);
        inside.setId(game.getNextEntityId());
        inside.setChassis("Test Mek");
        inside.setModel("Inside");
        inside.setCrew(new Crew(CrewType.SINGLE));
        inside.setOwner(game.getPlayer(0));
        inside.setWeight(20.0);
        game.addEntity(inside);
        inside.setDeployed(true);
        inside.setPosition(BUILDING_HEX);
        inside.setElevation(0);
        attacker.setPosition(BUILDING_HEX.translated(0));
        attacker.setFacing(3);
        Mounted<?> ammo = attacker.addEquipment(EquipmentType.get("IS Ammo LRM-20"), Mek.LOC_LEFT_TORSO);
        lrm.setLinked(ammo);
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), inside.getId(),
              attacker.getEquipmentNum(lrm));
        LRMHandler handler = new LRMHandler(new ToHitData(13, "always misses"), attack, game, gameManager);

        handler.handle(GamePhase.FIRING, new Vector<>());

        verify(gameManager, times(1)).damageBuilding(eq(building), anyInt(), eq(" absorbs "), eq(BUILDING_HEX), eq(0),
              eq(attacker), eq(false));
    }

    /** Damage from conventional infantry weapons reaches infantry inside unreduced (TW p. 216). */
    @Test
    void infantryWeaponDamageIsInfantryOrigin() throws Exception {
        int targetId = HexTarget.locationToId(BoardLocation.of(BUILDING_HEX, board.getBoardId()));
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), Targetable.TYPE_BUILDING, targetId,
              attacker.getEquipmentNum(lrm));
        WeaponHandler handler = new WeaponHandler(new ToHitData(), attack, game, gameManager);

        assertEquals(WeaponType.WEAPON_CLUSTER_MISSILE, handler.infantryDamageClass());

        handler.weaponType = (WeaponType) EquipmentType.get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        assertEquals(WeaponType.WEAPON_INFANTRY_ORIGIN, handler.infantryDamageClass());
    }
}
