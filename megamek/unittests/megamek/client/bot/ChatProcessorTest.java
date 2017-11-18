/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.bot.princess.Princess;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.logging.LogLevel;
import megamek.server.Server;
import megamek.server.commands.DefeatCommand;
import megamek.server.commands.VictoryCommand;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Created with IntelliJ IDEA.
 * User: Deric "Netzilla" Page (deric dot page at usa dot net)
 * Date: 8/17/13
 * Time: 9:32 AM
 */
@RunWith(JUnit4.class)
public class ChatProcessorTest {

    private static BotClient mockBotHal;
    private static final IGame MOCK_GAME = Mockito.mock(IGame.class);
    private static final LogLevel LOG_LEVEL = LogLevel.ERROR;

    private static IPlayer mockBotPlayerHal;
    private static IPlayer mockBotPlayerVGer;
    private static IPlayer mockHumanPlayerDave;
    private static IPlayer mockHumanPlayerKirk;

    @Before
    public void setUp() throws Exception {
        mockHumanPlayerDave = Mockito.mock(IPlayer.class);
        mockHumanPlayerKirk = Mockito.mock(IPlayer.class);
        mockBotPlayerHal = Mockito.mock(IPlayer.class);
        mockBotPlayerVGer = Mockito.mock(IPlayer.class);

        Mockito.when(mockHumanPlayerDave.getName()).thenReturn("Dave");
        Mockito.when(mockHumanPlayerDave.isEnemyOf(mockBotPlayerHal)).thenReturn(true);
        Mockito.when(mockHumanPlayerDave.isEnemyOf(mockBotPlayerVGer)).thenReturn(false);
        Mockito.when(mockHumanPlayerDave.isEnemyOf(mockHumanPlayerDave)).thenReturn(false);
        Mockito.when(mockHumanPlayerDave.isEnemyOf(mockHumanPlayerKirk)).thenReturn(true);
        Mockito.when(mockHumanPlayerDave.getTeam()).thenReturn(1);

        Mockito.when(mockHumanPlayerKirk.getName()).thenReturn("Kirk");
        Mockito.when(mockHumanPlayerKirk.isEnemyOf(mockBotPlayerHal)).thenReturn(false);
        Mockito.when(mockHumanPlayerKirk.isEnemyOf(mockBotPlayerVGer)).thenReturn(true);
        Mockito.when(mockHumanPlayerKirk.isEnemyOf(mockHumanPlayerDave)).thenReturn(true);
        Mockito.when(mockHumanPlayerKirk.isEnemyOf(mockHumanPlayerKirk)).thenReturn(false);
        Mockito.when(mockHumanPlayerKirk.getTeam()).thenReturn(2);

        Mockito.when(mockBotPlayerHal.getName()).thenReturn("Hal");
        Mockito.when(mockBotPlayerHal.isEnemyOf(mockBotPlayerHal)).thenReturn(false);
        Mockito.when(mockBotPlayerHal.isEnemyOf(mockBotPlayerVGer)).thenReturn(true);
        Mockito.when(mockBotPlayerHal.isEnemyOf(mockHumanPlayerDave)).thenReturn(true);
        Mockito.when(mockBotPlayerHal.isEnemyOf(mockHumanPlayerKirk)).thenReturn(false);
        Mockito.when(mockBotPlayerHal.getTeam()).thenReturn(2);

        Mockito.when(mockBotPlayerVGer.getName()).thenReturn("V'Ger");
        Mockito.when(mockBotPlayerVGer.isEnemyOf(mockBotPlayerHal)).thenReturn(true);
        Mockito.when(mockBotPlayerVGer.isEnemyOf(mockBotPlayerVGer)).thenReturn(false);
        Mockito.when(mockBotPlayerVGer.isEnemyOf(mockHumanPlayerDave)).thenReturn(false);
        Mockito.when(mockBotPlayerVGer.isEnemyOf(mockHumanPlayerKirk)).thenReturn(true);
        Mockito.when(mockBotPlayerVGer.getTeam()).thenReturn(1);

        final Vector<IPlayer> playerVector = new Vector<>(4);
        playerVector.add(mockHumanPlayerDave);
        playerVector.add(mockBotPlayerHal);
        playerVector.add(mockHumanPlayerKirk);
        playerVector.add(mockBotPlayerVGer);
        Mockito.when(MOCK_GAME.getPlayersVector()).thenReturn(playerVector);

        mockBotHal = Mockito.mock(BotClient.class);
        Mockito.when(mockBotHal.getLocalPlayer()).thenReturn(mockBotPlayerHal);
        Mockito.when(mockBotHal.getGame()).thenReturn(MOCK_GAME);

        final BotClient mockBotVGer = Mockito.mock(BotClient.class);
        Mockito.when(mockBotVGer.getLocalPlayer()).thenReturn(mockBotPlayerVGer);
        Mockito.when(mockBotVGer.getGame()).thenReturn(MOCK_GAME);
    }

