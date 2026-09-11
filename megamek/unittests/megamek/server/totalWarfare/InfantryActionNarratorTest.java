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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.MarinePointsTrait;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.server.totalWarfare.InfantryActionNarrator.Outcome;
import megamek.server.totalWarfare.InfantryActionNarrator.Side;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The story under the header: a lead for the outcome, then for each side its linked names, a clause from its
 * strongest trait, and its loss.
 */
@DisplayName("Infantry action narrative")
class InfantryActionNarratorTest {

    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final int SECOND_LEAD = 1;

    private Game game;
    private TWGameManager gameManager;
    private Player attackingPlayer;
    private Player defendingPlayer;
    private BuildingEntity building;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        attackingPlayer = new Player(0, "Attacker");
        defendingPlayer = new Player(1, "Defender");
        game.addPlayer(0, attackingPlayer);
        game.addPlayer(1, defendingPlayer);
        game.setBoard(new Board(16, 17));
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        gameManager.setGame(game);

        building = new BuildingEntity(BuildingType.LIGHT, 3);
        building.setOwner(defendingPlayer);
        building.setGame(game);
        building.setPosition(BUILDING_HEX);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 30, 20, BasementType.NONE, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.getCrew().setSize(4);
        building.getCrew().setCurrentSize(4);
        building.setId(0);
        game.addEntity(building);
    }

    private ConvInfantry platoon(Player owner, int id) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        infantry.setId(id);
        game.addEntity(infantry);
        infantry.setPosition(BUILDING_HEX);
        return infantry;
    }

    private InfantryActionNarrator narratorPicking(int lead) {
        return new InfantryActionNarrator(gameManager, count -> lead);
    }

    private List<Integer> reportIds() {
        return gameManager.getMainPhaseReport().stream().map(report -> report.messageId).toList();
    }

    private static Side side(List<Entity> units, int lost, int own, boolean eliminated, MarinePointsTrait... traits) {
        Set<MarinePointsTrait> traitSet = EnumSet.noneOf(MarinePointsTrait.class);
        traitSet.addAll(List.of(traits));
        return new Side(units, traitSet, lost, own, eliminated);
    }

    @Test
    @DisplayName("A repulse: the lead, then each side's name, clause and loss, each sentence closed")
    void repulseTellsBothSides() {
        ConvInfantry marines = platoon(attackingPlayer, 1);

        narratorPicking(SECOND_LEAD).narrate(Outcome.REPULSED,
              side(List.of(marines), 40, 40, false, MarinePointsTrait.MARINES),
              side(List.of(building), 6, 40, false, MarinePointsTrait.BUILDING_CREW), null);

        assertEquals(List.of(5674,
              InfantryActionNarrator.UNIT_NAME, 5688, InfantryActionNarrator.LOST_SHARE,
              InfantryActionNarrator.FULL_STOP,
              InfantryActionNarrator.BUILDING_CREW_NAME, 5691, InfantryActionNarrator.LOST_SHARE,
              InfantryActionNarrator.FULL_STOP), reportIds());
    }

    @Test
    @DisplayName("Several names are joined with commas and a final and; a wiped-out side says none survived")
    void severalNamesAndAWipedOutSide() {
        ConvInfantry first = platoon(attackingPlayer, 1);
        ConvInfantry second = platoon(attackingPlayer, 2);
        ConvInfantry third = platoon(attackingPlayer, 3);
        ConvInfantry defender = platoon(defendingPlayer, 4);

        narratorPicking(0).narrate(Outcome.ATTACKERS_ELIMINATED,
              side(List.of(first, second, third), 20, 20, true, MarinePointsTrait.CIVILIANS),
              side(List.of(defender), 0, 60, false, MarinePointsTrait.ELEMENTALS), null);

        assertEquals(List.of(5667,
              InfantryActionNarrator.UNIT_NAME, InfantryActionNarrator.COMMA, InfantryActionNarrator.UNIT_NAME,
              InfantryActionNarrator.AND, InfantryActionNarrator.UNIT_NAME, 5692,
              InfantryActionNarrator.NONE_SURVIVED, InfantryActionNarrator.FULL_STOP,
              InfantryActionNarrator.UNIT_NAME, 5686, InfantryActionNarrator.LOST_NOBODY,
              InfantryActionNarrator.FULL_STOP), reportIds());
    }

    @Test
    @DisplayName("When the building falls with crew uncommitted, the sentence ends with its name and their surrender")
    void capturedBuildingEndsTheDefendersSentence() {
        ConvInfantry attacker = platoon(attackingPlayer, 1);
        ConvInfantry defender = platoon(defendingPlayer, 2);

        narratorPicking(0).narrate(Outcome.DEFENDERS_ELIMINATED,
              side(List.of(attacker), 6, 93, false, MarinePointsTrait.BURST_FIRE),
              side(List.of(defender), 21, 21, true, MarinePointsTrait.LINE_INFANTRY), building);

        List<Integer> ids = reportIds();
        assertEquals(List.of(InfantryActionNarrator.UNIT_NAME, 5689, InfantryActionNarrator.NONE_SURVIVED,
              InfantryActionNarrator.AND_THE_BUILDING, InfantryActionNarrator.UNIT_NAME,
              InfantryActionNarrator.BUILDING_FALLS), ids.subList(ids.size() - 6, ids.size()));
        List<Report> reports = gameManager.getMainPhaseReport();
        for (int index = 0; index < reports.size() - 1; index++) {
            assertEquals(0, reports.get(index).newlines, "fragment " + index + " runs on");
        }
        assertEquals(1, reports.getLast().newlines);
    }

    @Test
    @DisplayName("When every crew member was committed and lost, the building simply falls")
    void capturedBuildingWithNoCrewLeftSimplyFalls() {
        building.commitCrew(4);
        ConvInfantry attacker = platoon(attackingPlayer, 1);

        narratorPicking(0).narrate(Outcome.DEFENDERS_ELIMINATED,
              side(List.of(attacker), 6, 93, false, MarinePointsTrait.BURST_FIRE),
              side(List.of(building), 2, 2, true, MarinePointsTrait.BUILDING_CREW), building);

        List<Integer> ids = reportIds();
        assertEquals(List.of(InfantryActionNarrator.BUILDING_CREW_NAME, 5691, InfantryActionNarrator.NONE_SURVIVED,
              InfantryActionNarrator.AND_THE_BUILDING, InfantryActionNarrator.UNIT_NAME,
              InfantryActionNarrator.BUILDING_FALLS_NOBODY_LEFT), ids.subList(ids.size() - 6, ids.size()));
    }

    @Test
    @DisplayName("A side with nobody to name is called the attackers or the defenders")
    void unnamedSidesGetAPlainSubject() {
        narratorPicking(0).narrate(Outcome.ENGAGED, side(List.of(), 12, 50, false), side(List.of(), 12, 25, false),
              null);

        List<Integer> ids = reportIds();
        assertEquals(InfantryActionNarrator.THE_ATTACKERS, ids.get(1));
        assertEquals(InfantryActionNarrator.PLAIN_TROOPERS, ids.get(2));
        assertTrue(ids.contains(InfantryActionNarrator.THE_DEFENDERS));
    }

    @Test
    @DisplayName("The lead picker's choice wraps into the outcome's leads")
    void leadChoiceWraps() {
        narratorPicking(InfantryActionNarrator.LEADS_PER_OUTCOME + 2).narrate(Outcome.PENETRATION,
              side(List.of(), 12, 60, false), side(List.of(), 39, 40, false), null);

        assertEquals(5681, reportIds().getFirst());
    }

    @Test
    @DisplayName("Units go by their chassis, unless two in the story share one")
    void unitsGoByChassisUnlessShared() {
        ConvInfantry rifles = platoon(attackingPlayer, 1);
        rifles.setChassis("Foot Platoon");
        rifles.setModel("(Rifle)");
        ConvInfantry lasers = platoon(defendingPlayer, 2);
        lasers.setChassis("Foot Platoon");
        lasers.setModel("(Laser)");
        ConvInfantry jump = platoon(defendingPlayer, 3);
        jump.setChassis("Jump Platoon");
        jump.setModel("(SRM)");

        assertEquals("Jump Platoon", InfantryActionNarrator.storyName(jump, List.of(rifles, lasers, jump)));
        assertEquals(rifles.getShortName(), InfantryActionNarrator.storyName(rifles, List.of(rifles, lasers, jump)));
        assertEquals("Foot Platoon", InfantryActionNarrator.storyName(rifles, List.of(rifles, jump)));
    }

    @Test
    @DisplayName("The strongest trait is the one that most shaped the fighting")
    void strongestTraitWins() {
        assertEquals(MarinePointsTrait.BURST_FIRE, InfantryActionNarrator.strongestTrait(
              EnumSet.of(MarinePointsTrait.ELEMENTALS, MarinePointsTrait.BURST_FIRE)));
        assertEquals(MarinePointsTrait.MARINES, InfantryActionNarrator.strongestTrait(
              EnumSet.of(MarinePointsTrait.BUILDING_CREW, MarinePointsTrait.MARINES)));
        assertNull(InfantryActionNarrator.strongestTrait(EnumSet.noneOf(MarinePointsTrait.class)));
    }

    @Test
    @DisplayName("The loss is a share of the side's own strength, never zero for a real loss")
    void lossIsAShareOfOwnStrength() {
        assertEquals(15, InfantryActionNarrator.percentOfOwnStrength(side(List.of(), 6, 40, false)));
        assertEquals(100, InfantryActionNarrator.percentOfOwnStrength(side(List.of(), 56, 40, false)));
        assertEquals(1, InfantryActionNarrator.percentOfOwnStrength(side(List.of(), 1, 500, false)));
    }

    @Test
    @DisplayName("A side read from the game names only the units that scored, and gathers their traits")
    void sideFromTheGameNamesWhoCounted() {
        ConvInfantry platoon = platoon(defendingPlayer, 1);
        // The building's crew are not committed, so it scores nothing and is not named
        Side defenders = Side.of(game, List.of(building.getId(), platoon.getId()), building, 5, 21, false);

        assertEquals(List.of(platoon), defenders.units());
        assertTrue(defenders.traits().contains(MarinePointsTrait.LINE_INFANTRY));

        building.commitCrew(4);
        Side withCrew = Side.of(game, List.of(building.getId(), platoon.getId()), building, 5, 23, false);
        assertEquals(List.of(building, platoon), withCrew.units());
        assertTrue(withCrew.traits().contains(MarinePointsTrait.BUILDING_CREW));
    }
}
