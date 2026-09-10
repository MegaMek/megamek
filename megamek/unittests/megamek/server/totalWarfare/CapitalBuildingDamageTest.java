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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import megamek.common.BuildingDamageTracker;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.util.SerializationHelper;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CapitalBuildingDamageTest {
    private static final Coords HEX = new Coords(0, 0);
    private static final String BOARD_DATA = """
          size 4 3
          hex 0101 0 "bldg_elev:4;building:3;bldg_class:4;bldg_cf:40" ""
          hex 0201 0 "" ""
          hex 0301 0 "" ""
          hex 0401 0 "bldg_elev:4;building:3;bldg_class:4;bldg_cf:40" ""
          hex 0102 0 "" ""
          hex 0202 0 "" ""
          hex 0302 0 "" ""
          hex 0402 0 "" ""
          hex 0103 0 "" ""
          hex 0203 0 "" ""
          hex 0303 0 "" ""
          hex 0403 0 "" ""
          end""";

    private TWGameManager manager;
    private Game game;
    private IBuilding building;
    private BipedMek attacker;

    @BeforeEach
    void setup() {
        manager = spy(new TWGameManager());
        doNothing().when(manager).send(any(Packet.class));
        doNothing().when(manager).sendChangedBuildings(any());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).entityUpdate(anyInt());
        doReturn(new Vector<Report>()).when(manager).damageEntity(any(Entity.class), any(HitData.class), anyInt(),
              anyBoolean(), any(DamageType.class), anyBoolean(), anyBoolean(), anyBoolean());
        game = manager.getGame();
        game.initializeRulesManager(OptionsConstants.RULES_TW);
        game.setBoard(BoardLoader.initializeBoard(BOARD_DATA));
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.FIRING);
        building = game.getBoard().getBuildingAt(HEX);
        attacker = mek(1, new Coords(2, 0), 0, 0);
    }

    private BipedMek mek(int id, Coords coords, int elevation, int boardId) {
        BipedMek mek = new BipedMek();
        mek.setId(id);
        mek.setChassis("Test Mek " + id);
        mek.setOwner(game.getPlayer(0));
        mek.setWeight(50);
        mek.setBoardId(boardId);
        game.addEntity(mek);
        mek.setPosition(coords);
        mek.setElevation(elevation);
        return mek;
    }

    private void hit(int damage) {
        manager.damageBuilding(building, damage, HEX, attacker);
    }

    @Test
    void smallWeaponHitsAccumulateBeforeCFRounding() {
        hit(5);
        assertEquals(40, building.getCurrentCF(HEX));
        hit(5);
        assertEquals(39, building.getCurrentCF(HEX));
        hit(10);
        assertEquals(39, building.getCurrentCF(HEX), "20 standard damage total removes one capital CF");
        hit(10);
        assertEquals(38, building.getCurrentCF(HEX), "30/20 rounds up, once for the complete attacker total");
    }

    @Test
    void separateAttackersDoNotShareFractions() {
        BipedMek second = mek(2, new Coords(2, 1), 0, 0);
        hit(5);
        manager.damageBuilding(building, 5, HEX, second);
        assertEquals(40, building.getCurrentCF(HEX));
        hit(5);
        assertEquals(39, building.getCurrentCF(HEX));
    }

    @Test
    void weaponAndPhysicalPhasesHaveSeparateTotals() {
        hit(5);
        game.setPhase(GamePhase.PHYSICAL);
        hit(5);
        assertEquals(40, building.getCurrentCF(HEX));
        hit(5);
        assertEquals(39, building.getCurrentCF(HEX));
    }

    @Test
    void successiveRoundsHaveSeparateTotals() {
        hit(5);
        game.setRoundCount(game.getRoundCount() + 1);
        hit(5);
        assertEquals(40, building.getCurrentCF(HEX));
    }

    @Test
    void differentHexesAndBoardsDoNotShareFractions() {
        Coords otherHex = new Coords(3, 0);
        IBuilding other = game.getBoard().getBuildingAt(otherHex);
        Board secondBoard = BoardLoader.initializeBoard(BOARD_DATA);
        secondBoard.setBoardId(7);
        game.setBoard(7, secondBoard);
        IBuilding onOtherBoard = secondBoard.getBuildingAt(HEX);
        hit(5);
        manager.damageBuilding(other, 5, otherHex, attacker);
        manager.damageBuilding(onOtherBoard, 5, HEX, attacker);
        assertEquals(40, other.getCurrentCF(otherHex));
        assertEquals(40, onOtherBoard.getCurrentCF(HEX));
        hit(5);
        assertEquals(39, building.getCurrentCF(HEX));
    }

    @Test
    void capitalArmorRetainsBothFractionsAndRoundingCredit() {
        building.setArmor(3, HEX);
        hit(2);
        assertEquals(3, building.getArmor(HEX));
        hit(3);
        assertEquals(2, building.getArmor(HEX));
        hit(5);
        assertEquals(2, building.getArmor(HEX), "Two five-point groups remove one capital armor point, not two");
        assertEquals(40, building.getCurrentCF(HEX));
    }

    @Test
    void armorOverflowKeepsItsStandardScaleRemainder() {
        building.setArmor(1, HEX);
        hit(14);
        assertEquals(0, building.getArmor(HEX));
        assertEquals(40, building.getCurrentCF(HEX), "The four points through armor do not round to CF damage");
        hit(6);
        assertEquals(39, building.getCurrentCF(HEX), "Ten points through armor round to one CF");
    }

    @Test
    void armorRoundedAwayEarlyIsStillPaidForByLaterHits() {
        building.setArmor(1, HEX);
        hit(5);
        hit(5);
        assertEquals(0, building.getArmor(HEX));
        assertEquals(40, building.getCurrentCF(HEX));
        hit(10);
        assertEquals(39, building.getCurrentCF(HEX));
    }

    @Test
    void internalAttacksBypassArmor() {
        building.setArmor(10, HEX);
        attacker.setPosition(HEX);
        hit(20);
        assertEquals(10, building.getArmor(HEX));
        assertEquals(39, building.getCurrentCF(HEX));
    }

    @Test
    void exceedingThresholdHitsEveryOccupantInTwoFivePointAreaGroups() {
        BipedMek first = mek(2, HEX, 0, 0);
        BipedMek second = mek(3, HEX, 1, 0);
        BipedMek roof = mek(4, HEX, 4, 0);
        game.setBoard(7, BoardLoader.initializeBoard(BOARD_DATA));
        game.getBoard(7).setBoardId(7);
        BipedMek otherBoard = mek(5, HEX, 0, 7);
        hit(40);
        verify(manager, never()).damageEntity(eq(first), any(), anyInt(), anyBoolean(), any(), anyBoolean(),
              anyBoolean(), anyBoolean());
        hit(41);
        for (BipedMek occupant : List.of(first, second)) {
            verify(manager, times(2)).damageEntity(eq(occupant), any(), eq(5), eq(false), eq(DamageType.NONE),
                  eq(false), eq(true), eq(false));
        }
        for (BipedMek exposed : List.of(roof, otherBoard)) {
            verify(manager, never()).damageEntity(eq(exposed), any(), anyInt(), anyBoolean(), any(), anyBoolean(),
                  anyBoolean(), anyBoolean());
        }
        assertTrue(manager.damageInfantryIn(building, 100, HEX).isEmpty(), "No second infantry damage path");
    }

    @Test
    void intactArmorPreventsOccupantDamage() {
        building.setArmor(10, HEX);
        BipedMek occupant = mek(2, HEX, 0, 0);
        hit(50);
        assertEquals(5, building.getArmor(HEX));
        verify(manager, never()).damageEntity(eq(occupant), any(), anyInt(), anyBoolean(), any(), anyBoolean(),
              anyBoolean(), anyBoolean());
    }

    @Test
    void severalSmallHitsDoNotCombineIntoAnOccupantBreach() {
        BipedMek occupant = mek(2, HEX, 0, 0);
        hit(20);
        hit(20);
        hit(20);
        assertEquals(37, building.getCurrentCF(HEX));
        verify(manager, never()).damageEntity(eq(occupant), any(), anyInt(), anyBoolean(), any(), anyBoolean(),
              anyBoolean(), anyBoolean());
    }

    @Test
    void armorOnlyDamageIsSentToClientsAtPhaseEnd() {
        building.setArmor(3, HEX);
        hit(10);
        manager.applyBuildingDamage();
        verify(manager).sendChangedBuildings(any());
        assertEquals(40, building.getPhaseCF(HEX));
    }

    @Test
    void serializedTrackerPreservesAnAttackersFraction() throws Exception {
        hit(5);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(game.getBuildingDamageTracker());
        }
        BuildingDamageTracker restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (BuildingDamageTracker) input.readObject();
        }
        assertEquals(1, restored.resolve(building, HEX, 5, attacker.getId(), false,
              game.getRoundCount(), game.getPhase()).cf());
    }

    @Test
    void standaloneMovementEventsRoundNormally() {
        manager.damageBuilding(building, 10, HEX);
        manager.damageBuilding(building, 10, HEX);
        assertEquals(38, building.getCurrentCF(HEX));
        assertEquals(1, building.scaleDamageToCF(10));
        assertEquals(10, building.getDamageFromScale());
    }

    @Test
    void saveGameXmlPreservesFractionsAndArmorUpdates() {
        building.setArmor(1, HEX);
        hit(14);
        String xml = SerializationHelper.getSaveGameXStream().toXML(game.getBuildingDamageTracker());
        BuildingDamageTracker restored = (BuildingDamageTracker) SerializationHelper.getLoadSaveGameXStream()
              .fromXML(xml);
        assertTrue(restored.hasChanges(building, HEX));
        assertEquals(1, restored.resolve(building, HEX, 6, attacker.getId(), false,
              game.getRoundCount(), game.getPhase()).cf());
    }

    @Test
    void castleBrianRoofSupportsCapitalScaleLoad() {
        BipedMek occupant = mek(2, HEX, 4, 0);
        occupant.setWeight(100);
        assertEquals(400, building.getLoadCapacity(HEX));
        assertFalse(new BuildingCollapseHandler(manager).checkForCollapse(building, game.getPositionMapMulti(), HEX,
              false, new Vector<>()));
    }

    @Test
    void collapseDamageIsConvertedToStandardScale() {
        BipedMek occupant = mek(2, HEX, 0, 0);
        doReturn(new Vector<Report>()).when(manager).damageEntity(any(Entity.class), any(HitData.class), anyInt());
        new BuildingCollapseHandler(manager).collapseBuilding(building, game.getPositionMapMulti(), HEX, new Vector<>());
        ArgumentCaptor<Integer> damage = ArgumentCaptor.forClass(Integer.class);
        verify(manager, times(32)).damageEntity(eq(occupant), any(), damage.capture());
        assertEquals(160, damage.getAllValues().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void chargingCastleBrianInflictsCapitalScaleCollisionDamage() {
        assertEquals(50, ChargeAttackAction.getDamageTakenBy(attacker, building, HEX));
    }

    @Test
    void buildingEntitiesUseTheSameCapitalDamagePath() {
        BuildingEntity tower = new BuildingEntity(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN);
        tower.configureConstruction(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN, 4, 40, 1, List.of(CubeCoords.ZERO));
        tower.setId(20);
        tower.setOwner(game.getPlayer(0));
        tower.setGame(game);
        tower.setPosition(new Coords(1, 1));
        HitData hit = new HitData(0);
        hit.setAttackerId(attacker.getId());
        hit.setCapital(true);
        manager.damageEntity(tower, hit, 3);
        assertEquals(0, tower.getArmor(tower.getPosition()));
        assertEquals(39, tower.getCurrentCF(tower.getPosition()));
    }

    @Test
    void ordinaryFortressDamageStillUsesTheExistingArmorAndHalfDamageRules() {
        Board board = BoardLoader.initializeBoard(BOARD_DATA.replace("bldg_class:4", "bldg_class:2"));
        IBuilding fortress = board.getBuildingAt(HEX);
        fortress.setArmor(5, HEX);
        manager.damageBuilding(fortress, 15, HEX, attacker);
        assertEquals(0, fortress.getArmor(HEX));
        assertEquals(35, fortress.getCurrentCF(HEX));
    }

    @Test
    void artilleryHasNoBuildingBonusAndDamagesAHexOncePerExplosion() {
        DamageFalloff falloff = new DamageFalloff();
        falloff.radius = 1;
        Set<BoardLocation> damaged = new HashSet<>();
        for (int level = 0; level < 3; level++) {
            manager.artilleryDamageHex(HEX, 0, HEX, 20, null, attacker.getId(), attacker, null, false,
                  level, 0, new Vector<>(), false, new Vector<>(), false, falloff, damaged);
        }
        assertEquals(39, building.getCurrentCF(HEX));
        assertEquals(1, damaged.size());
    }

    @Test
    void fuelAirReductionAlsoAppliesToUnarmoredCastlesBrian() {
        DamageFalloff falloff = new DamageFalloff();
        falloff.radius = 1;
        manager.artilleryDamageHex(HEX, 0, HEX, 40, BombType.createBombByType(BombTypeEnum.FAE_LARGE),
              attacker.getId(), attacker, null, false, 0, 0, new Vector<>(), false, new Vector<>(), false, falloff);
        assertEquals(39, building.getCurrentCF(HEX));
    }

    @Test
    void artilleryBlastAppliesThresholdDamageOnceAcrossElevations() {
        BipedMek groundFloor = mek(2, HEX, 0, 0);
        BipedMek upperFloor = mek(3, HEX, 1, 0);
        DamageFalloff falloff = new DamageFalloff();
        falloff.damage = 60;
        falloff.falloff = 30;
        falloff.radius = 1;
        manager.artilleryDamageArea(HEX, 0, null, attacker.getId(), attacker, falloff, false, 0,
              new Vector<>(), false);
        assertEquals(37, building.getCurrentCF(HEX));
        for (BipedMek occupant : new BipedMek[] { groundFloor, upperFloor }) {
            verify(manager, times(2)).damageEntity(eq(occupant), any(), eq(5), eq(false), eq(DamageType.NONE),
                  eq(false), eq(true), eq(false));
        }
    }

    @Test
    void gamemasterCanFlattenCapitalArmorAndCF() {
        building.setArmor(10, HEX);
        assertNull(manager.buildingEditHandler().applyEdit(HEX,
              new BuildingEditHandler.BuildingEdit(0, null, null, null), "Referee"));
        assertNull(game.getBoard().getBuildingAt(HEX));
    }
}
