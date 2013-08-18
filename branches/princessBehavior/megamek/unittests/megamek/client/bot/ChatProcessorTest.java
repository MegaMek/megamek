package megamek.client.bot;

import junit.framework.TestCase;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.server.Server;
import megamek.server.commands.DefeatCommand;
import megamek.server.commands.VictoryCommand;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Deric
 * Date: 8/17/13
 * Time: 9:32 AM
 */
@RunWith(JUnit4.class)
public class ChatProcessorTest {

    private static final BotClient mockBotHal = Mockito.mock(BotClient.class);
    private static final BotClient mockBotVGer = Mockito.mock(BotClient.class);
    private static final IGame mockGame = Mockito.mock(IGame.class);

    private static final Player mockBotPlayerHal = Mockito.mock(Player.class);
    private static final Player mockBotPlayerVGer = Mockito.mock(Player.class);
    private static final Player mockHumanPlayerDave = Mockito.mock(Player.class);
    private static final Player mockHumanPlayerKirk = Mockito.mock(Player.class);
    private static final Vector<Player> playerVector = new Vector<Player>(4);

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockBotHal.getLocalPlayer()).thenReturn(mockBotPlayerHal);
        Mockito.when(mockBotHal.getGame()).thenReturn(mockGame);

        Mockito.when(mockBotVGer.getLocalPlayer()).thenReturn(mockBotPlayerVGer);
        Mockito.when(mockBotVGer.getGame()).thenReturn(mockGame);

        Mockito.when(mockHumanPlayerDave.getName()).thenReturn("Dave");
        Mockito.when(mockHumanPlayerDave.isEnemyOf(mockBotPlayerHal)).thenReturn(true);
        Mockito.when(mockHumanPlayerDave.isEnemyOf(mockBotPlayerVGer)).thenReturn(false);
        Mockito.when(mockHumanPlayerDave.isEnemyOf(mockHumanPlayerDave)).thenReturn(false);
        Mockito.when(mockHumanPlayerDave.isEnemyOf(mockHumanPlayerKirk)).thenReturn(true);

        Mockito.when(mockHumanPlayerKirk.getName()).thenReturn("Kirk");
        Mockito.when(mockHumanPlayerKirk.isEnemyOf(mockBotPlayerHal)).thenReturn(false);
        Mockito.when(mockHumanPlayerKirk.isEnemyOf(mockBotPlayerVGer)).thenReturn(true);
        Mockito.when(mockHumanPlayerKirk.isEnemyOf(mockHumanPlayerDave)).thenReturn(true);
        Mockito.when(mockHumanPlayerKirk.isEnemyOf(mockHumanPlayerKirk)).thenReturn(false);

        Mockito.when(mockBotPlayerHal.getName()).thenReturn("Hal");
        Mockito.when(mockBotPlayerHal.isEnemyOf(mockBotPlayerHal)).thenReturn(false);
        Mockito.when(mockBotPlayerHal.isEnemyOf(mockBotPlayerVGer)).thenReturn(true);
        Mockito.when(mockBotPlayerHal.isEnemyOf(mockHumanPlayerDave)).thenReturn(true);
        Mockito.when(mockBotPlayerHal.isEnemyOf(mockHumanPlayerKirk)).thenReturn(false);

        Mockito.when(mockBotPlayerVGer.getName()).thenReturn("V'Ger");
        Mockito.when(mockBotPlayerVGer.isEnemyOf(mockBotPlayerHal)).thenReturn(true);
        Mockito.when(mockBotPlayerVGer.isEnemyOf(mockBotPlayerVGer)).thenReturn(false);
        Mockito.when(mockBotPlayerVGer.isEnemyOf(mockHumanPlayerDave)).thenReturn(false);
        Mockito.when(mockBotPlayerVGer.isEnemyOf(mockHumanPlayerKirk)).thenReturn(true);

        playerVector.add(mockHumanPlayerDave);
        playerVector.add(mockBotPlayerHal);
        playerVector.add(mockHumanPlayerKirk);
        playerVector.add(mockBotPlayerVGer);
        Mockito.when(mockGame.getPlayersVector()).thenReturn(playerVector);
    }

    @Test
    public void testShouldBotAcknowledgeDefeat() {
        ChatProcessor chatProcessor = new ChatProcessor();

        // Test individual human victory.
        String cmd = VictoryCommand.getDeclareIndividual(mockHumanPlayerDave.getName());
        String msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test team human victory.
        cmd = VictoryCommand.getDeclareTeam(mockHumanPlayerDave.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test a different message.
        msg = "This is general chat message with no bot commands.";
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test a null message.
        msg = null;
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test an empty message.
        msg = "";
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by human partner.
        cmd = VictoryCommand.getDeclareIndividual(mockHumanPlayerKirk.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by self.
        cmd = VictoryCommand.getDeclareIndividual(mockBotPlayerHal.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by opposing bot.
        cmd = VictoryCommand.getDeclareIndividual(mockBotPlayerVGer.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));
    }

    @Test
    public void testShouldBotAcknowledgeVictory() {
        ChatProcessor chatProcessor = new ChatProcessor();

        // Test enemy wants defeat.
        String cmd = DefeatCommand.getWantsDefeat(mockHumanPlayerDave.getName());
        String msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test enemy admits defeat.
        cmd = DefeatCommand.getAdmitsDefeat(mockHumanPlayerDave.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test ally wants defeat.
        cmd = DefeatCommand.getWantsDefeat(mockHumanPlayerKirk.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test ally admits defeat.
        cmd = DefeatCommand.getAdmitsDefeat(mockHumanPlayerKirk.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test a different message.
        msg = "This is general chat message with no bot commands.";
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test null message.
        msg = null;
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test empty message.
        msg = "";
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));
    }
}
