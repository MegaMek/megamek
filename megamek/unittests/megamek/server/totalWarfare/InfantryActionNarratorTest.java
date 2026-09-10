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
import megamek.server.totalWarfare.InfantryActionNarrator.Outcome;
import megamek.server.totalWarfare.InfantryActionNarrator.Side;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The story under the numbers: a lead for the outcome, a clause per side from its strongest trait, and its loss.
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
        building.commitCrew(4);
        building.setId(0);
        game.addEntity(building);
    }

    private InfantryActionNarrator narratorPicking(int lead) {
        return new InfantryActionNarrator(gameManager, count -> lead);
    }

    private List<Integer> reportIds() {
        return gameManager.getMainPhaseReport().stream().map(report -> report.messageId).toList();
    }

    private static Side side(int lost, int own, boolean eliminated, MarinePointsTrait... traits) {
        Set<MarinePointsTrait> traitSet = EnumSet.noneOf(MarinePointsTrait.class);
        traitSet.addAll(List.of(traits));
        return new Side(lost, own, eliminated, traitSet);
    }

    @Test
    @DisplayName("A repulse reads as the lead, the marines' clause and their loss, then the crew's clause and loss")
    void repulseTellsBothSides() {
        narratorPicking(SECOND_LEAD).narrate(Outcome.REPULSED, side(40, 40, false, MarinePointsTrait.MARINES),
              side(6, 40, false, MarinePointsTrait.BUILDING_CREW));

        assertEquals(List.of(5674, 5688, InfantryActionNarrator.ATTACKERS_LOST_SHARE, 5691,
              InfantryActionNarrator.DEFENDERS_LOST_SHARE), reportIds());
    }

    @Test
    @DisplayName("An eliminated side is wiped out and a side that lost nothing lost nobody")
    void eliminatedAndUntouchedSides() {
        narratorPicking(0).narrate(Outcome.ATTACKERS_ELIMINATED, side(20, 20, true, MarinePointsTrait.CIVILIANS),
              side(0, 60, false, MarinePointsTrait.ELEMENTALS));

        assertEquals(List.of(5667, 5692, InfantryActionNarrator.ATTACKERS_LOST_ALL, 5686,
              InfantryActionNarrator.DEFENDERS_LOST_NOBODY), reportIds());
    }

    @Test
    @DisplayName("Only the lead's last line ends the paragraph")
    void theStoryIsOneParagraph() {
        narratorPicking(0).narrate(Outcome.ENGAGED, side(12, 50, false), side(12, 25, false));

        List<Report> reports = gameManager.getMainPhaseReport();
        for (int index = 0; index < reports.size() - 1; index++) {
            assertEquals(0, reports.get(index).newlines, "line " + index + " runs on");
        }
        assertEquals(1, reports.getLast().newlines);
        assertEquals(InfantryActionNarrator.PLAIN_TROOPERS, reports.get(1).messageId, "no trait, plain clause");
    }

    @Test
    @DisplayName("The lead picker's choice wraps into the outcome's leads")
    void leadChoiceWraps() {
        narratorPicking(InfantryActionNarrator.LEADS_PER_OUTCOME + 2).narrate(Outcome.PENETRATION,
              side(12, 60, false), side(39, 40, false));

        assertEquals(5681, reportIds().getFirst());
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
        assertEquals(15, InfantryActionNarrator.percentOfOwnStrength(side(6, 40, false)));
        assertEquals(100, InfantryActionNarrator.percentOfOwnStrength(side(56, 40, false)));
        assertEquals(1, InfantryActionNarrator.percentOfOwnStrength(side(1, 500, false)));
    }

    @Test
    @DisplayName("A side's traits are the union over its units")
    void sideTraitsAreTheUnion() {
        ConvInfantry platoon = new ConvInfantry();
        platoon.setOwner(defendingPlayer);
        platoon.setGame(game);
        platoon.setSquadSize(28);
        platoon.setSquadCount(1);
        platoon.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        platoon.setId(1);
        game.addEntity(platoon);
        platoon.setPosition(BUILDING_HEX);

        Set<MarinePointsTrait> traits = narratorPicking(0).traitsOf(List.of(building.getId(), platoon.getId()),
              building);

        assertTrue(traits.contains(MarinePointsTrait.BUILDING_CREW));
        assertTrue(traits.contains(MarinePointsTrait.LINE_INFANTRY));
    }
}