    @Test
    public void testShouldBotAcknowledgeDefeat() throws Exception {
        final ChatProcessor chatProcessor = new ChatProcessor();

        // Test individual human victory.
        String cmd = VictoryCommand.getDeclareIndividual(mockHumanPlayerDave.getName());
        String msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertTrue(mockHumanPlayerDave.isEnemyOf(mockBotPlayerHal));
        Assert.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test team human victory.
        cmd = VictoryCommand.getDeclareTeam(mockHumanPlayerDave.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test a different message.
        msg = "This is general chat message with no bot commands.";
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test a null message.
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(null, mockBotHal));

        // Test an empty message.
        msg = "";
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by human partner.
        cmd = VictoryCommand.getDeclareIndividual(mockHumanPlayerKirk.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by self.
        cmd = VictoryCommand.getDeclareIndividual(mockBotPlayerHal.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by opposing bot.
        cmd = VictoryCommand.getDeclareIndividual(mockBotPlayerVGer.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));
    }

    @Test
    public void testShouldBotAcknowledgeVictory() {
        final ChatProcessor chatProcessor = new ChatProcessor();

        // Test enemy wants defeat.
        String cmd = DefeatCommand.getWantsDefeat(mockHumanPlayerDave.getName());
        String msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertTrue(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test enemy admits defeat.
        cmd = DefeatCommand.getAdmitsDefeat(mockHumanPlayerDave.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test ally wants defeat.
        cmd = DefeatCommand.getWantsDefeat(mockHumanPlayerKirk.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test ally admits defeat.
        cmd = DefeatCommand.getAdmitsDefeat(mockHumanPlayerKirk.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test a different message.
        msg = "This is general chat message with no bot commands.";
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test null message.
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(null, mockBotHal));

        // Test empty message.
        msg = "";
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));
    }

    @Test
    public void testAdditionalPrincessCommands() {
        final ChatProcessor testChatProcessor = new ChatProcessor();

        final IBoard mockBoard = Mockito.mock(IBoard.class);
        Mockito.when(mockBoard.contains(Mockito.any(Coords.class))).thenReturn(true);
        Mockito.when(MOCK_GAME.getBoard()).thenReturn(mockBoard);

        // Test the 'flee' command sent by a teammate.
        GamePlayerChatEvent mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        String chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                             ChatCommands.FLEE.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        Princess mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertTrue(mockPrincess.getFallBack());

        // Test the 'flee' command sent by the enemy.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerKirk.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.FLEE.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerKirk);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertFalse(mockPrincess.getFallBack());


        // Test the 'flee' command sent to a different bot player.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerHal.getName() + ": " +
                      ChatCommands.FLEE.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertFalse(mockPrincess.getFallBack());

        // Test the 'verbose' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.VERBOSE.getAbbreviation() + " : " + LogLevel.INFO;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(LogLevel.INFO, mockPrincess.getVerbosity());

        // Test the 'verbose' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.VERBOSE.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(LogLevel.WARNING, mockPrincess.getVerbosity());

        // Test the 'verbose' command with an invalid log level.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.VERBOSE.getAbbreviation() + " : blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(LogLevel.WARNING, mockPrincess.getVerbosity());

        // Test a good 'verbose' command with extra data after log level argument.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.VERBOSE.getAbbreviation() + " : " + LogLevel.INFO
                      + " blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(LogLevel.INFO, mockPrincess.getVerbosity());

        // Test the 'behavior' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.BEHAVIOR.getAbbreviation() + " : " +
                      BehaviorSettingsFactory.COWARDLY_BEHAVIOR_DESCRIPTION;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(BehaviorSettingsFactory.getInstance().COWARDLY_BEHAVIOR,
                            mockPrincess.getBehaviorSettings());

        // Test the 'behavior' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.BEHAVIOR.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR,
                            mockPrincess.getBehaviorSettings());

        // Test the 'behavior' command with an invalid behavior name
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.BEHAVIOR.getAbbreviation() + " : blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR,
                            mockPrincess.getBehaviorSettings());

        // Test the 'caution' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.CAUTION.getAbbreviation() + " : +++";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(8, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'caution' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.CAUTION.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'caution' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.CAUTION.getAbbreviation() + " : +4";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(6, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'avoid' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.AVOID.getAbbreviation() + " : --";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(3, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'avoid' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.AVOID.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'avoid' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.AVOID.getAbbreviation() + " : +5";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(6, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'aggression' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.AGGRESSION.getAbbreviation() + " : ++";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(7, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'aggression' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.AGGRESSION.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'aggression' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.AGGRESSION.getAbbreviation() + " : blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'herding' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.HERDING.getAbbreviation() + " : -";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(4, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'herding' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.HERDING.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'herding' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.HERDING.getAbbreviation() + " : -4";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(4, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'brave' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.BRAVERY.getAbbreviation() + " : +++";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(8, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'brave' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.BRAVERY.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'brave' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.BRAVERY.getAbbreviation() + " : -2";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(4, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'target' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.TARGET.getAbbreviation() + " : 1234";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Set<Coords> expected = new HashSet<>(1);
        expected.add(new Coords(11, 33));
        Assert.assertEquals(expected, mockPrincess.getStrategicBuildingTargets());

        // Test the 'target' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.TARGET.getAbbreviation();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet<>(0);
        Assert.assertEquals(expected, mockPrincess.getStrategicBuildingTargets());

        // Test the 'target' command with an invalid hex number.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.TARGET.getAbbreviation() + " : blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet<>(0);
        Assert.assertEquals(expected, mockPrincess.getStrategicBuildingTargets());

        // Test the 'priority' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.PRIORITIZE.getAbbreviation() + " : 12";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        final Set<Integer> expectedUnits = new HashSet<>(1);
        expectedUnits.add(12);
        Assert.assertEquals(expectedUnits, mockPrincess.getBehaviorSettings().getPriorityUnitTargets());

        // Test the 'target' command with a too large hex number.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = mockHumanPlayerDave.getName() + ": " + mockBotPlayerVGer.getName() + ": " +
                      ChatCommands.TARGET.getAbbreviation() + " : 12345";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(mockHumanPlayerDave);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(),
                                                   Matchers.any(LogLevel.class), Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet<>(0);
        Assert.assertEquals(expected, mockPrincess.getStrategicBuildingTargets());

        // Test a chat message not directed at Princess.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = "MovementDisplay: tried to select non-existant entity: -1";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(null);
        mockPrincess = Mockito.spy(new Princess(mockBotPlayerVGer.getName(), "test", 1, LOG_LEVEL));
        Mockito.doReturn(MOCK_GAME).when(mockPrincess).getGame();
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final String msg = (String) invocationOnMock.getArguments()[3];
                if ("speakerPlayer is NULL.".equalsIgnoreCase(msg)) {
                    Assert.fail("Should not reach this point.");
                }
                return null;
            }
        }).when(mockPrincess).log(Matchers.any(Class.class), Matchers.anyString(), Matchers.any(LogLevel.class),
                                  Matchers.anyString());
        Mockito.doReturn(mockBotPlayerVGer).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Matchers.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet<>(0);
        Assert.assertEquals(expected, mockPrincess.getStrategicBuildingTargets());
    }
}
