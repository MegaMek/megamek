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
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.BipedMek;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.utils.BoardLoader;
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
    private static final int REPORT_CREW_ALREADY_DEAD = 3811;
    private static final int REPORT_NO_WEAPON_LEFT = 3841;
    private static final int REPORT_NO_WEAPON_TO_JAM = 3846;
    private static final int REPORT_NO_TURRET = 3826;
    private static final int REPORT_AMMO_NO_EFFECT = 3831;
    private static final int REPORT_EQUIPMENT_NO_EFFECT = 3835;

    private static final String BOARD_DATA = """
              size 16 17
              hex 0505 0 "" ""
              hex 0506 0 "" ""
              end""";

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

        Board board = BoardLoader.initializeBoard(BOARD_DATA);
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
    void criticalRollOfSevenOnDeadGunnersHasNoEffect() {
        building.killGunnersAt(BUILDING_HEX);

        Vector<Report> reports = new BuildingEntityCriticalHandler(gameManager)
              .applyCriticalResult(building, BUILDING_HEX, 7, 1);

        assertTrue(containsReport(reports, REPORT_CREW_ALREADY_DEAD));
        assertFalse(building.isStunned());
    }

    @Test
    void criticalRollOfEightDestroysAWeapon() {
        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 8, 1);

        assertTrue(laser.isHit());
    }

    @Test
    void criticalResultsSkipAWeaponDestroyedWithoutTheHitFlag() {
        // a weapon in a collapsed hex is destroyed directly, never marked hit
        laser.setDestroyed(true);

        Vector<Report> destroyedReports = new BuildingEntityCriticalHandler(gameManager)
              .applyCriticalResult(building, BUILDING_HEX, 8, 1);
        Vector<Report> malfunctionReports = new BuildingEntityCriticalHandler(gameManager)
              .applyCriticalResult(building, BUILDING_HEX, 6, 1);

        assertTrue(containsReport(destroyedReports, REPORT_NO_WEAPON_LEFT));
        assertTrue(containsReport(malfunctionReports, REPORT_NO_WEAPON_TO_JAM));
        assertFalse(laser.isHit());
        assertFalse(laser.jammedThisPhase());
    }

    @Test
    void criticalRollOfNineKillsTheGunnersOfTheHex() {
        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 9, 1);

        assertTrue(building.hasDeadGunners(laser.getLocation()));
        assertTrue(building.allGunnersDead(), "a single-hex building has lost all of its gunners");
        assertFalse(building.getCrew().isDoomed(), "gunner casualties do not kill equipment operators or occupants");
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
    void criticalRollOfTenWithLowTurretRollJamsTheTurret() {
        laser.setMekTurretMounted(true);

        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 10, 3);

        assertTrue(building.isTurretJammed(laser));
        assertFalse(laser.jammedThisPhase(), "a frozen turret can still fire in its current facing until repair");
        assertFalse(building.isTurretLocked(laser));
    }

    @Test
    void criticalRollOfTenWithHighTurretRollLocksTheTurretToTheForwardArc() {
        laser.setMekTurretMounted(true);
        int allRoundArc = building.getWeaponArc(building.getEquipmentNum(laser));

        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 10, 4);

        assertTrue(building.isTurretLocked(laser));
        assertFalse(laser.jammedThisPhase());
        assertEquals(0, allRoundArc, "a working turret fires in every direction");
        assertEquals(1, building.getWeaponArc(building.getEquipmentNum(laser)),
              "a locked turret fires only into the forward arc");
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

    @Test
    void castleBrianUsesTheSharedDamagePathAndKeepsAttackerRounding() {
        building.configureConstruction(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN, 1, 40, 0,
              java.util.List.of(CubeCoords.ZERO));
        BipedMek attacker = new BipedMek();
        attacker.setId(1);
        attacker.setOwner(game.getPlayer(0));
        game.addEntity(attacker);
        attacker.setPosition(new Coords(2, 2));
        HitData hit = new HitData(0);
        hit.setAttackerId(attacker.getId());

        gameManager.damageEntity(building, hit, 5);
        assertEquals(40, building.getCurrentCF(BUILDING_HEX));
        gameManager.damageEntity(building, hit, 5);
        assertEquals(39, building.getCurrentCF(BUILDING_HEX));
    }

    @Test
    void castleBrianCriticalThresholdUsesCapitalCF() {
        building.configureConstruction(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN, 1, 40, 0,
              java.util.List.of(CubeCoords.ZERO));
        assertFalse(containsReport(hitBuilding(50), REPORT_CRITICAL_CHECK),
              "the occupant threshold is separate from the advanced-building critical threshold");
        assertTrue(containsReport(hitBuilding(100), REPORT_CRITICAL_CHECK));
    }

    @Test
    void criticalThresholdDoesNotDropWhenPhaseCFIsUpdated() {
        building.setArmor(0, BUILDING_HEX);
        hitBuilding(4);
        building.setPhaseCF(20, BUILDING_HEX);
        assertFalse(containsReport(hitBuilding(3), REPORT_CRITICAL_CHECK));
        assertEquals(4, building.getCriticalDamageThreshold(BUILDING_HEX));
        building.setCurrentCF(20, BUILDING_HEX);
        building.newRound(2);
        assertEquals(2, building.getCriticalDamageThreshold(BUILDING_HEX));
    }

    @Test
    void lockedTurretRetainsItsSelectedFacing() {
        laser.setMekTurretMounted(true);
        laser.setFacing(3);
        building.lockTurretWeapon(laser);
        assertEquals(52, building.getWeaponArc(building.getEquipmentNum(laser)));
    }

    @Test
    void fireFromInsideBypassesBuildingArmor() {
        BipedMek attacker = new BipedMek();
        attacker.setId(1);
        attacker.setOwner(game.getPlayer(0));
        game.addEntity(attacker);
        attacker.setPosition(BUILDING_HEX);
        attacker.setElevation(0);
        HitData hit = new HitData(0);
        hit.setAttackerId(attacker.getId());

        gameManager.damageEntity(building, hit, 4);

        assertEquals(STARTING_ARMOR, building.getArmor(BUILDING_HEX));
        assertEquals(STARTING_CF - 4, building.getCurrentCF(BUILDING_HEX));
    }

    private Coords addSecondHex() {
        CubeCoords second = new CubeCoords(1, 0, -1);
        building.getInternalBuilding().addHex(second, STARTING_CF, 0, BasementType.UNKNOWN, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setPosition(BUILDING_HEX);
        return building.relativeToBoard(second);
    }

    @Test
    void staleHitLocationFallsBackToTheRemainingStandingHex() {
        Coords second = addSecondHex();
        building.setCurrentCF(0, BUILDING_HEX);

        hitBuilding(4);

        assertEquals(STARTING_CF - 4, building.getCurrentCF(second));
        assertEquals(0, building.getCurrentCF(BUILDING_HEX));
    }

    @Test
    void gunnerStunIsConfinedToTheHitHexAndExpires() {
        Coords second = addSecondHex();
        int firstLocation = building.getLocationsAt(BUILDING_HEX).getFirst();
        int secondLocation = building.getLocationsAt(second).getFirst();
        new BuildingEntityCriticalHandler(gameManager).applyCriticalResult(building, BUILDING_HEX, 7, 1);

        assertFalse(building.isGunnersStunned(firstLocation), "the critical affects the following turn");
        assertFalse(building.isGunnersStunned(secondLocation));
        building.newRound(2);
        assertTrue(building.isGunnersStunned(firstLocation));
        building.newRound(3);
        assertFalse(building.isGunnersStunned(firstLocation));
    }

    @Test
    void secondTurretJamLocksEvenAfterRepairAndFurtherHitsHaveNoEffect() {
        laser.setMekTurretMounted(true);
        var handler = new BuildingEntityCriticalHandler(gameManager);
        handler.applyCriticalResult(building, BUILDING_HEX, 10, 1);
        laser.setJammedImmediately(false);
        handler.applyCriticalResult(building, BUILDING_HEX, 10, 1);

        assertTrue(building.isTurretLocked(laser));
        assertFalse(laser.jammedThisPhase());
        handler.applyCriticalResult(building, BUILDING_HEX, 10, 1);
        assertFalse(laser.jammedThisPhase());
    }

    @Test
    void caseReducesAmmoExplosionAndDestroyedAmmoCannotExplodeAgain() throws Exception {
        var ammo = (AmmoMounted) building.addEquipment(EquipmentType.get("ISAC5 Ammo"), 0);
        ammo.setShotsLeft(4);
        building.addEquipment(MiscType.createISCASE(), 0);
        var handler = new BuildingEntityCriticalHandler(gameManager);

        handler.applyCriticalResult(building, BUILDING_HEX, 11, 1);
        assertEquals(STARTING_CF - 2, building.getCurrentCF(BUILDING_HEX));
        assertEquals(STARTING_ARMOR, building.getArmor(BUILDING_HEX));
        handler.applyCriticalResult(building, BUILDING_HEX, 11, 1);
        assertEquals(STARTING_CF - 2, building.getCurrentCF(BUILDING_HEX));
    }

    @Test
    void automatedWeaponsHaveNoGunnersToStunOrKill() {
        building.getDesign().getAutomatedWeapons().add(laser);
        var handler = new BuildingEntityCriticalHandler(gameManager);
        handler.applyCriticalResult(building, BUILDING_HEX, 7, 1);
        handler.applyCriticalResult(building, BUILDING_HEX, 9, 1);
        assertEquals(0, building.getStunnedTurns());
        assertFalse(building.hasDeadGunners(laser.getLocation()));
        assertFalse(building.getCrew().isDoomed());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void perFloorDamageRequiresTheGameOptionForStaticAndMobileStructures(boolean mobile, boolean expandedCF) {
        AbstractBuildingEntity target = mobile ? new MobileStructure(BuildingType.MEDIUM, 1)
              : new BuildingEntity(BuildingType.MEDIUM, 1);
        target.getInternalBuilding().setBuildingHeight(3);
        target.getInternalBuilding().addHex(CubeCoords.ZERO, STARTING_CF, STARTING_ARMOR, BasementType.UNKNOWN, false);
        target.refreshLocations();
        target.refreshAdditionalLocations();
        target.setPosition(BUILDING_HEX);
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(expandedCF);

        gameManager.damageBuilding(target, 7, " takes ", BUILDING_HEX, 1, null, false);

        assertEquals(expandedCF, target.usesExpandedCF());
        assertEquals(STARTING_CF - 2, target.getCurrentCF(BUILDING_HEX, 1));
        assertEquals(0, target.getArmor(BUILDING_HEX, 1));
        assertEquals(expandedCF ? STARTING_CF : STARTING_CF - 2, target.getCurrentCF(BUILDING_HEX, 0));
        assertEquals(expandedCF ? STARTING_ARMOR : 0, target.getArmor(BUILDING_HEX, 2));
    }

    @Test
    void expandedCFHitsOnlyTheSelectedFloorAndSurvivesPhaseResolution() {
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().setHeight(3, CubeCoords.ZERO);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.updateBuildingEntityHexes(building.getBoardId(), gameManager);
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        gameManager.damageBuilding(building, 9, " takes ", BUILDING_HEX, 1, null, false);
        assertEquals(STARTING_CF - 4, building.getCurrentCF(BUILDING_HEX, 1));
        assertEquals(0, building.getArmor(BUILDING_HEX, 1));
        assertEquals(STARTING_CF, building.getCurrentCF(BUILDING_HEX, 0));
        assertEquals(STARTING_ARMOR, building.getArmor(BUILDING_HEX, 2));
        gameManager.applyBuildingDamage();
        assertEquals(STARTING_CF - 4, building.getInternal(1));
        assertEquals(STARTING_CF, building.getInternal(0));
        assertEquals(3, building.getHeight(BUILDING_HEX));
    }

    @Test
    void expandedCriticalsCannotHitEquipmentOnAnotherFloor() {
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().setHeight(3, CubeCoords.ZERO);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        try (var dice = Mockito.mockStatic(megamek.common.compute.Compute.class, Mockito.CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(8);
            dice.when(megamek.common.compute.Compute::d6).thenReturn(1);
            gameManager.damageBuilding(building, 10, " takes ", BUILDING_HEX, 1, null, false);
            assertFalse(laser.isHit(), "the weapon is on the lowest floor, not the damaged floor");
            gameManager.damageBuilding(building, 10, " takes ", BUILDING_HEX, 0, null, false);
            assertTrue(laser.isHit());
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({ "5, false", "6, true", "7, true", "8, true", "9, false" })
    void aimedBuildingShotsUseTheImmobileTargetRoll(int roll, boolean hitsAimedLocation) {
        try (var dice = Mockito.mockStatic(megamek.common.compute.Compute.class, Mockito.CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(roll);
            HitData hit = building.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT, 0,
                  megamek.common.enums.AimingMode.IMMOBILE, 0);
            assertTrue(hit.isAimedShotAttempt());
            assertEquals(hitsAimedLocation, hit.hitAimedLocation());
            assertEquals(0, hit.getLocation());
        }
    }

    @Test
    void aimedHitBonusDoesNotBypassArmorOrDamageThreshold() {
        HitData hit = new HitData(0, false, true);
        try (var dice = Mockito.mockStatic(megamek.common.compute.Compute.class, Mockito.CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(5);
            dice.when(megamek.common.compute.Compute::d6).thenReturn(1);
            var reports = gameManager.damageEntity(building, hit, 5);
            assertFalse(containsReport(reports, REPORT_CRITICAL_CHECK));
            reports = gameManager.damageEntity(building, hit, 4);
            assertFalse(containsReport(reports, REPORT_CRITICAL_CHECK));
            reports = gameManager.damageEntity(building, hit, 5);
            assertTrue(containsReport(reports, REPORT_CRITICAL_CHECK));
            assertEquals(2, building.getStunnedTurns(), "5 + 2 from the aimed shot is a gunner stun");
        }
    }
}
