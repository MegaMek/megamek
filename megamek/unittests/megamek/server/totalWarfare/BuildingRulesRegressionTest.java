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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKStructureFile;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.*;
import megamek.common.verifier.TestBuilding;
import megamek.common.verifier.TestXMLOption;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Rules and persistence regressions found during the building release review. */
class BuildingRulesRegressionTest {
    private static final Coords ORIGIN = new Coords(8, 8);
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);

    private static class ReviewGameManager extends TWGameManager {
        @Override
        public void send(Packet packet) { }
        @Override
        public void send(int connectionId, Packet packet) { }
    }

    @BeforeAll
    static void equipment() { EquipmentType.initializeTypes(); }

    private AbstractBuildingEntity structure(boolean mobile, int height, List<CubeCoords> hexes) {
        AbstractBuildingEntity unit = mobile ? new MobileStructure(BuildingType.MEDIUM, IBuilding.FORTRESS)
              : new BuildingEntity(BuildingType.MEDIUM, IBuilding.FORTRESS);
        unit.setChassis("Release Review");
        unit.setModel("Probe");
        unit.setYear(3145);
        unit.setTechLevel(TechConstants.T_IS_ADVANCED);
        if (!mobile) { unit.setEngine(new Engine(0, Engine.NONE, 0)); }
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.FORTRESS, height, 40, 0, hexes);
        return unit;
    }

    private TWGameManager manager(AbstractBuildingEntity unit) {
        TWGameManager manager = spy(new ReviewGameManager());
        doNothing().when(manager).send(any(Packet.class));
        doNothing().when(manager).sendChangedHex(any(Coords.class), any(int.class));
        doNothing().when(manager).entityUpdate(any(int.class));
        doNothing().when(manager).sendChangedBuildings(any());
        Game game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        Player owner = new Player(0, "Review");
        game.addPlayer(0, owner);
        unit.setOwner(owner);
        unit.setId(1);
        game.addEntity(unit);
        unit.setPosition(ORIGIN);
        unit.updateBuildingEntityHexes(0, manager);
        return manager;
    }

    @Test
    void mobileStructureSurvivesBlkRoundTrip() throws Exception {
        var unit = (MobileStructure) structure(true, 3, List.of(CubeCoords.ZERO, EAST));
        unit.setMaximumMP(1.25);
        var loaded = new BLKStructureFile(BLKFile.getBlock(unit)).getEntity();
        assertInstanceOf(MobileStructure.class, loaded, "saving must preserve Mobile Structure identity");
    }

    @Test
    void mobileMinimumFootprintIsEnforced() {
        var unit = structure(true, 1, List.of(CubeCoords.ZERO));
        assertFalse(new TestBuilding(unit, new TestXMLOption(), "").correctEntity(new StringBuffer(), unit.getTechLevel()),
              "TO:AUE requires at least two hexes, even before checking required crew quarters");
    }

    @Test
    void mobileQuarterBudgetPaysFourQuartersForOneClearHex() {
        var unit = (MobileStructure) structure(true, 1, List.of(CubeCoords.ZERO, EAST));
        unit.setMaximumMP(1);
        var manager = manager(unit);
        var path = new MovePath(manager.getGame(), unit);
        path.addStep(MoveStepType.FORWARDS);
        assertEquals(unit.getWalkMP(), path.getMpUsed(), "one clear hex must exhaust a 1-MP structure's budget");
    }

    @Test
    void serverFloorDamageReachesClientBuilding() {
        var clientUnit = structure(false, 3, List.of(CubeCoords.ZERO));
        var manager = manager(clientUnit);
        var serverUnit = structure(false, 3, List.of(CubeCoords.ZERO));
        serverUnit.setId(clientUnit.getId());
        serverUnit.setPosition(ORIGIN);
        serverUnit.getInternalBuilding().enableExpandedCF();
        serverUnit.setCurrentCF(36, ORIGIN, 1);
        manager.getGame().getBoard(0).updateBuilding(serverUnit);
        assertEquals(36, clientUnit.getCurrentCF(ORIGIN, 1), "BLDG_UPDATE must transmit the independently damaged floor");
    }

    @Test
    void losingMostHexesToHalfHeightCollapsesTheWholeBuilding() {
        var unit = structure(false, 4, List.of(CubeCoords.ZERO, EAST, new CubeCoords(0, 1, -1), new CubeCoords(1, -1, 0)));
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        unit.getInternalBuilding().enableExpandedCF();
        var coords = unit.getCoordsList();
        for (Coords hex : coords.subList(0, 3)) {
            unit.setCurrentCF(0, hex, 2);
            unit.setCurrentCF(0, hex, 3);
        }
        manager.applyBuildingDamage();
        assertEquals(0, unit.getCurrentCF(coords.get(3)), "TO:AR: three of four hexes at half height cause total collapse");
    }

    @Test
    void authoredElevatorIsAvailableAfterDeployment() {
        var unit = structure(false, 2, List.of(CubeCoords.ZERO, EAST));
        unit.getDesign().getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 20, Map.of(0, 4, 1, 4)));
        var manager = manager(unit);
        assertNotNull(manager.getGame().getIndustrialElevator(ORIGIN, 0),
              "TO:AR p.135: an authored elevator must be usable when its building is deployed");
    }

    @Test
    void authoredBridgeDeckIsPublishedAsBridgeTerrain() {
        var unit = structure(false, 1, List.of(CubeCoords.ZERO, EAST));
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.BRIDGE, 1, 40, 0,
              List.of(CubeCoords.ZERO, EAST));
        unit.getDesign().getBridgeDecks().put(CubeCoords.ZERO, 3);
        unit.getDesign().getBridgeDecks().put(EAST, 3);
        var manager = manager(unit);
        assertEquals(3, manager.getGame().getBoard(0).getHex(ORIGIN).terrainLevel(Terrains.BRIDGE_ELEV),
              "an authored level-3 bridge must deploy a traversable level-3 deck");
    }

    @Test
    void mobileResultSixWithHighSubrollJamsTheTurretInsteadOfTheGun() throws Exception {
        var unit = (MobileStructure) structure(true, 1, List.of(CubeCoords.ZERO, EAST));
        WeaponMounted gun = new WeaponMounted(unit, new ISLaserMedium());
        gun.setSponsonTurretMounted(true);
        unit.addEquipment(gun, 0, false);
        var manager = manager(unit);
        new BuildingEntityCriticalHandler(manager).applyCriticalResult(unit, ORIGIN, 6, 6);
        assertTrue(unit.isTurretJammed(gun), "TO:AUE p.40: 6 with a 4-6 subroll is a turret jam");
        assertFalse(gun.isJammed());
    }

    @Test
    void artilleryAtGroundLevelOnAHillHitsTheLowestFloor() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        manager.getGame().getBoard(0).getHex(ORIGIN).setLevel(5);
        var killer = new BipedMek();
        killer.setId(2);
        manager.artilleryDamageHex(ORIGIN, 0, ORIGIN, 1, null, killer.getId(), killer, null,
              false, 5, 5, new Vector<>(), false, new Vector<>(), false, new DamageFalloff());
        assertEquals(38, unit.getCurrentCF(ORIGIN, 0),
              "absolute blast elevation 5 on level-5 terrain is relative building floor 0");
        assertEquals(40, unit.getCurrentCF(ORIGIN, 2));
    }

    @Test
    void expandedCastleBrianBlastDamagesEveryAffectedFloor() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.configureConstruction(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN, 3, 40, 0,
              List.of(CubeCoords.ZERO));
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        var killer = new BipedMek();
        killer.setId(2);
        var damagedHexes = new HashSet<megamek.common.board.BoardLocation>();
        for (int level = 0; level < 2; level++) {
            manager.artilleryDamageHex(ORIGIN, 0, ORIGIN, 20, null, killer.getId(), killer, null,
                  false, level, 0, new Vector<>(), false, new Vector<>(), false, new DamageFalloff(), damagedHexes);
        }
        assertTrue(unit.getCurrentCF(ORIGIN, 0) < 40, "the blast damages the first affected floor");
        assertTrue(unit.getCurrentCF(ORIGIN, 1) < 40,
              "Expanded CF tracks each affected Castle Brian floor separately; whole-hex deduplication drops upper hits");
    }
}
