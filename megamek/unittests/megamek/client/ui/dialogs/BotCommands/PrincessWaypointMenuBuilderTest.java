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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.AbstractClient;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.ui.Messages;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;
import megamek.server.commands.arguments.BooleanArgument;
import megamek.server.commands.arguments.MultiHexNumberArgument;
import megamek.server.commands.arguments.UnitArgument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests for {@link PrincessWaypointMenuBuilder}: a Princess bot's Move Order and Quick Orders menus offer her waypoint
 * and hold-position commands, and each item sends a chat line her own command parser accepts.
 */
class PrincessWaypointMenuBuilderTest {

    private static final int BOT_CONNECTION = 4;
    private static final String BOT_NAME = "Lyran Allies";
    private static final String PICKED_HEXES = "1508-1504";

    private AbstractClient client;
    private Player bot;
    private PrincessWaypointMenuBuilder builder;
    private final List<BipedMek> units = new ArrayList<>();

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        Game game = new Game();
        bot = new Player(BOT_CONNECTION, BOT_NAME);
        bot.setBot(true);
        game.addPlayer(BOT_CONNECTION, bot);
        for (int index = 0; index < 2; index++) {
            BipedMek mek = new BipedMek();
            mek.setId(20 + index);
            mek.setOwner(bot);
            mek.setChassis("Champion");
            mek.setModel("CHP-2N");
            game.addEntity(mek);
            mek.setDeployed(true);
            mek.setPosition(new Coords(5 + index, 5));
            units.add(mek);
        }
        client = mock(AbstractClient.class);
        when(client.getGame()).thenReturn(game);
        builder = new PrincessWaypointMenuBuilder(client,
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

    private static String princessText(String key) {
        return Messages.getString("BotCommandPanel.PrincessOrders." + key);
    }

    /** Clicks the item and returns the chat line it sent. */
    private String clickAndCaptureChat(JMenuItem item) {
        assertNotNull(item);
        item.doClick();
        ArgumentCaptor<String> chatLine = ArgumentCaptor.forClass(String.class);
        verify(client).sendChat(chatLine.capture());
        return chatLine.getValue();
    }

    /**
     * Reads a chat line the way {@code ChatProcessor} does once the server has put the sender's name in front:
     * sender, bot, command, then the arguments split on spaces; then parses them against the command's own argument
     * definitions, which throws if the line is not one Princess would accept.
     */
    private static Arguments parseAsPrincess(String chatLine, ChatCommands expectedCommand) {
        StringTokenizer tokenizer = new StringTokenizer("HammerGS: " + chatLine, ":");
        assertTrue(tokenizer.countTokens() >= 3, "Princess ignores a chat line with fewer than three parts");
        tokenizer.nextToken();
        assertEquals(BOT_NAME, tokenizer.nextToken().trim());
        String command = tokenizer.nextToken().trim();
        assertEquals(expectedCommand.getAbbreviation(), command);
        List<String> arguments = new ArrayList<>();
        arguments.add(command);
        if (tokenizer.hasMoreTokens()) {
            arguments.addAll(Arrays.asList(tokenizer.nextToken().trim().split(" ")));
        }
        return ArgumentsParser.parse(arguments.toArray(String[]::new),
              expectedCommand.getChatCommand().defineArguments());
    }

    @Test
    void theMoveOrderMenuHasEachUnitThenClearAll() {
        JMenu botMenu = new JMenu();
        builder.populateMoveOrders(botMenu, bot);

        JMenuItem firstUnit = botMenu.getItem(0);
        assertInstanceOf(JMenu.class, firstUnit);
        assertEquals(Messages.getString("BotCommandPanel.Orders.unit", 20, units.get(0).getDisplayName()),
              firstUnit.getText());
        JMenu unitMenu = (JMenu) firstUnit;
        assertNotNull(findItem(unitMenu, princessText("setWaypoints")));
        assertNotNull(findItem(unitMenu, princessText("addWaypoint")));
        assertNotNull(findItem(unitMenu, princessText("clearWaypoints")));
        assertNotNull(findItem(botMenu, princessText("clearAllWaypoints")));
    }

    @Test
    void setWaypointsSendsTheUnitAndPickedHexesPrincessCanRead() {
        JMenu botMenu = new JMenu();
        builder.populateMoveOrders(botMenu, bot);

        String chatLine = clickAndCaptureChat(findItem((JMenu) botMenu.getItem(1), princessText("setWaypoints")));

        assertEquals(BOT_NAME + ": sw : 21 " + PICKED_HEXES, chatLine);
        Arguments arguments = parseAsPrincess(chatLine, ChatCommands.SET_WAYPOINT);
        assertEquals(21, arguments.get("unitID", UnitArgument.class).getValue());
        assertEquals(List.of(Coords.parseHexNumber("1508"), Coords.parseHexNumber("1504")),
              arguments.get("hexNumber", MultiHexNumberArgument.class).getValue());
    }

    @Test
    void addWaypointSendsTheUnitAndPickedHexesPrincessCanRead() {
        JMenu botMenu = new JMenu();
        builder.populateMoveOrders(botMenu, bot);

        String chatLine = clickAndCaptureChat(findItem((JMenu) botMenu.getItem(0), princessText("addWaypoint")));

        Arguments arguments = parseAsPrincess(chatLine, ChatCommands.ADD_WAYPOINT);
        assertEquals(20, arguments.get("unitID", UnitArgument.class).getValue());
        assertEquals(2, arguments.get("hexNumber", MultiHexNumberArgument.class).getValue().size());
    }

    @Test
    void clearWaypointsSendsTheUnitPrincessCanRead() {
        JMenu botMenu = new JMenu();
        builder.populateMoveOrders(botMenu, bot);

        String chatLine = clickAndCaptureChat(findItem((JMenu) botMenu.getItem(0), princessText("clearWaypoints")));

        Arguments arguments = parseAsPrincess(chatLine, ChatCommands.CLEAR_WAYPOINT);
        assertEquals(20, arguments.get("unitID", UnitArgument.class).getValue());
    }

    @Test
    void removeLastWaypointSendsTheUnitPrincessCanRead() {
        JMenu botMenu = new JMenu();
        builder.populateMoveOrders(botMenu, bot);

        String chatLine = clickAndCaptureChat(findItem((JMenu) botMenu.getItem(1),
              princessText("removeLastWaypoint")));

        Arguments arguments = parseAsPrincess(chatLine, ChatCommands.REMOVE_WAYPOINT);
        assertEquals(21, arguments.get("unitID", UnitArgument.class).getValue());
    }

    @Test
    void retreatToAnEdgeSendsTheEdgePrincessCanRead() {
        JMenu botMenu = new JMenu();
        builder.populateQuickOrders(botMenu, bot);
        JMenuItem retreatMenu = findItem(botMenu, princessText("retreat"));
        assertInstanceOf(JMenu.class, retreatMenu);

        String chatLine = clickAndCaptureChat(findItem((JMenu) retreatMenu, princessText("retreat.EAST")));

        Arguments arguments = parseAsPrincess(chatLine, ChatCommands.FLEE);
        assertEquals(CardinalEdge.EAST, arguments.getEnum("edge", CardinalEdge.class));
    }

    @Test
    void theRetreatMenuOffersEveryEdgeAndTheNearest() {
        JMenu botMenu = new JMenu();
        builder.populateQuickOrders(botMenu, bot);
        JMenu retreatMenu = (JMenu) findItem(botMenu, princessText("retreat"));

        for (CardinalEdge edge : List.of(CardinalEdge.NORTH, CardinalEdge.EAST, CardinalEdge.SOUTH,
              CardinalEdge.WEST, CardinalEdge.NEAREST)) {
            assertNotNull(findItem(retreatMenu, princessText("retreat." + edge.name())), edge.name());
        }
    }

    @Test
    void cancelRetreatSendsNoEdge() {
        JMenu botMenu = new JMenu();
        builder.populateQuickOrders(botMenu, bot);

        String chatLine = clickAndCaptureChat(findItem(botMenu, princessText("retreat.NONE")));

        Arguments arguments = parseAsPrincess(chatLine, ChatCommands.FLEE);
        assertEquals(CardinalEdge.NONE, arguments.getEnum("edge", CardinalEdge.class));
    }

    @Test
    void clearAllWaypointsSendsTheCommandWithNoArguments() {
        JMenu botMenu = new JMenu();
        builder.populateMoveOrders(botMenu, bot);

        String chatLine = clickAndCaptureChat(findItem(botMenu, princessText("clearAllWaypoints")));

        assertEquals(BOT_NAME + ": nw", chatLine);
        parseAsPrincess(chatLine, ChatCommands.CLEAR_ALL_WAYPOINTS);
    }

    @Test
    void holdPositionSendsTrue() {
        JMenu botMenu = new JMenu();
        builder.populateQuickOrders(botMenu, bot);

        String chatLine = clickAndCaptureChat(findItem(botMenu, princessText("holdPosition")));

        Arguments arguments = parseAsPrincess(chatLine, ChatCommands.HOLD_POSITION);
        assertTrue(arguments.get("hold", BooleanArgument.class).getValue());
    }

    @Test
    void resumeSendsFalse() {
        JMenu botMenu = new JMenu();
        builder.populateQuickOrders(botMenu, bot);

        String chatLine = clickAndCaptureChat(findItem(botMenu, princessText("resume")));

        Arguments arguments = parseAsPrincess(chatLine, ChatCommands.HOLD_POSITION);
        assertFalse(arguments.get("hold", BooleanArgument.class).getValue());
    }

    @Test
    void destroyedAndOffBoardUnitsGetNoWaypoints() {
        units.get(0).setDestroyed(true);
        units.get(1).setOffBoard(1, OffBoardDirection.NORTH);

        JMenu moveMenu = new JMenu();
        builder.populateMoveOrders(moveMenu, bot);
        JMenu quickMenu = new JMenu();
        builder.populateQuickOrders(quickMenu, bot);

        assertEquals(0, moveMenu.getItemCount());
        assertEquals(0, quickMenu.getItemCount());
    }
}
