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

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
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
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Deric
 * Date: 8/17/13
 * Time: 9:32 AM
 */
@RunWith(JUnit4.class)
public class ChatProcessorTest {

    private static BotClient mockBotHal;
    private static BotClient mockBotVGer;
    private static final IGame mockGame = Mockito.mock(IGame.class);
    private static final LogLevel logLevel = LogLevel.ERROR;

    private static IPlayer MOCK_BOT_PLAYER_HAL;
    private static IPlayer MOCK_BOT_PLAYER_V_GER;
    private static IPlayer MOCK_HUMAN_PLAYER_DAVE;
    private static IPlayer MOCK_HUMAN_PLAYER_KIRK;

    @Before
    public void setUp() throws Exception {
        MOCK_HUMAN_PLAYER_DAVE = Mockito.mock(IPlayer.class);
        MOCK_HUMAN_PLAYER_KIRK = Mockito.mock(IPlayer.class);
        MOCK_BOT_PLAYER_HAL = Mockito.mock(IPlayer.class);
        MOCK_BOT_PLAYER_V_GER = Mockito.mock(IPlayer.class);

        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.getName()).thenReturn("Dave");
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_BOT_PLAYER_HAL)).thenReturn(true);
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_BOT_PLAYER_V_GER)).thenReturn(false);
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_HUMAN_PLAYER_DAVE)).thenReturn(false);
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_HUMAN_PLAYER_KIRK)).thenReturn(true);
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.getTeam()).thenReturn(1);

        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.getName()).thenReturn("Kirk");
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.isEnemyOf(MOCK_BOT_PLAYER_HAL)).thenReturn(false);
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.isEnemyOf(MOCK_BOT_PLAYER_V_GER)).thenReturn(true);
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.isEnemyOf(MOCK_HUMAN_PLAYER_DAVE)).thenReturn(true);
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.isEnemyOf(MOCK_HUMAN_PLAYER_KIRK)).thenReturn(false);
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.getTeam()).thenReturn(2);

        Mockito.when(MOCK_BOT_PLAYER_HAL.getName()).thenReturn("Hal");
        Mockito.when(MOCK_BOT_PLAYER_HAL.isEnemyOf(MOCK_BOT_PLAYER_HAL)).thenReturn(false);
        Mockito.when(MOCK_BOT_PLAYER_HAL.isEnemyOf(MOCK_BOT_PLAYER_V_GER)).thenReturn(true);
        Mockito.when(MOCK_BOT_PLAYER_HAL.isEnemyOf(MOCK_HUMAN_PLAYER_DAVE)).thenReturn(true);
        Mockito.when(MOCK_BOT_PLAYER_HAL.isEnemyOf(MOCK_HUMAN_PLAYER_KIRK)).thenReturn(false);
        Mockito.when(MOCK_BOT_PLAYER_HAL.getTeam()).thenReturn(2);

        Mockito.when(MOCK_BOT_PLAYER_V_GER.getName()).thenReturn("V'Ger");
        Mockito.when(MOCK_BOT_PLAYER_V_GER.isEnemyOf(MOCK_BOT_PLAYER_HAL)).thenReturn(true);
        Mockito.when(MOCK_BOT_PLAYER_V_GER.isEnemyOf(MOCK_BOT_PLAYER_V_GER)).thenReturn(false);
        Mockito.when(MOCK_BOT_PLAYER_V_GER.isEnemyOf(MOCK_HUMAN_PLAYER_DAVE)).thenReturn(false);
        Mockito.when(MOCK_BOT_PLAYER_V_GER.isEnemyOf(MOCK_HUMAN_PLAYER_KIRK)).thenReturn(true);
        Mockito.when(MOCK_BOT_PLAYER_V_GER.getTeam()).thenReturn(1);

        Vector<IPlayer> PLAYER_VECTOR = new Vector<IPlayer>(4);
        PLAYER_VECTOR.add(MOCK_HUMAN_PLAYER_DAVE);
        PLAYER_VECTOR.add(MOCK_BOT_PLAYER_HAL);
        PLAYER_VECTOR.add(MOCK_HUMAN_PLAYER_KIRK);
        PLAYER_VECTOR.add(MOCK_BOT_PLAYER_V_GER);
        Mockito.when(mockGame.getPlayersVector()).thenReturn(PLAYER_VECTOR);

        mockBotHal = Mockito.mock(BotClient.class);
        Mockito.when(mockBotHal.getLocalPlayer()).thenReturn(MOCK_BOT_PLAYER_HAL );
        Mockito.when(mockBotHal.getGame()).thenReturn(mockGame);

        mockBotVGer = Mockito.mock(BotClient.class);
        Mockito.when(mockBotVGer.getLocalPlayer()).thenReturn(MOCK_BOT_PLAYER_V_GER);
        Mockito.when(mockBotVGer.getGame()).thenReturn(mockGame);
    }

    @Test
    public void testShouldBotAcknowledgeDefeat() throws Exception {
        ChatProcessor chatProcessor = new ChatProcessor();

        // Test individual human victory.
        String cmd = VictoryCommand.getDeclareIndividual(MOCK_HUMAN_PLAYER_DAVE.getName());
        String msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertTrue(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_BOT_PLAYER_HAL));
        Assert.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test team human victory.
        cmd = VictoryCommand.getDeclareTeam(MOCK_HUMAN_PLAYER_DAVE.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test a different message.
        msg = "This is general chat message with no bot commands.";
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test a null message.
        msg = null;
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test an empty message.
        msg = "";
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by human partner.
        cmd = VictoryCommand.getDeclareIndividual(MOCK_HUMAN_PLAYER_KIRK.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by self.
        cmd = VictoryCommand.getDeclareIndividual(MOCK_BOT_PLAYER_HAL.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by opposing bot.
        cmd = VictoryCommand.getDeclareIndividual(MOCK_BOT_PLAYER_V_GER.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));
    }

    @Test
    public void testShouldBotAcknowledgeVictory() {
        ChatProcessor chatProcessor = new ChatProcessor();

        // Test enemy wants defeat.
        String cmd = DefeatCommand.getWantsDefeat(MOCK_HUMAN_PLAYER_DAVE.getName());
        String msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertTrue(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test enemy admits defeat.
        cmd = DefeatCommand.getAdmitsDefeat(MOCK_HUMAN_PLAYER_DAVE.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test ally wants defeat.
        cmd = DefeatCommand.getWantsDefeat(MOCK_HUMAN_PLAYER_KIRK.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test ally admits defeat.
        cmd = DefeatCommand.getAdmitsDefeat(MOCK_HUMAN_PLAYER_KIRK.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test a different message.
        msg = "This is general chat message with no bot commands.";
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test null message.
        msg = null;
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test empty message.
        msg = "";
        Assert.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));
    }

    @Test
    public void testAdditionalPrincessCommands() {
        ChatProcessor testChatProcessor = new ChatProcessor();

        // Test the 'flee' command sent by a teammate.
        GamePlayerChatEvent mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        String chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                             Princess.CMD_FLEE;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        Princess mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertTrue(mockPrincess.shouldFlee());

        // Test the 'flee' command sent by the enemy.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage =MOCK_HUMAN_PLAYER_KIRK.getName() + ": " +  MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                     Princess.CMD_FLEE;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_KIRK);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertFalse(mockPrincess.shouldFlee());


        // Test the 'flee' command sent to a different bot player.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " +  MOCK_BOT_PLAYER_HAL.getName() + ": " +
                      Princess.CMD_FLEE;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertFalse(mockPrincess.shouldFlee());

        // Test the 'verbose' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_VERBOSE + " : " + LogLevel.INFO.toString();
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(LogLevel.INFO, mockPrincess.getVerbosity());

        // Test the 'verbose' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_VERBOSE;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(logLevel, mockPrincess.getVerbosity());

        // Test the 'verbose' command with an invalid log level.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_VERBOSE + " : blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(logLevel, mockPrincess.getVerbosity());

        // Test a good 'verbose' command with extra data after log level argument.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_VERBOSE + " : " + LogLevel.INFO.toString()
                      + " blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(LogLevel.INFO, mockPrincess.getVerbosity());

        // Test the 'behavior' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_BEHAVIOR + " : " + BehaviorSettingsFactory.COWARDLY_BEHAVIOR_DESCRIPTION;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(BehaviorSettingsFactory.getInstance().COWARDLY_BEHAVIOR,
                            mockPrincess.getBehaviorSettings());

        // Test the 'behavior' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_BEHAVIOR;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR,
                            mockPrincess.getBehaviorSettings());

        // Test the 'behavior' command with an invalid behavior name
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_BEHAVIOR + " : blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR,
                            mockPrincess.getBehaviorSettings());

        // Test the 'caution' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_CAUTION + " : +++";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(8, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'caution' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_CAUTION;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'caution' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_CAUTION + " : +4";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(6, mockPrincess.getBehaviorSettings().getFallShameIndex());

        // Test the 'avoid' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_AVOID + " : --";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(3, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'avoid' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_AVOID;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'avoid' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_AVOID + " : +5";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(6, mockPrincess.getBehaviorSettings().getSelfPreservationIndex());

        // Test the 'aggression' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_AGGRESSION + " : ++";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(7, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'aggression' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_AGGRESSION;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'aggression' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_AGGRESSION + " : blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getHyperAggressionIndex());

        // Test the 'herding' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_HERDING + " : -";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(4, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'herding' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_HERDING;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'herding' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_HERDING + " : -4";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(4, mockPrincess.getBehaviorSettings().getHerdMentalityIndex());

        // Test the 'brave' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_BRAVERY + " : +++";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(8, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'brave' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_BRAVERY;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(5, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'brave' command with invalid arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_BRAVERY + " : -2";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Assert.assertEquals(4, mockPrincess.getBehaviorSettings().getBraveryIndex());

        // Test the 'target' command.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_TARGET + " : 1234";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        Set<String> expected = new HashSet(1);
        expected.add("1234");
        Assert.assertEquals(expected, mockPrincess.getBehaviorSettings().getStrategicTargets());

        // Test the 'target' command with no arguments.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_TARGET;
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet(0);
        Assert.assertEquals(expected, mockPrincess.getBehaviorSettings().getStrategicTargets());

        // Test the 'target' command with an invalid hex number.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_TARGET + " : blah";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet(0);
        Assert.assertEquals(expected, mockPrincess.getBehaviorSettings().getStrategicTargets());

        // Test the 'target' command with a too large hex number.
        mockChatEvent = Mockito.mock(GamePlayerChatEvent.class);
        chatMessage = MOCK_HUMAN_PLAYER_DAVE.getName() + ": " + MOCK_BOT_PLAYER_V_GER.getName() + ": " +
                      Princess.CMD_TARGET + " : 12345";
        Mockito.when(mockChatEvent.getMessage()).thenReturn(chatMessage);
        Mockito.when(mockChatEvent.getPlayer()).thenReturn(MOCK_HUMAN_PLAYER_DAVE);
        mockPrincess = Mockito.spy(new Princess(MOCK_BOT_PLAYER_V_GER.getName(), "test", 1, logLevel));
        mockPrincess.setBehaviorSettings(BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.doReturn(MOCK_BOT_PLAYER_V_GER).when(mockPrincess).getLocalPlayer();
        Mockito.doNothing().when(mockPrincess).sendChat(Mockito.anyString());
        testChatProcessor.additionalPrincessCommands(mockChatEvent, mockPrincess);
        expected = new HashSet(0);
        Assert.assertEquals(expected, mockPrincess.getBehaviorSettings().getStrategicTargets());
    }
}