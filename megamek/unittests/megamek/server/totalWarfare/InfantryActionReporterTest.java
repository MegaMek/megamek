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

import java.util.List;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.MarinePointsBreakdown;
import megamek.common.compute.MarinePointsScoreCalculator;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityWeightClass;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The lines that show a Marine Points Score's working and a loss's conversion back to people.
 */
@DisplayName("Infantry action report: the working")
class InfantryActionReporterTest {

    private static final Coords BUILDING_HEX = new Coords(5, 5);

    private Game game;
    private TWGameManager gameManager;
    private Player attackingPlayer;
    private Player defendingPlayer;
    private BuildingEntity building;
    private InfantryActionReporter reporter;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        game = new Game();
        attackingPlayer = new Player(0, "Attacker");
        defendingPlayer = new Player(1, "Defender");
        game.addPlayer(0, attackingPlayer);
        game.addPlayer(1, defendingPlayer);
        game.setBoard(new Board(16, 17));
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        gameManager.setGame(game);
        reporter = new InfantryActionReporter(gameManager);

        building = new BuildingEntity(BuildingType.LIGHT, 3);
        building.setOwner(defendingPlayer);
        building.setGame(game);
        building.setPosition(BUILDING_HEX);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(
              new CubeCoords(BUILDING_HEX.getX(), -BUILDING_HEX.getX() - BUILDING_HEX.getY(), BUILDING_HEX.getY()),
              30, 20, BasementType.NONE, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.addEquipment(new WeaponMounted(building, new ISLaserMedium()), 0, false);
        building.addEquipment(new WeaponMounted(building, new ISLaserMedium()), 0, false);
        building.setId(0);
        game.addEntity(building);
    }

