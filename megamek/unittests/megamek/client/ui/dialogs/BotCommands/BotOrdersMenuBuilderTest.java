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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import megamek.client.AbstractClient;
import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BotOrdersMenuBuilder}: which groups of a bot's units the Orders menu offers, and that an order goes
 * to every unit of the chosen group as a {@code /unitOrder} command.
 */
class BotOrdersMenuBuilderTest {

    private static final int BOT_CONNECTION = 4;
    private static final String PICKED_HEXES = "1508-1504";

    private AbstractClient client;
    private Player bot;
    private BotOrdersMenuBuilder builder;
    private final List<BipedMek> units = new ArrayList<>();

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        Game game = new Game();
        bot = new Player(BOT_CONNECTION, "Lyran Allies");
        bot.setBot(true);
        game.addPlayer(BOT_CONNECTION, bot);
        int lanceId = game.getForces().addTopLevelForce(Force.createToplevelForce("Command Lance", bot), bot);
        for (int index = 0; index < 3; index++) {
            BipedMek mek = new BipedMek();
            mek.setId(20 + index);
            mek.setOwner(bot);
            mek.setChassis("Champion");
            mek.setModel("CHP-2N");
            game.addEntity(mek);
            mek.setDeployed(true);
            mek.setPosition(new Coords(5 + index, 5));
            if (index < 2) {
                game.getForces().addEntity(mek, lanceId);
            }
            units.add(mek);
        }
        client = mock(AbstractClient.class);
        when(client.getGame()).thenReturn(game);
        builder = new BotOrdersMenuBuilder(client,
              (orderDescription, singleHex, onPicked) -> onPicked.accept(PICKED_HEXES), (player, text) -> {});
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

    @Test
    void theGroupsAreAllUnitsEachLanceAndEachUnit() {
        List<BotOrdersMenuBuilder.OrderGroup> groups = builder.groupsFor(bot);

        assertEquals(5, groups.size());
        assertEquals(Messages.getString("BotCommandPanel.Orders.allUnits", 3), groups.get(0).label());
        assertEquals(List.of(20, 21, 22), groups.get(0).unitIds());
        assertEquals("Command Lance (2)", groups.get(1).label());
        assertEquals(List.of(20, 21), groups.get(1).unitIds());
    }

    @Test
    void aRouteOrderGoesToEveryUnitOfTheLance() {
        JMenu botMenu = new JMenu();
        builder.populate(botMenu, bot, null);
        JMenu lanceMenu = (JMenu) findItem(botMenu, "Command Lance (2)");
        assertNotNull(lanceMenu);
        JMenuItem moveTo = findItem(lanceMenu, Messages.getString("BotCommandPanel.Orders.moveTo"));
        assertNotNull(moveTo);

        moveTo.doClick();

        verify(client).sendChat("/unitOrder 20 ROUTE hexes=" + PICKED_HEXES);
        verify(client).sendChat("/unitOrder 21 ROUTE hexes=" + PICKED_HEXES);
        verify(client, never()).sendChat("/unitOrder 22 ROUTE hexes=" + PICKED_HEXES);
    }

    @Test
    void theRightClickedHexIsTheRoute() {
        JMenu botMenu = new JMenu();
        builder.populate(botMenu, bot, new Coords(14, 7));
        JMenu unitMenu = (JMenu) findItem(botMenu,
              Messages.getString("BotCommandPanel.Orders.unit", 22, units.get(2).getDisplayName()));
        assertNotNull(unitMenu);

        findItem(unitMenu, Messages.getString("BotCommandPanel.Orders.moveHere")).doClick();

        verify(client).sendChat("/unitOrder 22 ROUTE hexes=1508");
    }

    @Test
    void theChecklistGroupsUnitsByLance() {
        Map<String, List<BotOrdersMenuBuilder.OrderGroup>> unitsByLance = builder.unitsByLance(bot);

        assertEquals(List.of("Command Lance", Messages.getString("BotCommandPanel.Orders.noLance")),
              List.copyOf(unitsByLance.keySet()));
        assertEquals(2, unitsByLance.get("Command Lance").size());
    }

    @Test
    void chosenUnitsGetEveryOrder() {
        BotOrdersMenuBuilder.OrderGroup chosen = new BotOrdersMenuBuilder.OrderGroup("Chosen units (2)",
              List.of(20, 22));

        JPopupMenu popup = builder.ordersPopup(bot, chosen);
        JMenuItem pause = null;
        for (int index = 0; index < popup.getComponentCount(); index++) {
            if ((popup.getComponent(index) instanceof JMenuItem item)
                  && Messages.getString("BotCommandPanel.Orders.pause").equals(item.getText())) {
                pause = item;
            }
        }
        assertNotNull(pause);
        pause.doClick();

        verify(client).sendChat("/unitOrder 20 PAUSE");
        verify(client).sendChat("/unitOrder 22 PAUSE");
    }

    @Test
    void aNewRouteAsksForTheFacings() {
        builder.withFacingChooser((group, moving, stopped, onChosen) -> onChosen.accept(0, 1));
        JMenu botMenu = new JMenu();
        builder.populate(botMenu, bot, null);
        JMenu lanceMenu = (JMenu) findItem(botMenu, "Command Lance (2)");

        findItem(lanceMenu, Messages.getString("BotCommandPanel.Orders.moveTo")).doClick();

        verify(client).sendChat("/unitOrder 20 FACING moving=0 stopped=1");
        verify(client).sendChat("/unitOrder 21 FACING moving=0 stopped=1");
    }
}
