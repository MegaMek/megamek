/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.bot.princess.Princess;
import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.event.GamePlayerChatEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * User: Deric "Netzilla" Page (deric dot page at usa dot net)
 * Date: 8/17/13
 * Time: 9:32 AM
 */
public class ChatProcessorTest {

    private static BotClient mockBotHal;
    private static final Game MOCK_GAME = mock(Game.class);

    private static Player mockBotPlayerHal;
    private static Player mockBotPlayerVGer;
    private static Player mockHumanPlayerDave;
    private static Player mockHumanPlayerKirk;

    @BeforeAll
    public static void beforeAll() {
        mockHumanPlayerDave = mock(Player.class);
        mockHumanPlayerKirk = mock(Player.class);
        mockBotPlayerHal = mock(Player.class);
        mockBotPlayerVGer = mock(Player.class);

        when(mockHumanPlayerDave.getName()).thenReturn("Dave");
        when(mockHumanPlayerDave.isEnemyOf(mockBotPlayerHal)).thenReturn(true);
        when(mockHumanPlayerDave.isEnemyOf(mockBotPlayerVGer)).thenReturn(false);
        when(mockHumanPlayerDave.isEnemyOf(mockHumanPlayerDave)).thenReturn(false);
        when(mockHumanPlayerDave.isEnemyOf(mockHumanPlayerKirk)).thenReturn(true);
        when(mockHumanPlayerDave.getTeam()).thenReturn(1);

        when(mockHumanPlayerKirk.getName()).thenReturn("Kirk");
        when(mockHumanPlayerKirk.isEnemyOf(mockBotPlayerHal)).thenReturn(false);
        when(mockHumanPlayerKirk.isEnemyOf(mockBotPlayerVGer)).thenReturn(true);
        when(mockHumanPlayerKirk.isEnemyOf(mockHumanPlayerDave)).thenReturn(true);
        when(mockHumanPlayerKirk.isEnemyOf(mockHumanPlayerKirk)).thenReturn(false);
        when(mockHumanPlayerKirk.getTeam()).thenReturn(2);

        when(mockBotPlayerHal.getName()).thenReturn("Hal");
        when(mockBotPlayerHal.isEnemyOf(mockBotPlayerHal)).thenReturn(false);
        when(mockBotPlayerHal.isEnemyOf(mockBotPlayerVGer)).thenReturn(true);
        when(mockBotPlayerHal.isEnemyOf(mockHumanPlayerDave)).thenReturn(true);
        when(mockBotPlayerHal.isEnemyOf(mockHumanPlayerKirk)).thenReturn(false);
        when(mockBotPlayerHal.getTeam()).thenReturn(2);

        when(mockBotPlayerVGer.getName()).thenReturn("V'Ger");
        when(mockBotPlayerVGer.isEnemyOf(mockBotPlayerHal)).thenReturn(true);
        when(mockBotPlayerVGer.isEnemyOf(mockBotPlayerVGer)).thenReturn(false);
        when(mockBotPlayerVGer.isEnemyOf(mockHumanPlayerDave)).thenReturn(false);
        when(mockBotPlayerVGer.isEnemyOf(mockHumanPlayerKirk)).thenReturn(true);
        when(mockBotPlayerVGer.getTeam()).thenReturn(1);

        final Vector<Player> playerVector = new Vector<>(4);
        playerVector.add(mockHumanPlayerDave);
        playerVector.add(mockBotPlayerHal);
        playerVector.add(mockHumanPlayerKirk);
        playerVector.add(mockBotPlayerVGer);
        when(MOCK_GAME.getPlayersVector()).thenReturn(playerVector);

        mockBotHal = mock(BotClient.class);
        when(mockBotHal.getLocalPlayer()).thenReturn(mockBotPlayerHal);
        when(mockBotHal.getGame()).thenReturn(MOCK_GAME);

        final BotClient mockBotVGer = mock(BotClient.class);
        when(mockBotVGer.getLocalPlayer()).thenReturn(mockBotPlayerVGer);
        when(mockBotVGer.getGame()).thenReturn(MOCK_GAME);
    }

    @Test
    public void testAdditionalPrincessCommands() {
        final ChatProcessor testChatProcessor = new ChatProcessor();

        final Board mockBoard = mock(Board.class);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);
        when(MOCK_GAME.getBoard()).thenReturn(mockBoard);

