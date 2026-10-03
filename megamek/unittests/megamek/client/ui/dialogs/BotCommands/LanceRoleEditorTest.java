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
package megamek.client.ui.dialogs.BotCommands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;
import megamek.common.orders.UnitOrders;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Tests the lance Role panel shared by the Move Order editor and the lobby, the commands a role turns into, and the
 * defaults a new convoy starts with.
 */
class LanceRoleEditorTest {

    private static final int CONVOY_FORCE_ID = 7;
    private static final List<LanceRoles.ConvoyChoice> ONE_CONVOY = List.of(
          LanceRoles.ConvoyChoice.convoy(CONVOY_FORCE_ID, "Supply Lance", 4));

    private static Entity unit(int unitId, int startingPosition) {
        Entity unit = new BipedMek();
        unit.setId(unitId);
        unit.setStartingPos(startingPosition);
        return unit;
    }

    @Test
    void aConvoyLeavesByTheEdgeOppositeWhereItDeploys() {
        assertEquals(OffBoardDirection.NORTH, LanceRoles.defaultExitEdge(unit(1, Board.START_S)));
        assertEquals(OffBoardDirection.SOUTH, LanceRoles.defaultExitEdge(unit(1, Board.START_N)));
        assertEquals(OffBoardDirection.SOUTH, LanceRoles.defaultExitEdge(unit(1, Board.START_NW)));
        assertEquals(OffBoardDirection.WEST, LanceRoles.defaultExitEdge(unit(1, Board.START_E)));
        assertEquals(OffBoardDirection.EAST, LanceRoles.defaultExitEdge(unit(1, Board.START_W)));
        assertEquals(OffBoardDirection.NORTH, LanceRoles.defaultExitEdge(unit(1, Board.START_ANY)));
        assertEquals(OffBoardDirection.NORTH, LanceRoles.defaultExitEdge(null));
    }

    @Test
    void aRoleIsSentOnlyToUnitsThatDoNotHaveItYet() {
        LanceRole convoy = LanceRole.convoy(OffBoardDirection.EAST);
        Entity alreadySet = unit(1, Board.START_W);
        alreadySet.setLanceRole(convoy);
        Entity notSet = unit(2, Board.START_W);

        List<String> commands = MoveOrderCommands.roleCommands(List.of(alreadySet, notSet), convoy);

        assertEquals(List.of("/unitOrder 2 SET_ROLE role=CONVOY:EAST"), commands);
        assertEquals(List.of("/unitOrder 1 SET_ROLE role=NONE"),
              MoveOrderCommands.roleCommands(List.of(alreadySet, notSet), null));
    }

    @Test
    void theLanceTakesTheRoleItsUnitsHave() {
        Entity noRole = unit(1, Board.START_S);
        Entity escort = unit(2, Board.START_S);
        escort.setLanceRole(LanceRole.defaultEscort(CONVOY_FORCE_ID));

        assertEquals(LanceRole.defaultEscort(CONVOY_FORCE_ID), LanceRoles.roleOf(List.of(noRole, escort)));
        assertNull(LanceRoles.roleOf(List.of(noRole)));
    }

    @Test
    void theRolePanelGivesBackTheRoleItWasShown() {
        LanceRolePanel panel = new LanceRolePanel("Fire Lance", ONE_CONVOY, OffBoardDirection.NORTH);
        assertNull(panel.getRole());
        assertTrue(panel.isComplete());

        LanceRole convoy = LanceRole.convoy(OffBoardDirection.WEST);
        panel.setRole(convoy);
        assertEquals(convoy, panel.getRole());
        assertFalse(panel.isEscortChosen());

        LanceRole escort = LanceRole.escort(CONVOY_FORCE_ID, EnumSet.of(LanceRole.Position.LEAD,
                    LanceRole.Position.REAR), LanceRole.Distance.FAR, LanceRole.Movement.BOUNDING,
              LanceRole.Contact.STAY, LanceRole.LeaveToFight.HUNT, LanceRole.WhenConvoyGone.BREAK_OFF);
        panel.setRole(escort);
        assertEquals(escort, panel.getRole());
        assertTrue(panel.isEscortChosen());

        panel.setRole(null);
        assertNull(panel.getRole());
    }

