/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import megamek.client.Client;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.event.GameListener;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/6/13 4:24 PM
 */
class AddBotUtilTest {

        private static final String HUMAN_PLAYER_NAME = "MockHuman";
        private static final String BOT_PLAYER_NAME = "MockBot";

        private Client mockClient;
        private Game mockGame;
        private Princess mockPrincess;
        private AddBotUtil testAddBotUtil;

        @BeforeEach
        void beforeEach() {
                final Player mockHumanPlayer = mock(Player.class);
                when(mockHumanPlayer.getName()).thenReturn(HUMAN_PLAYER_NAME);
                when(mockHumanPlayer.isGhost()).thenReturn(false);

                final Player mockBotPlayer = mock(Player.class);
                when(mockBotPlayer.getName()).thenReturn(BOT_PLAYER_NAME);
                when(mockBotPlayer.isGhost()).thenReturn(true);

                final Vector<Player> playerVector = new Vector<>(2);
                playerVector.add(mockHumanPlayer);
                playerVector.add(mockBotPlayer);

                mockGame = mock(Game.class);
                when(mockGame.getPlayersList()).thenReturn(playerVector);
                doNothing().when(mockGame).addGameListener(any(GameListener.class));

                mockClient = mock(Client.class);
                doNothing().when(mockClient).sendChat(anyString());
                when(mockClient.getGame()).thenReturn(mockGame);
                when(mockClient.getHost()).thenReturn("mockHost");
                when(mockClient.getPort()).thenReturn(1);

                mockPrincess = spy(new Princess("Princess", "mockHost", 1));
                doCallRealMethod().when(mockPrincess).setBehaviorSettings(any(BehaviorSettings.class));
                doReturn(mockGame).when(mockPrincess).getGame();
                doReturn(true).when(mockPrincess).connect();
                doReturn(new HashSet<Coords>()).when(mockPrincess).getStrategicBuildingTargets();
                doReturn(new HashSet<Integer>()).when(mockPrincess).getPriorityUnitTargets();
                doCallRealMethod().when(mockPrincess).getBehaviorSettings();

                testAddBotUtil = spy(new AddBotUtil());
                doReturn(mockPrincess).when(testAddBotUtil).makeNewPrincessClient(
                                any(Player.class), anyString(), anyInt());
        }

        @Test
        void testReplacePlayerWithABot() {
                // Test most basic version of command.
                final String actual = testAddBotUtil.addBot(new String[] { "/replacePlayer", BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: DEFAULT.\n", actual);
        }

        @Test
        void testExplicitlySpecifyingPrincess() {
                // Test explicitly specifying Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", BOT_PLAYER_NAME }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: DEFAULT.\n", actual);
        }

        @Test
        void testSpecifyingPrincessConfig() {
                // Test specifying the config to be used with Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-c:BERSERK", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: BERSERK.\n", actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("BERSERK"),
                                mockPrincess.getBehaviorSettings());
        }

        @Test
        void testSettingPrincessVerbosityLevel() {
                // Test setting the verbosity level for Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-p:" + BOT_PLAYER_NAME }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: DEFAULT.\n", actual);
        }

        @Test
        void testSettingPrincessConfig() {
                // Test setting both config and verbosity for Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-c:ESCAPE", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: ESCAPE.\n", actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("ESCAPE"),
                                mockPrincess.getBehaviorSettings());
        }

        @Test
        void testReplacingNonGhostPlayer() {
                // Test a non-ghost player.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", HUMAN_PLAYER_NAME }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("Player MockHuman is not a ghost.\n", actual);
        }

        @Test
        void testReplacingNonExistentPlayer() {
                // Test a non-existent player.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "invalid player" }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("No player with the name 'invalid player'.\n", actual);
        }

        @Test
        void testReplaceBotWithInvalidBotName() {
                // Test an invalid bot name.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:InvalidBot", BOT_PLAYER_NAME }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("Unrecognized bot: 'InvalidBot'.  Defaulting to Princess.\nPrincess has replaced MockBot.  Config: DEFAULT.\n",
                                actual);
        }

        @Test
        void testAddPrincessBotWithInvalidConfigName() {
                // Test an invalid config name for Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-c:invalid", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Unrecognized Behavior Setting: 'invalid'.  Using DEFAULT.\nPrincess has replaced MockBot.  Config: DEFAULT.\n",
                                actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("DEFAULT"),
                                mockPrincess.getBehaviorSettings());
        }

        @Test
        void testAddPrincessWithMissingDelimiter() {
                // Test leaving out a delimiter.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-c:ESCAPE", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: ESCAPE.\n", actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("ESCAPE"),
                                mockPrincess.getBehaviorSettings());
        }

        @Test
        void testAddPrincessWithOtherMissingDelimiter() {
                // Test leaving out a different delimiter.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "ESCAPE", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: DEFAULT.\n", actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("DEFAULT"),
                                mockPrincess.getBehaviorSettings());
        }
}