        // Test the 'flee' command sent by a teammate.
        GamePlayerChatEvent mockChatEvent = mock(GamePlayerChatEvent.class);
        String chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName()
                + ": " + ChatCommands.FLEE.getAbbreviation() + ": " + CardinalEdge.NORTH.getIndex();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        Princess mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertTrue(mockPrincess.getFallBack());

        // Test the 'flee' command sent by the enemy.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerKirk.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.FLEE.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerKirk);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertFalse(mockPrincess.getFallBack());

        // Test the 'flee' command sent to a different bot player.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerHal.getName() + ": "
                + ChatCommands.FLEE.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertFalse(mockPrincess.getFallBack());

        // Test the 'verbose' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.VERBOSE.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);

        // Test the 'verbose' command with no arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.VERBOSE.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);

        // Test the 'verbose' command with an invalid log level.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.VERBOSE.getAbbreviation() + " : blah";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);

        // Test a good 'verbose' command with extra data after log level argument.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName()
                + ": " + ChatCommands.VERBOSE.getAbbreviation() + " blah";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);

        // Test the 'behavior' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.BEHAVIOR.getAbbreviation() + " : "
                + BehaviorSettingsFactory.COWARDLY_BEHAVIOR_DESCRIPTION;
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(BehaviorSettingsFactory.getInstance().COWARDLY_BEHAVIOR,
                mockPrincess.getBehaviorSettings());

        // Test the 'behavior' command with no arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.BEHAVIOR.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR,
                mockPrincess.getBehaviorSettings());

        // Test the 'behavior' command with an invalid behavior name
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.BEHAVIOR.getAbbreviation() + " : blah";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR,
                mockPrincess.getBehaviorSettings());

        // Test the 'caution' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.CAUTION.getAbbreviation() + " : +++";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(8, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'caution' command with no arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.CAUTION.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(5, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'caution' command with invalid arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.CAUTION.getAbbreviation() + " : +4";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(6, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'avoid' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.AVOID.getAbbreviation() + " : --";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(3, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'avoid' command with no arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.AVOID.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(5, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'avoid' command with invalid arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.AVOID.getAbbreviation() + " : +5";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(6, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'aggression' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.AGGRESSION.getAbbreviation() + " : ++";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(7, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'aggression' command with no arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.AGGRESSION.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(5, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'aggression' command with invalid arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.AGGRESSION.getAbbreviation() + " : blah";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(5, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'herding' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.HERDING.getAbbreviation() + " : -";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(4, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'herding' command with no arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.HERDING.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(5, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'herding' command with invalid arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.HERDING.getAbbreviation() + " : -4";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(4, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'brave' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.BRAVERY.getAbbreviation() + " : +++";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(8, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'brave' command with no arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.BRAVERY.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(5, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'brave' command with invalid arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.BRAVERY.getAbbreviation() + " : -2";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(4, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'target' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.TARGET.getAbbreviation() + " : 1234";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Set<Coords> expected = new HashSet<>(1);
        expected.add(new Coords(11, 33));
        assertEquals(expected, mockPrincess.getStrategicBuildingTargets());

        // Test the 'target' command with no arguments.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.TARGET.getAbbreviation();
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet<>(0);
        assertEquals(expected, mockPrincess.getStrategicBuildingTargets());

        // Test the 'target' command with an invalid hex number.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.TARGET.getAbbreviation() + " : blah";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet<>(0);
        assertEquals(expected, mockPrincess.getStrategicBuildingTargets());

        // Test the 'priority' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.PRIORITIZE.getAbbreviation() + " : 12";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        final Set<Integer> expectedUnits = new HashSet<>(1);
        expectedUnits.add(12);
        assertEquals(expectedUnits, mockPrincess.getBehaviorSettings().getPriorityUnitTargets());

        // Test the 'target' command with a too large hex number.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
                + ChatCommands.TARGET.getAbbreviation() + " : 12345";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet<>(0);
        assertEquals(expected, mockPrincess.getStrategicBuildingTargets());

        // Test a chat message not directed at Princess.
        mockChatEvent = mock(GamePlayerChatEvent.class);
        chatMessage = "MovementDisplay: tried to select non-existant entity: -1";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(null);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet<>(0);
        assertEquals(expected, mockPrincess.getStrategicBuildingTargets());
    }
}