    @Test
    void anEscortWithNoConvoyToGuardCannotBeSent() {
        LanceRolePanel panel = new LanceRolePanel("Fire Lance", List.of(), OffBoardDirection.NORTH);

        panel.setRole(LanceRole.defaultEscort(CONVOY_FORCE_ID));

        assertTrue(panel.isEscortChosen());
        assertFalse(panel.isComplete());
        assertNull(panel.getRole());
    }

    @Test
    void aConvoyIsPutInARunningColumnLedByItsFirstUnit() {
        List<Entity> trucks = List.of(unit(3, Board.START_S), unit(4, Board.START_S), unit(5, Board.START_S));

        List<String> commands = MoveOrderCommands.convoyColumnCommands(trucks,
              LanceRole.convoy(OffBoardDirection.NORTH));

        assertEquals(3, commands.size());
        assertEquals("/unitOrder 4 FORMATION shape=COLUMN leader=3 spacing=1 slot=1 pace=RUN contact=HOLD together=true",
              commands.get(1));
    }

    @Test
    void aShapeThePlayerSetIsKeptAndOnlyAConvoyGetsAColumn() {
        List<Entity> trucks = List.of(unit(3, Board.START_S), unit(4, Board.START_S));
        LanceRole convoy = LanceRole.convoy(OffBoardDirection.NORTH);

        assertTrue(MoveOrderCommands.convoyColumnCommands(trucks, LanceRole.defaultEscort(CONVOY_FORCE_ID)).isEmpty());
        assertTrue(MoveOrderCommands.convoyColumnCommands(trucks.subList(0, 1), convoy).isEmpty());

        trucks.get(1).setUnitOrders(UnitOrders.NONE.withFormation(new FormationOrder(FormationShape.WEDGE, 3, 1, 1,
              FormationPace.WALK, ContactRule.BREAK)));
        assertTrue(MoveOrderCommands.convoyColumnCommands(trucks, convoy).isEmpty());
    }

    /** A game with two bot lances, Supply and Fire, and a human player's lance on the same side. */
    private record Lances(Game game, Player bot, int supplyId, int fireId, int humanLanceId, List<Entity> supply,
          List<Entity> fire, Entity humanUnit) {}

    private static Lances lances() {
        Game game = new Game();
        Player bot = new Player(1, "Princess");
        bot.setBot(true);
        game.addPlayer(1, bot);
        bot.setTeam(1);
        Player human = new Player(0, "Raven's Nest");
        human.setTeam(1);
        game.addPlayer(0, human);
        int supplyId = game.getForces().addTopLevelForce(Force.createToplevelForce("Supply Lance", bot), bot);
        int fireId = game.getForces().addTopLevelForce(Force.createToplevelForce("Fire Lance", bot), bot);
        int humanLanceId = game.getForces().addTopLevelForce(Force.createToplevelForce("Wraith", human), human);
        List<Entity> supply = List.of(lanceUnit(game, 3, bot, supplyId), lanceUnit(game, 4, bot, supplyId));
        List<Entity> fire = List.of(lanceUnit(game, 10, bot, fireId), lanceUnit(game, 11, bot, fireId));
        Entity humanUnit = lanceUnit(game, 20, human, humanLanceId);
        return new Lances(game, bot, supplyId, fireId, humanLanceId, supply, fire, humanUnit);
    }

    private static List<String> names(List<LanceRoles.ConvoyChoice> choices) {
        List<String> names = new ArrayList<>();
        for (LanceRoles.ConvoyChoice choice : choices) {
            names.add(choice.name());
        }
        return names;
    }

    private static Entity lanceUnit(Game game, int unitId, Player owner, int lanceId) {
        Entity unit = new BipedMek();
        unit.setId(unitId);
        unit.setOwner(owner);
        unit.setStartingPos(Board.START_S);
        game.addEntity(unit);
        game.getForces().addEntity(unit, lanceId);
        return unit;
    }

