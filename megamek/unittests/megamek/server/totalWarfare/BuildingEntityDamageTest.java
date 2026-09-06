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
import static org.mockito.ArgumentMatchers.any;

import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Weapon damage against an Advanced Building entity (issues #8754 and #7857): hits must reach the building's armour
 * and Construction Factor, and a single attack over the hex's damage threshold must roll on the Advanced Building
 * Critical Hits Table (TO:AR pp. 118-119).
 */
class BuildingEntityDamageTest extends GameBoardTestCase {

    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final int STARTING_CF = 40;
    private static final int STARTING_ARMOR = 5;
    private static final int REPORT_CRITICAL_CHECK = 3800;
    private static final int REPORT_NO_CRITICAL = 3805;
    private static final int REPORT_NO_TURRET = 3826;
    private static final int REPORT_AMMO_NO_EFFECT = 3831;
    private static final int REPORT_EQUIPMENT_NO_EFFECT = 3835;

    static {
        initializeBoard("BUILDING_ENTITY_DAMAGE_BOARD", """
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

        Board board = getBoard("BUILDING_ENTITY_DAMAGE_BOARD");
        game.setBoard(board);

        building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, STARTING_CF, STARTING_ARMOR, BasementType.UNKNOWN,
              false);
        building.setOwner(game.getPlayer(0));
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);
        laser = new WeaponMounted(building, new ISLaserMedium());
        building.addEquipment(laser, 0, false);
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);
    }

    private Vector<Report> hitBuilding(int damage) {
        HitData hit = building.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
        return gameManager.damageEntity(building, hit, damage);
    }

    private static boolean containsReport(Vector<Report> reports, int messageId) {
        return reports.stream().anyMatch(report -> report.messageId == messageId);
    }

    @Test
    void weaponDamageReachesArmorThenConstructionFactor() {
        int damage = 8;
        Vector<Report> reports = hitBuilding(damage);

        int throughArmor = (int) Math.floor(building.getDamageToScale() * (damage - STARTING_ARMOR));
        assertFalse(reports.isEmpty(), "damage against a building entity must produce a report");
        assertEquals(0, building.getArmor(BUILDING_HEX));
        assertEquals(STARTING_CF - throughArmor, building.getCurrentCF(BUILDING_HEX));
    }

    @Test
    void damageStoppedByArmorMakesNoCriticalCheck() {
        Vector<Report> reports = hitBuilding(STARTING_ARMOR);

        assertEquals(STARTING_CF, building.getCurrentCF(BUILDING_HEX));
        assertFalse(containsReport(reports, REPORT_CRITICAL_CHECK),
              "TO:AR p. 118: no critical while armour keeps the CF untouched");
    }

    @Test
    void damageOverThresholdMakesCriticalCheck() {
        // threshold is CF 40 / 10 = 4; 12 damage leaves 7 after armour, which exceeds it
        Vector<Report> reports = hitBuilding(12);

        assertTrue(building.getCurrentCF(BUILDING_HEX) < STARTING_CF);
        assertTrue(containsReport(reports, REPORT_CRITICAL_CHECK),
              "TO:AR p. 118: a single attack over the damage threshold rolls for a critical");
    }

    @Test
    void damageAtOrUnderThresholdMakesNoCriticalCheck() {
        // 9 damage leaves 4 after armour, which equals the threshold and does not exceed it
        Vector<Report> reports = hitBuilding(9);

        assertTrue(building.getCurrentCF(BUILDING_HEX) < STARTING_CF);
        assertFalse(containsReport(reports, REPORT_CRITICAL_CHECK));
    }

    @Test
    void criticalRollOfFiveHasNoEffect() {
        Vector<Report> reports = new BuildingEntityCriticalHandler(gameManager)
              .applyCriticalResult(building, BUILDING_HEX, 5, 1);

        assertTrue(containsReport(reports, REPORT_NO_CRITICAL));
        assertFalse(laser.isHit());
        assertFalse(laser.jammedThisPhase());
    }

    @Test
    void criticalRollOfSixJamsAWeapon() {
        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 6, 1);

        // a jam is recorded for this phase and becomes the weapon's jammed state at the phase change
        assertTrue(laser.jammedThisPhase());
        assertFalse(laser.isHit());
    }

    @Test
    void criticalRollOfSevenStunsGunnersForTheFollowingTurn() {
        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 7, 1);

        assertTrue(building.isStunned());
        building.newRound(2);
        assertTrue(building.isStunned(), "the building takes no actions during the following turn");
        building.newRound(3);
        assertFalse(building.isStunned());
    }

    @Test
    void criticalRollOfEightDestroysAWeapon() {
        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 8, 1);

        assertTrue(laser.isHit());
    }

    @Test
    void criticalRollOfNineKillsTheGunnersOfTheHex() {
        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 9, 1);

        assertTrue(building.hasDeadGunners(laser.getLocation()));
        assertTrue(building.allGunnersDead(), "a single-hex building has lost all of its gunners");
        assertTrue(building.getCrew().isDoomed());
    }

    @Test
    void criticalRollOfTenWithoutATurretHasNoEffect() {
        Vector<Report> reports = new BuildingEntityCriticalHandler(gameManager)
              .applyCriticalResult(building, BUILDING_HEX, 10, 2);

        assertTrue(containsReport(reports, REPORT_NO_TURRET));
        assertFalse(laser.jammedThisPhase());
        assertFalse(building.isTurretLocked(laser));
    }

    @Test
    void criticalRollOfElevenWithoutAmmunitionHasNoEffect() {
        Vector<Report> reports = new BuildingEntityCriticalHandler(gameManager)
              .applyCriticalResult(building, BUILDING_HEX, 11, 1);

        assertTrue(containsReport(reports, REPORT_AMMO_NO_EFFECT));
        assertEquals(STARTING_CF, building.getCurrentCF(BUILDING_HEX));
    }

    @Test
    void criticalRollOfElevenExplodesAmmunitionIntoTheConstructionFactor() throws Exception {
        AmmoType autocannonAmmo = (AmmoType) EquipmentType.get("ISAC5 Ammo");
        AmmoMounted ammo = new AmmoMounted(building, autocannonAmmo);
        building.addEquipment(ammo, 0, false);
        ammo.setShotsLeft(4);
        int expectedExplosion = (int) Math.floor(building.getDamageToScale()
              * 4 * autocannonAmmo.getDamagePerShot() * autocannonAmmo.getRackSize());

        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 11, 1);

        assertTrue(ammo.isHit());
        assertEquals(STARTING_CF - Math.min(STARTING_CF, expectedExplosion), building.getCurrentCF(BUILDING_HEX));
    }

    @Test
    void criticalRollOfTwelveWithoutOtherEquipmentHasNoEffect() {
        Vector<Report> reports = new BuildingEntityCriticalHandler(gameManager)
              .applyCriticalResult(building, BUILDING_HEX, 12, 1);

        assertTrue(containsReport(reports, REPORT_EQUIPMENT_NO_EFFECT));
        assertFalse(laser.isHit());
    }

    @Test
    void buildingEntityIsNeverInsideABuilding() {
        assertFalse(building.isInBuilding(),
              "a building entity treated as an occupant of its own hex absorbs its own damage twice");
    }
}
