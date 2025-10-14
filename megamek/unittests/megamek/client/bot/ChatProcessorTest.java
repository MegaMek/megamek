/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.bot.princess.Princess;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

/**
 * User: Deric "Netzilla" Page (deric dot page at usa dot net) Date: 8/17/13 Time: 9:32 AM
 */
class ChatProcessorTest {

    private static final Game MOCK_GAME = mock(Game.class);

    private static Player mockBotPlayerHal;
    private static Player mockBotPlayerVGer;
    private static Player mockHumanPlayerDave;
    private static Player mockHumanPlayerKirk;

    @BeforeAll
    static void beforeAll() {
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
        when(MOCK_GAME.getPlayersList()).thenReturn(playerVector);

        BotClient mockBotHal = mock(BotClient.class);
        when(mockBotHal.getLocalPlayer()).thenReturn(mockBotPlayerHal);
        when(mockBotHal.getGame()).thenReturn(MOCK_GAME);

        final BotClient mockBotVGer = mock(BotClient.class);
        when(mockBotVGer.getLocalPlayer()).thenReturn(mockBotPlayerVGer);
        when(mockBotVGer.getGame()).thenReturn(MOCK_GAME);
    }

    @Test
    void testAdditionalPrincessCommands() {
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
              + ChatCommands.CAUTION.getAbbreviation() + " : +";
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

        // Test the 'avoid' command.
        mockChatEvent = mock(GamePlayerChatEvent.class);

        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": "
              + ChatCommands.AVOID.getAbbreviation() + " : 2";
        when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = spy(new Princess(mockBotPlayerVGer.getName(), "test", 1));
        doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        doNothing().when(mockPrincess).sendChat(ArgumentMatchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        assertEquals(2, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

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
              + ChatCommands.AVOID.getAbbreviation() + " : 6";
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
              + ChatCommands.HERDING.getAbbreviation() + " : -a";
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
              + ChatCommands.BRAVERY.getAbbreviation() + " : -p";
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
        chatMessage = "MovementDisplay: tried to select non-existent entity: -1";
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