    private ConvInfantry platoon(int troopers, Player owner, int id) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setSquadSize(troopers);
        infantry.setSquadCount(1);
        infantry.initializeInternal(troopers, ConvInfantry.LOC_INFANTRY);
        infantry.setId(id);
        game.addEntity(infantry);
        return infantry;
    }

    private BattleArmor elementalPoint(Player owner, int id) {
        BattleArmor squad = new BattleArmor();
        squad.setOwner(owner);
        squad.setGame(game);
        squad.setChassisType(BattleArmor.CHASSIS_TYPE_BIPED);
        squad.setSquadSize(5);
        squad.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        squad.setTechLevel(TechConstants.T_CLAN_TW);
        squad.autoSetInternal();
        for (int trooper = 1; trooper <= 5; trooper++) {
            squad.initializeArmor(10, trooper);
        }
        squad.setId(id);
        game.addEntity(squad);
        return squad;
    }

    private List<Integer> reportIds() {
        return gameManager.getMainPhaseReport().stream().map(report -> report.messageId).toList();
    }

    @Test
    @DisplayName("Each side gets a header and one working line per unit, in order")
    void sidesAreReportedUnitByUnit() {
        ConvInfantry attackers = platoon(28, attackingPlayer, 1);
        BattleArmor defendingSquad = elementalPoint(defendingPlayer, 2);

        reporter.reportSides(List.of(attackers.getId()), List.of(defendingSquad.getId(), building.getId()),
              building);

        assertEquals(List.of(InfantryActionReporter.ATTACKERS_HEADER, InfantryActionReporter.CONVENTIONAL_INFANTRY_SCORE,
                    InfantryActionReporter.DEFENDERS_HEADER, InfantryActionReporter.BATTLE_ARMOR_SCORE,
                    InfantryActionReporter.CREW_SCORE),
              reportIds(), "a one-level building has no modifier line");
    }

    @Test
    @DisplayName("A unit with nobody left is not listed, so the lines add up to the total")
    void unitsOutOfTheFightAreNotListed() {
        ConvInfantry attackers = platoon(28, attackingPlayer, 1);
        ConvInfantry fallen = platoon(28, defendingPlayer, 2);
        fallen.setDestroyed(true);

        reporter.reportSides(List.of(attackers.getId()), List.of(fallen.getId(), building.getId()), building);

        assertEquals(List.of(InfantryActionReporter.ATTACKERS_HEADER, InfantryActionReporter.CONVENTIONAL_INFANTRY_SCORE,
                    InfantryActionReporter.DEFENDERS_HEADER, InfantryActionReporter.CREW_SCORE),
              reportIds(), "the destroyed platoon gets no line");
    }

    @Test
    @DisplayName("The working behind the numbers is the table's: 28 rifles at 0.75, an Elemental point at 4 each plus armor")
    void breakdownsCarryTheTableArithmetic() {
        ConvInfantry rifles = platoon(28, attackingPlayer, 1);
        BattleArmor elementals = elementalPoint(attackingPlayer, 2);

        MarinePointsBreakdown riflesBreakdown = MarinePointsScoreCalculator.breakdown(rifles, null);
        assertEquals(28, riflesBreakdown.headCount());
        assertEquals(0.75, riflesBreakdown.perTrooper());
        assertEquals(21.0, riflesBreakdown.score());

        MarinePointsBreakdown elementalsBreakdown = MarinePointsScoreCalculator.breakdown(elementals, null);
        assertEquals(5, elementalsBreakdown.headCount());
        assertEquals(2.0, elementalsBreakdown.baseValue(), "Elemental trooper");
        assertEquals(2.0, elementalsBreakdown.weightClassModifier(), "medium weight class");
        assertEquals(4.0, elementalsBreakdown.perTrooper());
        assertEquals(50, elementalsBreakdown.intactArmor());
        assertEquals(25.0, elementalsBreakdown.armorPoints());
        assertEquals(45.0, elementalsBreakdown.score());

        MarinePointsBreakdown buildingBreakdown = MarinePointsScoreCalculator.breakdown(building, building);
        assertEquals(3, buildingBreakdown.crew(), "two gunners and an officer");
        assertEquals(1.5, buildingBreakdown.score());
        assertEquals(1.0, buildingBreakdown.buildingModifier(), "a one-level emplacement gets no modifier");
    }

    @Test
    @DisplayName("A loss line shows head-count times points lost over own strength, and the people lost")
    void lossLineShowsTheConversion() {
        ConvInfantry rifles = platoon(28, attackingPlayer, 1);

        reporter.reportUnitLoss(rifles, 28, 10, 83, 3, false);

        List<Report> reports = gameManager.getMainPhaseReport();
        assertEquals(1, reports.size());
        assertEquals(InfantryActionReporter.TROOPER_LOSS, reports.getFirst().messageId);
    }

    @Test
    @DisplayName("A unit whose share is under one trooper gets a line saying it lost nobody")
    void shareUnderOneTrooperSaysSo() {
        BattleArmor elementals = elementalPoint(attackingPlayer, 2);

        reporter.reportUnitLoss(elementals, 5, 10, 83, 0, false);

        assertEquals(InfantryActionReporter.NO_TROOPER_LOSS, gameManager.getMainPhaseReport().getFirst().messageId);
    }

    @Test
    @DisplayName("A side's casualty lines sit under their own header")
    void casualtiesHaveAHeader() {
        reporter.reportCasualtiesHeader(true);
        reporter.reportCasualtiesHeader(false);

        assertEquals(List.of(InfantryActionReporter.ATTACKERS_CASUALTIES_HEADER,
              InfantryActionReporter.DEFENDERS_CASUALTIES_HEADER), reportIds());
    }

    @Test
    @DisplayName("Report figures drop trailing zeros and keep two places")
    void figuresAreTidy() {
        assertEquals("21", InfantryActionReporter.number(21.0));
        assertEquals("12.25", InfantryActionReporter.number(12.25));
        assertEquals("3.37", InfantryActionReporter.number(28.0 * 10 / 83));
        assertEquals("0.75", InfantryActionReporter.number(0.75));
    }
}
