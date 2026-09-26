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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.AbstractClient;
import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.BipedMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the menu on a waypoint flag: changing the waypoint's facing and hold, and removing it, as edits of the route.
 */
class BotWaypointMenuBuilderTest {

    private static final int TEAM = 1;
    private static final int NORTH_EAST = 1;
    private static final Coords FIRST_HEX = Coords.parseHexNumber("1709");
    private static final Coords HOLD_HEX = Coords.parseHexNumber("1706");

    private AbstractClient client;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        Game game = new Game();
        Player human = new Player(0, "Defenders");
        human.setTeam(TEAM);
        game.addPlayer(0, human);
        Player bot = new Player(1, "Princess");
        bot.setBot(true);
        bot.setTeam(TEAM);
        game.addPlayer(1, bot);
        BipedMek mek = new BipedMek();
        mek.setId(20);
        mek.setOwner(bot);
        mek.setChassis("Grasshopper");
        mek.setModel("GHR-5H");
        game.addEntity(mek);
        mek.setPosition(new Coords(15, 15));
        mek.setUnitOrders(UnitOrders.NONE.withRoute(List.of(FIRST_HEX, HOLD_HEX),
              List.of(new WaypointOrder(NORTH_EAST, 2))));
        client = mock(AbstractClient.class);
        when(client.getGame()).thenReturn(game);
        when(client.getLocalPlayer()).thenReturn(human);
    }

    private static JMenuItem findItem(JMenu menu, String text) {
        for (int index = 0; index < menu.getItemCount(); index++) {
            JMenuItem item = menu.getItem(index);
            if ((item != null) && text.equals(item.getText())) {
                return item;
            }
        }
        return null;
    }

    private JMenu waypointMenuOn(Coords hex) {
        List<JMenu> menus = BotWaypointMenuBuilder.menusFor(client, hex, 0, null, (player, text) -> {});
        assertEquals(1, menus.size());
        return menus.get(0);
    }

    @Test
    void aFlagsFacingIsChangedAndItsHoldKept() {
        JMenu menu = waypointMenuOn(FIRST_HEX);
        assertEquals(Messages.getString("BotCommandPanel.Waypoint.menu", 1, "GHR-5H"), menu.getText());
        JMenu facingMenu = (JMenu) findItem(menu, Messages.getString("BotCommandPanel.MoveOrder.column.facing"));
        assertNotNull(facingMenu);

        findItem(facingMenu, Messages.getString("BotCommandPanel.Orders.facing.0")).doClick();

        verify(client).sendChat("/unitOrder 20 EDIT_ROUTE hexes=1709/N/2-1706");
    }

    @Test
    void theLastWaypointOffersNoHoldAndCanBeRemoved() {
        JMenu menu = waypointMenuOn(HOLD_HEX);
        assertFalse(findItem(menu, Messages.getString("BotCommandPanel.MoveOrder.column.hold")).isEnabled());

        findItem(menu, Messages.getString("BotCommandPanel.Waypoint.remove")).doClick();

        verify(client).sendChat("/unitOrder 20 EDIT_ROUTE hexes=1709/NE/2");
    }

    @Test
    void aHexWithNoWaypointHasNoMenu() {
        assertTrue(BotWaypointMenuBuilder.menusFor(client, new Coords(2, 2), 0, null, (player, text) -> {}).isEmpty());
    }
}
