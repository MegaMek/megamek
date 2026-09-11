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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.*;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Actual server AFFA processing, TO:AUE p.41, including exact struck floors and complete mobile footprints. */
class MobileStructureFallTest {
    private static final Coords ORIGIN = new Coords(7, 7);
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);
    private static class LocalManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }
    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private TWGameManager manager(int depth) {
        var manager = spy(new LocalManager());
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        manager.getGame().setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        if (depth > 0) for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) {
            manager.getGame().getBoard().getHex(new Coords(x,y)).addTerrain(new Terrain(Terrains.WATER, depth));
        }
        manager.getGame().addPlayer(0, new Player(0, "Fall review"));
        return manager;
    }

    private MobileStructure mobile(TWGameManager manager, int id, int levels, int elevation,
          EntityMovementMode mode, Coords position, List<CubeCoords> cells, int armor) {
        var unit = new MobileStructure(BuildingType.HARDENED, IBuilding.FORTRESS);
        unit.setId(id);
        unit.setOwner(manager.getGame().getPlayer(0));
        unit.setChassis("Fall review " + id);
        unit.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, levels, 150, armor, cells);
        unit.setMovementMode(mode);
        manager.getGame().addEntity(unit);
        unit.setPosition(position);
        unit.setElevation(elevation);
        unit.setDeployed(true);
        unit.updateBuildingEntityHexes(0, manager);
        return unit;
    }

    private BipedMek mek(TWGameManager manager, Coords position, int elevation) {
        var mek = new BipedMek();
        mek.setId(10);
        mek.setOwner(manager.getGame().getPlayer(0));
        mek.setChassis("Falling Mek");
        mek.setWeight(50);
        for (int loc = 0; loc < mek.locations(); loc++) {
            mek.initializeInternal(100, loc);
            mek.initializeArmor(1000, loc);
        }
        manager.getGame().addEntity(mek);
        mek.setDeployed(true);
        mek.setPosition(position);
        mek.setElevation(elevation);
        doReturn(new Vector<>()).when(manager).damageEntity(eq(mek), any(), anyInt());
        return mek;
    }

    @Test void collapsingWallLandsItsOccupantOnTheMobileRoofInsteadOfInsideItsHull() {
        var manager = manager(0);
        var mobile = mobile(manager, 1, 3, 0, EntityMovementMode.VTOL, ORIGIN, List.of(CubeCoords.ZERO), 0);
        var wall = new BuildingEntity(BuildingType.HARDENED, IBuilding.WALL);
        wall.setId(3);
        wall.setOwner(manager.getGame().getPlayer(0));
        wall.configureConstruction(BuildingType.HARDENED, IBuilding.WALL, 6, 150, 0, List.of(CubeCoords.ZERO));
        wall.getDesign().getWallSides().put(CubeCoords.ZERO, 1);
        manager.getGame().addEntity(wall);
        wall.setPosition(ORIGIN);
        wall.setDeployed(true);
        wall.updateBuildingEntityHexes(0, manager);
        var segment = WallRules.at(manager.getGame(), 0, ORIGIN, 0).getFirst();
        var mek = mek(manager, ORIGIN, 6);
        mek.setOccupiedWall(segment.target(null));
        manager.damageWall(segment, 150, false);
        assertEquals(0, segment.cf());
        assertNull(mek.getOccupiedWall());
        assertEquals(3, mek.getElevation());
        assertEquals(144, mobile.getCurrentCF(ORIGIN));
        assertTrue(mek.isProne());
    }

    @Test void failedSinkingDeckFootingFallsOntoAnAdjacentLowerMobileRoof() {
        var manager = manager(20);
        manager.getGame().setRoundCount(1);
        var upper = mobile(manager, 1, 4, -4, EntityMovementMode.SUBMARINE, ORIGIN, List.of(CubeCoords.ZERO), 0);
        var destination = ORIGIN.translated(0);
        var lower = mobile(manager, 2, 4, -5, EntityMovementMode.SUBMARINE, destination, List.of(CubeCoords.ZERO), 0);
        for (Coords adjacent : ORIGIN.allAdjacent()) {
            if (!adjacent.equals(destination)) {
                manager.getGame().getBoard().getHex(adjacent).addTerrain(new Terrain(Terrains.IMPASSABLE, 1));
            }
        }
        var mek = mek(manager, ORIGIN, -2);
        doReturn(1).when(manager).doSkillCheckWhileMoving(eq(mek), eq(-2), eq(ORIGIN), eq(ORIGIN), any(), eq(false), any());
        upper.getNavalState().beginSinking(1, 5);
        upper.getNavalState().descended(1);
        new MobileStructureNavalHandler(manager).endTurn();
        assertEquals(destination, mek.getPosition());
        assertEquals(-3, mek.getElevation(), "the lower roof is encountered before the seabed at -20");
        assertEquals(148, lower.getCurrentCF(destination));
        assertTrue(mek.isProne());
    }

    @Test void fallingMekHitsTheExactElevatedNonPivotHexAndTopFloor() {
        var manager = manager(0);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        var mobile = mobile(manager, 1, 3, 4, EntityMovementMode.VTOL, ORIGIN, List.of(CubeCoords.ZERO, EAST), 0);
        var struck = mobile.relativeToBoard(EAST);
        var mek = mek(manager, struck, 11);
        manager.doEntityFallsInto(mek, 11, struck, struck, mek.getBasePilotingRoll(), true);
        assertEquals(142, mobile.getCurrentCF(struck, 2), "four 5-point AFFA clusters, fortress scale");
        assertEquals(150, mobile.getCurrentCF(struck, 0));
        assertEquals(150, mobile.getCurrentCF(ORIGIN, 2));
        assertEquals(7, mek.getElevation());
        assertTrue(mek.isProne());
        assertEquals(ORIGIN, mobile.getPosition());
        assertFalse(mobile.isDoomed() || mobile.isDestroyed());
    }

    @Test void aSubmergedRoofReceivesTheFallAndSupportsTheSurvivorAtItsActualDepth() {
        var manager = manager(20);
        var mobile = mobile(manager, 1, 4, -4, EntityMovementMode.SUBMARINE, ORIGIN, List.of(CubeCoords.ZERO), 0);
        var mek = mek(manager, ORIGIN, 0);
        assertEquals(-2, BuildingElevation.roof(mobile, ORIGIN));
        manager.doEntityFallsInto(mek, 0, ORIGIN, ORIGIN, mek.getBasePilotingRoll(), true);
        assertEquals(146, mobile.getCurrentCF(ORIGIN));
        assertEquals(-2, mek.getElevation(), "the roof is encountered before the water bed at -20");
        assertEquals(ORIGIN, mobile.getPosition());
    }

    @Test void mobileFallingOntoAnotherDamagesNonPivotContactBeforeDisplacingTheSmallerStructure() {
        var manager = manager(0);
        var targetPosition = ORIGIN.toCube().add(EAST).toOffset();
        var target = mobile(manager, 1, 3, 0, EntityMovementMode.VTOL, targetPosition, List.of(CubeCoords.ZERO), 100000);
        var faller = mobile(manager, 2, 3, 10, EntityMovementMode.VTOL, ORIGIN, List.of(CubeCoords.ZERO, EAST), 0);
        manager.doEntityFallsInto(faller, 10, ORIGIN, ORIGIN, faller.getBasePilotingRoll(), true);
        assertTrue(target.getArmor(target.getPosition()) < 100000, "AFFA reaches the non-pivot contact hex");
        assertEquals(115, faller.getCurrentCF(ORIGIN), "70 standard self damage uses fortress scale once");
        assertEquals(115, faller.getCurrentCF(faller.relativeToBoard(EAST)));
        assertFalse(faller.getCoordsList().contains(target.getPosition()));
        assertEquals(0, faller.getElevation());
        assertFalse(target.isDoomed() || target.isDestroyed());
        assertFalse(faller.isDoomed() || faller.isDestroyed());
    }

    @Test void aMobileBelowTheLandingIntervalIsNotDisplacedByAnUnrelatedFall() {
        var manager = manager(30);
        var deep = mobile(manager, 1, 4, -15, EntityMovementMode.SUBMARINE, ORIGIN, List.of(CubeCoords.ZERO), 0);
        var faller = mobile(manager, 2, 3, 5, EntityMovementMode.VTOL, ORIGIN, List.of(CubeCoords.ZERO), 0);
        new MobileStructureCollisionHandler(manager).fall(faller, ORIGIN, 5, List.of());
        assertEquals(ORIGIN, deep.getPosition());
        assertEquals(-15, deep.getElevation());
        assertEquals(150, deep.getCurrentCF(ORIGIN));
    }

    @Test void forcedSinkingGroundHullUsesItsSavedBaseInsteadOfJumpingBackToTheBed() {
        var manager = manager(30);
        var shallow = mobile(manager, 1, 4, 0, EntityMovementMode.TRACKED, ORIGIN, List.of(CubeCoords.ZERO), 0);
        shallow.getNavalState().getBaseOffsets().put(CubeCoords.ZERO, -3);
        shallow.getNavalState().beginSinking(1, 5);
        shallow.setElevation(-4);
        var destination = ORIGIN.translated(0);
        mobile(manager, 2, 4, -15, EntityMovementMode.SUBMARINE, destination, List.of(CubeCoords.ZERO), 0);
        assertTrue(MobileStructureCollisionHandler.canDisplaceTo(shallow, destination));
    }

    @Test void aRooftopVehicleReceivesTheAccidentalHitBeforeItsSupportingMobile() {
        var manager = manager(0);
        var mobile = mobile(manager, 1, 3, 4, EntityMovementMode.VTOL, ORIGIN, List.of(CubeCoords.ZERO), 0);
        var tank = new Tank();
        tank.setId(11);
        tank.setOwner(mobile.getOwner());
        tank.setWeight(50);
        tank.setPosition(ORIGIN);
        tank.setElevation(7);
        tank.setDeployed(true);
        manager.getGame().addEntity(tank);
        doReturn(new Vector<>()).when(manager).damageEntity(eq(tank), any(), anyInt());
        doReturn(new Vector<>()).when(manager).doEntityDisplacement(eq(tank), any(), any(), any());
        var faller = mek(manager, ORIGIN, 11);
        manager.doEntityFallsInto(faller, 11, ORIGIN, ORIGIN, faller.getBasePilotingRoll(), true);
        verify(manager, atLeastOnce()).damageEntity(eq(tank), any(), eq(5));
        assertEquals(150, mobile.getCurrentCF(ORIGIN));
        assertEquals(7, faller.getElevation());
        assertEquals(ORIGIN, mobile.getPosition());
    }

    @Test void aRigidFallingFootprintOnlyStrikesTheFirstRoofHeightItReaches() {
        var manager = manager(0);
        var high = mobile(manager, 1, 6, 0, EntityMovementMode.VTOL, ORIGIN, List.of(CubeCoords.ZERO), 100000);
        var lowPosition = ORIGIN.toCube().add(EAST).toOffset();
        var low = mobile(manager, 2, 3, 0, EntityMovementMode.VTOL, lowPosition, List.of(CubeCoords.ZERO), 100000);
        var faller = mobile(manager, 3, 3, 10, EntityMovementMode.VTOL, ORIGIN, List.of(CubeCoords.ZERO, EAST), 0);
        var contacts = MobileStructureCollisionHandler.fallContacts(faller, ORIGIN, 10, ORIGIN, 0);
        assertEquals(1, contacts.size());
        assertSame(high, contacts.getFirst().target());
        manager.doEntityFallsInto(faller, 10, ORIGIN, ORIGIN, faller.getBasePilotingRoll(), true);
        assertTrue(high.getArmor(high.getPosition()) < 100000);
        assertEquals(100000, low.getArmor(low.getPosition()), "a lower roof was not part of the simultaneous first impact");
    }

    @Test void aRoofDestroyedByTheAccidentalHitCannotLeaveTheFallerFloatingAboveTheRubble() {
        var manager = manager(0);
        var mobile = mobile(manager, 1, 3, 0, EntityMovementMode.VTOL, ORIGIN, List.of(CubeCoords.ZERO), 0);
        mobile.setCurrentCF(2, ORIGIN);
        var faller = mek(manager, ORIGIN, 7);
        manager.doEntityFallsInto(faller, 7, ORIGIN, ORIGIN, faller.getBasePilotingRoll(), true);
        assertFalse(mobile.hasCFIn(ORIGIN));
        assertEquals(0, faller.getElevation());
        assertTrue(faller.isProne());
        assertNull(manager.getGame().getBoard().getBuildingAt(ORIGIN));
    }
}
