package megamek.common.util;

import megamek.client.Client;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.event.GameListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/6/13 4:24 PM
 */
public class AddBotUtilTest {

        private static final String HUMAN_PLAYER_NAME = "MockHuman";
        private static final String BOT_PLAYER_NAME = "MockBot";

        private Client mockClient;
        private Game mockGame;
        private Princess mockPrincess;
        private AddBotUtil testAddBotUtil;

        @BeforeEach
        public void beforeEach() {
                final Player mockHumanPlayer = mock(Player.class);
                when(mockHumanPlayer.getName()).thenReturn(HUMAN_PLAYER_NAME);
                when(mockHumanPlayer.isGhost()).thenReturn(false);

                final Player mockBotPlayer = mock(Player.class);
                when(mockBotPlayer.getName()).thenReturn(BOT_PLAYER_NAME);
                when(mockBotPlayer.isGhost()).thenReturn(true);

                final Vector<Player> playerVector = new Vector<>(2);
                playerVector.add(mockHumanPlayer);
                playerVector.add(mockBotPlayer);

                final Enumeration<Player> playerEnumeration = playerVector.elements();

                mockGame = mock(Game.class);
                when(mockGame.getPlayersVector()).thenReturn(playerVector);
                when(mockGame.getPlayers()).thenReturn(playerEnumeration);
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
        public void testReplacePlayerWithABot() {
                // Test most basic version of command.
                final String actual = testAddBotUtil.addBot(new String[] { "/replacePlayer", BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: DEFAULT.\n", actual);
        }

        @Test
        public void testExplicitlySpecifyingPrincess() {
                // Test explicitly specifying Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", BOT_PLAYER_NAME }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: DEFAULT.\n", actual);
        }

        @Test
        public void testSpecifyingPrincessConfig() {
                // Test specifying the config to be used with Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-c:BERSERK", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: BERSERK.\n", actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("BERSERK"),
                                mockPrincess.getBehaviorSettings());
        }

        @Test
        public void testSettingPrincessVerbosityLevel() {
                // Test setting the verbosity level for Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-p:" + BOT_PLAYER_NAME }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: DEFAULT.\n", actual);
        }

        @Test
        public void testSettingPrincessConfig() {
                // Test setting both config and verbosity for Princess.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-c:ESCAPE", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: ESCAPE.\n", actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("ESCAPE"),
                                mockPrincess.getBehaviorSettings());
        }

        @Test
        public void testReplacingNonGhostPlayer() {
                // Test a non-ghost player.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", HUMAN_PLAYER_NAME }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("Player MockHuman is not a ghost.\n", actual);
        }

        @Test
        public void testReplacingNonExistentPlayer() {
                // Test a non-existent player.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "invalid player" }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("No player with the name 'invalid player'.\n", actual);
        }

        @Test
        public void testReplaceBotWithInvalidBotName() {
                // Test an invalid bot name.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:InvalidBot", BOT_PLAYER_NAME }, mockGame,
                                mockClient.getHost(), mockClient.getPort());
                assertEquals("Unrecognized bot: 'InvalidBot'.  Defaulting to Princess.\nPrincess has replaced MockBot.  Config: DEFAULT.\n",
                                actual);
        }

        @Test
        public void testAddPrincessBotWithInvalidConfigName() {
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
        public void testAddPrincessWithMissingDelimiter() {
                // Test leaving out a delimiter.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "-c:ESCAPE", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: ESCAPE.\n", actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("ESCAPE"),
                                mockPrincess.getBehaviorSettings());
        }

        @Test
        public void testAddPrincessWithOtherMissingDelimiter() {
                // Test leaving out a different delimiter.
                final String actual = testAddBotUtil.addBot(
                                new String[] { "/replacePlayer", "-b:Princess", "ESCAPE", "-p:" + BOT_PLAYER_NAME },
                                mockGame, mockClient.getHost(), mockClient.getPort());
                assertEquals("Princess has replaced MockBot.  Config: DEFAULT.\n", actual);
                assertEquals(BehaviorSettingsFactory.getInstance().getBehavior("DEFAULT"),
                                mockPrincess.getBehaviorSettings());
        }
}
