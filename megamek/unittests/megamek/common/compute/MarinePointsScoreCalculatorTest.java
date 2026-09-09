/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.IBuilding;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Marine Points Tables (TO:AR p. 170) and the Building Modifiers Table (TO:AR p. 171), checked against the
 * printed values and the book's own worked examples.
 */
class MarinePointsScoreCalculatorTest {

    private static final double TOLERANCE = 1e-9;
    private static final int PLATOON = 28;
    private static final int HEX_RADIUS_FOR_61_HEXES = 4;

    private Game game;
    private Player player;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        player = new Player(0, "Test");
        game.addPlayer(0, player);
    }

    private ConvInfantry platoon(int troopers) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(player);
        infantry.setGame(game);
        infantry.setSquadSize(troopers);
        infantry.setSquadCount(1);
        infantry.initializeInternal(troopers, ConvInfantry.LOC_INFANTRY);
        return infantry;
    }

    private BattleArmor squad(int troopers, int weightClass, boolean clan, int armorPerTrooper) {
        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setOwner(player);
        battleArmor.setGame(game);
        battleArmor.setChassisType(BattleArmor.CHASSIS_TYPE_BIPED);
        battleArmor.setSquadSize(troopers);
        battleArmor.setWeightClass(weightClass);
        battleArmor.setTechLevel(clan ? TechConstants.T_CLAN_TW : TechConstants.T_IS_TW_NON_BOX);
        battleArmor.autoSetInternal();
        for (int trooper = 1; trooper <= troopers; trooper++) {
            battleArmor.initializeArmor(armorPerTrooper, trooper);
        }
        return battleArmor;
    }

    private static void mount(BattleArmor battleArmor, String internalName) throws Exception {
        battleArmor.addEquipment(EquipmentType.get(internalName), BattleArmor.LOC_SQUAD);
    }

    /** A building whose footprint is a filled hexagon of radius 4, which is 61 hexes, at a uniform height. */
    private static BuildingEntity largeBuilding(int buildingClass, int levels) {
        BuildingEntity building = new BuildingEntity(BuildingType.HEAVY, buildingClass);
        building.getInternalBuilding().setBuildingHeight(levels);
        for (int q = -HEX_RADIUS_FOR_61_HEXES; q <= HEX_RADIUS_FOR_61_HEXES; q++) {
            for (int r = -HEX_RADIUS_FOR_61_HEXES; r <= HEX_RADIUS_FOR_61_HEXES; r++) {
                int s = -q - r;
                if (Math.abs(s) <= HEX_RADIUS_FOR_61_HEXES) {
                    building.getInternalBuilding().addHex(new CubeCoords(q, r, s), 100, 0, BasementType.NONE, false);
                }
            }
        }
        building.refreshLocations();
        building.refreshAdditionalLocations();
        return building;
    }

    private static BuildingEntity smallBuilding(int buildingClass, int levels) {
        BuildingEntity building = new BuildingEntity(BuildingType.HEAVY, buildingClass);
        building.getInternalBuilding().setBuildingHeight(levels);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 100, 0, BasementType.NONE, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        return building;
    }

    @Test
    void nonMarineConventionalInfantryAreWorthThreeQuartersEach() {
        assertEquals(21.0, MarinePointsScoreCalculator.calculateScore(platoon(PLATOON)), TOLERANCE);
    }

    @Test
    void marinesAreWorthOneEach() {
        ConvInfantry marines = platoon(PLATOON);
        marines.setSpecializations(ConvInfantry.MARINES);

        assertEquals(28.0, MarinePointsScoreCalculator.calculateScore(marines), TOLERANCE);
    }

    @Test
    void armoredConventionalInfantryGainHalfAPointEach() {
        ConvInfantry armored = platoon(PLATOON);
        armored.setCustomArmorDamageDivisor(2.0);

        assertEquals(35.0, MarinePointsScoreCalculator.calculateScore(armored), TOLERANCE);
    }

    @Test
    void onlySurvivingTroopersCount() {
        ConvInfantry platoon = platoon(PLATOON);
        platoon.setInternal(14, ConvInfantry.LOC_INFANTRY);

        assertEquals(10.5, MarinePointsScoreCalculator.calculateScore(platoon), TOLERANCE);
        assertEquals(11, MarinePointsScoreCalculator.calculateMPS(platoon), "whole-number scores round up");
    }

    @Test
    void wipedOutPlatoonScoresNothing() {
        ConvInfantry platoon = platoon(PLATOON);
        platoon.setInternal(0, ConvInfantry.LOC_INFANTRY);

        assertEquals(0.0, MarinePointsScoreCalculator.calculateScore(platoon), TOLERANCE);
    }

    @Test
    void nothingScoresNothing() {
        assertEquals(0.0, MarinePointsScoreCalculator.calculateScore(null), TOLERANCE);
    }

    /**
     * Five Elementals, medium weight class, ten armor each, each mounting a flamer. As in the book's Salamander
     * example (TO:AR p. 171), a flamer counts both as a burst-fire weapon and as a flame weapon.
     */
    @Test
    void elementalPointWithFlamersScoresSixty() throws Exception {
        BattleArmor point = squad(5, EntityWeightClass.WEIGHT_MEDIUM, true, 10);
        mount(point, "CLBAFlamer");

        // 5 troopers x (2 base + 2 medium + 2 burst fire + 1 flame) = 35, plus 50 armor x 0.5 = 25
        assertEquals(60.0, MarinePointsScoreCalculator.calculateScore(point), TOLERANCE);
    }

    /** The audit example: an Inner Sphere assault squad, four troopers, seventeen armor each, no burst weapons. */
    @Test
    void innerSphereAssaultSquadScoresFiftyFour() {
        BattleArmor squad = squad(4, EntityWeightClass.WEIGHT_ASSAULT, false, 17);

        // 4 troopers x (1 base + 4 assault) = 20, plus 68 armor x 0.5 = 34
        assertEquals(54.0, MarinePointsScoreCalculator.calculateScore(squad), TOLERANCE);
    }

    @Test
    void pairedVibroClawsAddThreePerTrooper() throws Exception {
        BattleArmor squad = squad(4, EntityWeightClass.WEIGHT_LIGHT, false, 0);
        mount(squad, "BABattleClawVibro");
        mount(squad, "BABattleClawVibro");

        // 4 troopers x (1 base + 2 light + 3 paired vibro-claws)
        assertEquals(24.0, MarinePointsScoreCalculator.calculateScore(squad), TOLERANCE);
    }

    @Test
    void pairedPlainClawsAddTwoAndHeavyClawsAnotherHalf() throws Exception {
        BattleArmor squad = squad(4, EntityWeightClass.WEIGHT_LIGHT, false, 0);
        mount(squad, "BAHeavyBattleClaw");
        mount(squad, "BAHeavyBattleClaw");

        // 4 troopers x (1 base + 2 light + 2 paired claws + 0.5 heavy)
        assertEquals(22.0, MarinePointsScoreCalculator.calculateScore(squad), TOLERANCE);
    }

    @Test
    void aSingleClawIsNotAPair() throws Exception {
        BattleArmor squad = squad(4, EntityWeightClass.WEIGHT_LIGHT, false, 0);
        mount(squad, "BABattleClaw");

        assertEquals(12.0, MarinePointsScoreCalculator.calculateScore(squad), TOLERANCE);
    }

    @Test
    void torchDrillAndAntiPersonnelMountEachAddTheirFraction() throws Exception {
        BattleArmor squad = squad(4, EntityWeightClass.WEIGHT_ULTRA_LIGHT, false, 0);
        mount(squad, "BACuttingTorch");
        mount(squad, "BAIndustrialDrill");
        mount(squad, "BAArmoredGlove");

        // 4 troopers x (1 base + 1 PA(L) + 0.5 torch + 0.5 drill + 0.25 AP mount)
        assertEquals(13.0, MarinePointsScoreCalculator.calculateScore(squad), TOLERANCE);
    }

    @Test
    void burstFireWeaponAddsTwoPerTrooperOnce() throws Exception {
        BattleArmor squad = squad(4, EntityWeightClass.WEIGHT_LIGHT, false, 0);
        mount(squad, "ISBAHeavyMachineGun");
        mount(squad, "ISBAHeavyMachineGun");

        // 4 troopers x (1 base + 2 light + 2 burst fire), the second machine gun adds nothing
        assertEquals(20.0, MarinePointsScoreCalculator.calculateScore(squad), TOLERANCE);
    }

    @Test
    void deadTroopersAndTheirArmorDropOut() {
        BattleArmor squad = squad(4, EntityWeightClass.WEIGHT_ASSAULT, false, 17);
        squad.setInternal(0, 4);

        // 3 troopers x (1 base + 4 assault) = 15, plus 51 armor x 0.5 = 25.5
        assertEquals(40.5, MarinePointsScoreCalculator.calculateScore(squad), TOLERANCE);
    }

    @Test
    void hangarGivesNoBuildingModifier() {
        assertEquals(1.0, MarinePointsScoreCalculator.buildingModifier(largeBuilding(IBuilding.HANGAR, 12)),
              TOLERANCE);
    }

    @Test
    void standardBuildingGainsATenthPerSixLevelsBeyondTheFirst() {
        // 13 levels: 12 beyond the first, two full steps of six
        assertEquals(1.2, MarinePointsScoreCalculator.buildingModifier(largeBuilding(IBuilding.STANDARD, 13)),
              TOLERANCE);
    }

    @Test
    void fortressGainsATenthPerThreeLevelsBeyondTheFirst() {
        // 10 levels: 9 beyond the first, three steps of three
        assertEquals(1.3, MarinePointsScoreCalculator.buildingModifier(largeBuilding(IBuilding.FORTRESS, 10)),
              TOLERANCE);
    }

    @Test
    void smallFootprintGetsNoModifier() {
        assertEquals(1.0, MarinePointsScoreCalculator.buildingModifier(smallBuilding(IBuilding.FORTRESS, 10)),
              TOLERANCE);
    }

    @Test
    void onlyLevelsMadeOfSixtyHexesCount() {
        BuildingEntity building = largeBuilding(IBuilding.FORTRESS, 10);
        // Taper the tower: only two hexes rise above the fourth level.
        int tallHexes = 0;
        for (CubeCoords coords : building.getInternalBuilding().getCoordsList()) {
            if (tallHexes < 2) {
                tallHexes++;
                continue;
            }
            building.getInternalBuilding().setHeight(4, coords);
        }

        // 4 qualifying levels: 3 beyond the first, one step of three
        assertEquals(1.1, MarinePointsScoreCalculator.buildingModifier(building), TOLERANCE);
    }

    @Test
    void defenderScoreIsMultipliedAndAttackerScoreIsNot() {
        ConvInfantry marines = platoon(PLATOON);
        marines.setSpecializations(ConvInfantry.MARINES);
        BuildingEntity fortress = largeBuilding(IBuilding.FORTRESS, 10);

        assertEquals(36.4, MarinePointsScoreCalculator.calculateScore(marines, fortress), TOLERANCE);
        assertEquals(28.0, MarinePointsScoreCalculator.calculateScore(marines, null), TOLERANCE);
    }
}
