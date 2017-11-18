package megamek.common.util;

import junit.framework.TestCase;
import megamek.client.Client;
import megamek.client.bot.TestBot;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.event.GameListener;
import megamek.common.logging.LogLevel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 *
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/6/13 4:24 PM
 */
@RunWith(JUnit4.class)
public class AddBotUtilTest {

    private static final String HUMAN_PLAYER_NAME = "MockHuman";
    private static final String BOT_PLAYER_NAME = "MockBot";
    private static final String DEFAULT_VERBOSITY = LogLevel.WARNING.toString();

    private Client mockClient;
    private IGame mockGame;
    private Princess mockPrincess;
    private AddBotUtil testAddBotUtil;

    @Before
    public void setUp() {
        final IPlayer mockHumanPlayer = Mockito.mock(IPlayer.class);
        Mockito.when(mockHumanPlayer.getName()).thenReturn(HUMAN_PLAYER_NAME);
        Mockito.when(mockHumanPlayer.isGhost()).thenReturn(false);

        final IPlayer mockBotPlayer = Mockito.mock(IPlayer.class);
        Mockito.when(mockBotPlayer.getName()).thenReturn(BOT_PLAYER_NAME);
        Mockito.when(mockBotPlayer.isGhost()).thenReturn(true);

        final Vector<IPlayer> playerVector = new Vector<>(2);
        playerVector.add(mockHumanPlayer);
        playerVector.add(mockBotPlayer);

        final Enumeration<IPlayer> playerEnumeration = playerVector.elements();

        mockGame = Mockito.mock(IGame.class);
        Mockito.when(mockGame.getPlayersVector()).thenReturn(playerVector);
        Mockito.when(mockGame.getPlayers()).thenReturn(playerEnumeration);
        Mockito.doNothing().when(mockGame).addGameListener(Mockito.any(GameListener.class));

        mockClient = Mockito.mock(Client.class);
        Mockito.doNothing().when(mockClient).sendChat(Mockito.anyString());
        Mockito.when(mockClient.getGame()).thenReturn(mockGame);
        Mockito.when(mockClient.getHost()).thenReturn("mockHost");
        Mockito.when(mockClient.getPort()).thenReturn(1);

        mockPrincess = Mockito.spy(new Princess("Princess", "mockHost", 1, LogLevel.ERROR));
        Mockito.doCallRealMethod().when(mockPrincess).setBehaviorSettings(Mockito.any(BehaviorSettings.class));
        Mockito.doReturn(mockGame).when(mockPrincess).getGame();
        Mockito.doReturn(true).when(mockPrincess).connect();
        Mockito.doReturn(new HashSet<Coords>()).when(mockPrincess).getStrategicBuildingTargets();
        Mockito.doReturn(new HashSet<Integer>()).when(mockPrincess).getPriorityUnitTargets();
        Mockito.doCallRealMethod().when(mockPrincess).getBehaviorSettings();
        Mockito.doCallRealMethod().when(mockPrincess).getVerbosity();

        final TestBot mockTestBot = Mockito.mock(TestBot.class);
        Mockito.when(mockTestBot.connect()).thenReturn(true);
        Mockito.when(mockTestBot.getGame()).thenReturn(mockGame);

        testAddBotUtil = Mockito.spy(new AddBotUtil());
        Mockito.doReturn(mockPrincess).when(testAddBotUtil).makeNewPrincessClient(Mockito.any(IPlayer.class),
                                                                                  Mockito.any(LogLevel.class),
                                                                                  Mockito.anyString(),
                                                                                  Mockito.anyInt());
        Mockito.doReturn(mockTestBot).when(testAddBotUtil).makeNewTestBotClient(Mockito.any(IPlayer.class),
                                                                                Mockito.anyString(),
                                                                                Mockito.anyInt());
    }

    @Test
    public void testRun() {

        // Test most basic version of command.
        String[] args = { "/replacePlayer", BOT_PLAYER_NAME };
        String expected = "TestBot has replaced MockBot.\n";
        String actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);

        // Test explicitly specifying TestBot.
        setUp();
        args = new String[]{"/replacePlayer", "-b:TestBot", BOT_PLAYER_NAME};
        expected = "TestBot has replaced MockBot.\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);

        // Test explicitly specifying Princess.
        setUp();
        args = new String[]{"/replacePlayer", "-b:Princess", BOT_PLAYER_NAME};
        expected = "Princess has replaced MockBot.  Config: DEFAULT.  Verbosity: " + DEFAULT_VERBOSITY + ".\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);

        // Test specifying the config to be used with Princess.
        setUp();
        args = new String[]{"/replacePlayer", "-b:Princess", "-c:BERSERK", "-p:" + BOT_PLAYER_NAME};
        expected = "Princess has replaced MockBot.  Config: BERSERK.  Verbosity: " + DEFAULT_VERBOSITY + ".\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);
        BehaviorSettings expectedBehavior = BehaviorSettingsFactory.getInstance().getBehavior("BERSERK");
        TestCase.assertEquals(expectedBehavior, mockPrincess.getBehaviorSettings());

