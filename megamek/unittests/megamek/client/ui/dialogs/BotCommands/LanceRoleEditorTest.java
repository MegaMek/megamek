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

import java.util.EnumSet;
import java.util.List;

import megamek.common.OffBoardDirection;
import megamek.common.board.Board;
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
          new LanceRoles.ConvoyChoice(CONVOY_FORCE_ID, "Supply Lance", 4));

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
}