    @Test
    void aUnitAddedToALanceLaterTakesTheLancesRole() {
        Lances lances = lances();
        LanceRole convoy = LanceRole.convoy(OffBoardDirection.NORTH);
        lances.supply().get(0).setLanceRole(convoy);

        assertEquals(convoy, lances.supply().get(1).getLanceRole());
        assertNull(lances.supply().get(1).getOwnLanceRole());
        assertNull(lances.fire().get(0).getLanceRole(), "another lance does not take it");
    }

    @Test
    void anyOtherLanceCanBeEscortedConvoysFirst() {
        Lances lances = lances();
        lances.fire().get(0).setLanceRole(LanceRole.convoy(OffBoardDirection.WEST));

        List<LanceRoles.ConvoyChoice> choices = LanceRoles.convoyChoices(lances.game(), lances.bot(), -1);

        // the Wraith's lance has no bot unit to take the convoy role
        assertEquals(List.of("Fire Lance", "Supply Lance"), names(choices));
        assertTrue(choices.get(0).isConvoy());
        assertFalse(choices.get(1).isConvoy());
        assertEquals(OffBoardDirection.NORTH, choices.get(1).newExitEdge(), "deploying south, it leaves north");
    }

    @Test
    void anEscortLanceIsNotOfferedAsAConvoy() {
        Lances lances = lances();
        lances.fire().get(0).setLanceRole(LanceRole.defaultEscort(lances.supplyId()));

        List<LanceRoles.ConvoyChoice> choices = LanceRoles.convoyChoices(lances.game(), lances.bot(), -1);

        assertEquals(List.of("Supply Lance"), names(choices));
    }

    @Test
    void aLanceGivenAnEscortBecomesAConvoyOnItsBotUnitsOnly() {
        Lances lances = lances();
        List<Entity> mixed = List.of(lances.supply().get(0), lances.supply().get(1), lances.humanUnit());

        List<String> commands = MoveOrderCommands.makeConvoyCommands(mixed, OffBoardDirection.NORTH);

        assertEquals(List.of("/unitOrder 3 SET_ROLE role=CONVOY:NORTH", "/unitOrder 4 SET_ROLE role=CONVOY:NORTH",
              "/unitOrder 3 FORMATION shape=COLUMN leader=3 spacing=1 slot=0 pace=RUN contact=HOLD together=true",
              "/unitOrder 4 FORMATION shape=COLUMN leader=3 spacing=1 slot=1 pace=RUN contact=HOLD together=true"),
              commands);
    }

    @Test
    void theRolePanelSaysWhenTheEscortedLanceBecomesAConvoy() {
        LanceRoles.ConvoyChoice notYet = new LanceRoles.ConvoyChoice(5, "Fire Lance", 4, false,
              OffBoardDirection.EAST);
        LanceRolePanel panel = new LanceRolePanel("Striker Lance", List.of(notYet), OffBoardDirection.NORTH);

        panel.setRole(LanceRole.defaultEscort(5));

        assertEquals(Optional.of(notYet), panel.lanceToMakeConvoy());
        panel.setRole(null);
        assertTrue(panel.lanceToMakeConvoy().isEmpty());
    }

    @Test
    void aConvoySetToWaitIsSentAsSuch() {
        LanceRole waiting = LanceRole.convoy(OffBoardDirection.EAST, true);
        LanceRolePanel panel = new LanceRolePanel("Supply Lance", List.of(), OffBoardDirection.NORTH);

        panel.setRole(waiting);

        assertEquals(waiting, panel.getRole());
        assertEquals("CONVOY:EAST:WAIT", waiting.toCommandText());
        assertEquals(waiting, LanceRole.parse("CONVOY:EAST:WAIT"));
        assertFalse(LanceRole.parse("CONVOY:EAST").isWaitingAtRouteEnd(), "a convoy leaves unless told to wait");
    }
}