        // Test setting the verbosity level for Princess.
        // Because makeNewPrincessClient is mocked out, the log level is always going to be ERROR.
        setUp();
        args = new String[] { "/replacePlayer", "-b:Princess", "-v:" + LogLevel.INFO, "-p:" + BOT_PLAYER_NAME };
        expected = "Verbosity set to 'INFO'.\nPrincess has replaced MockBot.  Config: DEFAULT.  Verbosity: " + DEFAULT_VERBOSITY + ".\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);

        // Test setting both config and verbosity for Princess.
        // Because makeNewPrincessClient is mocked out, the log level is always going to be ERROR.
        setUp();
        args = new String[] {
                "/replacePlayer", "-b:Princess", "-v:" + LogLevel.WARNING, "-c:ESCAPE",
                            "-p:" + BOT_PLAYER_NAME};
        expected = "Verbosity set to 'WARNING'.\nPrincess has replaced MockBot.  Config: ESCAPE.  Verbosity: " + DEFAULT_VERBOSITY + ".\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);
        expectedBehavior = BehaviorSettingsFactory.getInstance().getBehavior("ESCAPE");
        TestCase.assertEquals(expectedBehavior, mockPrincess.getBehaviorSettings());

        // Test a non-ghost player.
        setUp();
        args = new String[]{"/replacePlayer", "-b:TestBot", HUMAN_PLAYER_NAME};
        expected = "Player MockHuman is not a ghost.\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);

        // Test a non-existant player.
        setUp();
        args = new String[]{"/replacePlayer", "-b:TestBot", "invalid player"};
        expected = "No player with the name 'invalid player'.\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);

        // Test an invalid bot name.
        setUp();
        args = new String[]{"/replacePlayer", "-b:InvalidBot", BOT_PLAYER_NAME};
        expected = "Unrecognized bot: 'InvalidBot'.  Defaulting to TestBot.\nTestBot has replaced MockBot.\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);

        // Test an invalid config name for Princess.
        setUp();
        args = new String[]{"/replacePlayer", "-b:Princess", "-c:invalid", "-p:" + BOT_PLAYER_NAME};
        expected = "Unrecognized Behavior Setting: 'invalid'.  Using DEFAULT.\n" +
                   "Princess has replaced MockBot.  Config: DEFAULT.  Verbosity: " + DEFAULT_VERBOSITY + ".\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);
        expectedBehavior = BehaviorSettingsFactory.getInstance().getBehavior("DEFAULT");
        TestCase.assertEquals(expectedBehavior, mockPrincess.getBehaviorSettings());

        // Test an invalid verbosity level for Princess.
        setUp();
        args = new String[]{"/replacePlayer", "-b:Princess", "-v:invalid", "-p:" + BOT_PLAYER_NAME};
        expected = "Invalid Verbosity: 'invalid'.  Defaulting to " + DEFAULT_VERBOSITY +
                   ".\nVerbosity set to '" + DEFAULT_VERBOSITY + "'." +
                   "\nPrincess has replaced MockBot.  Config: DEFAULT.  Verbosity: " + DEFAULT_VERBOSITY + ".\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);

        // Test leaving out a delimiter.
        setUp();
        args = new String[]{"/replacePlayer", "-b:Princess", LogLevel.WARNING.toString(), "-c:ESCAPE",
                            "-p:" + BOT_PLAYER_NAME};
        expected = "Princess has replaced MockBot.  Config: ESCAPE.  Verbosity: " + DEFAULT_VERBOSITY + ".\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);
        expectedBehavior = BehaviorSettingsFactory.getInstance().getBehavior("ESCAPE");
        TestCase.assertEquals(expectedBehavior, mockPrincess.getBehaviorSettings());

        // Test leaving out a different delimiter.
        setUp();
        args = new String[] {
                "/replacePlayer", "-b:Princess", "-v:" + LogLevel.WARNING, "ESCAPE",
                            "-p:" + BOT_PLAYER_NAME};
        expected = "Invalid Verbosity: 'WARNING ESCAPE'.  Defaulting to " + DEFAULT_VERBOSITY +
                   ".\nVerbosity set to '" + DEFAULT_VERBOSITY + "'." +
                   "\nPrincess has replaced MockBot.  Config: DEFAULT.  Verbosity: " + DEFAULT_VERBOSITY + ".\n";
        actual = testAddBotUtil.addBot(args, mockGame, mockClient.getHost(), mockClient.getPort());
        TestCase.assertEquals(expected, actual);
        expectedBehavior = BehaviorSettingsFactory.getInstance().getBehavior("DEFAULT");
        TestCase.assertEquals(expectedBehavior, mockPrincess.getBehaviorSettings());
    }
}
