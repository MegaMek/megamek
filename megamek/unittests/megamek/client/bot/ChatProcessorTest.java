package megamek.client.bot;

import junit.framework.TestCase;
import megamek.common.IGame;
import megamek.common.IPlayer;
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

    private static final IPlayer MOCK_BOT_PLAYER_HAL = Mockito.mock(IPlayer.class);
    private static final IPlayer MOCK_BOT_PLAYER_V_GER = Mockito.mock(IPlayer.class);
    private static final IPlayer MOCK_HUMAN_PLAYER_DAVE = Mockito.mock(IPlayer.class);
    private static final IPlayer MOCK_HUMAN_PLAYER_KIRK = Mockito.mock(IPlayer.class);
    private static final Vector<IPlayer> PLAYER_VECTOR = new Vector<IPlayer>(4);

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockBotHal.getLocalPlayer()).thenReturn(MOCK_BOT_PLAYER_HAL );
        Mockito.when(mockBotHal.getGame()).thenReturn(mockGame);

        Mockito.when(mockBotVGer.getLocalPlayer()).thenReturn(MOCK_BOT_PLAYER_V_GER);
        Mockito.when(mockBotVGer.getGame()).thenReturn(mockGame);

        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.getName()).thenReturn("Dave");
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_BOT_PLAYER_HAL )).thenReturn(true);
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_BOT_PLAYER_V_GER)).thenReturn(false);
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_HUMAN_PLAYER_DAVE)).thenReturn(false);
        Mockito.when(MOCK_HUMAN_PLAYER_DAVE.isEnemyOf(MOCK_HUMAN_PLAYER_KIRK)).thenReturn(true);

        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.getName()).thenReturn("Kirk");
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.isEnemyOf(MOCK_BOT_PLAYER_HAL)).thenReturn(false);
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.isEnemyOf(MOCK_BOT_PLAYER_V_GER)).thenReturn(true);
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.isEnemyOf(MOCK_HUMAN_PLAYER_DAVE)).thenReturn(true);
        Mockito.when(MOCK_HUMAN_PLAYER_KIRK.isEnemyOf(MOCK_HUMAN_PLAYER_KIRK)).thenReturn(false);

        Mockito.when(MOCK_BOT_PLAYER_HAL .getName()).thenReturn("Hal");
        Mockito.when(MOCK_BOT_PLAYER_HAL .isEnemyOf(MOCK_BOT_PLAYER_HAL )).thenReturn(false);
        Mockito.when(MOCK_BOT_PLAYER_HAL .isEnemyOf(MOCK_BOT_PLAYER_V_GER)).thenReturn(true);
        Mockito.when(MOCK_BOT_PLAYER_HAL .isEnemyOf(MOCK_HUMAN_PLAYER_DAVE)).thenReturn(true);
        Mockito.when(MOCK_BOT_PLAYER_HAL .isEnemyOf(MOCK_HUMAN_PLAYER_KIRK)).thenReturn(false);

        Mockito.when(MOCK_BOT_PLAYER_V_GER.getName()).thenReturn("V'Ger");
        Mockito.when(MOCK_BOT_PLAYER_V_GER.isEnemyOf(MOCK_BOT_PLAYER_HAL )).thenReturn(true);
        Mockito.when(MOCK_BOT_PLAYER_V_GER.isEnemyOf(MOCK_BOT_PLAYER_V_GER)).thenReturn(false);
        Mockito.when(MOCK_BOT_PLAYER_V_GER.isEnemyOf(MOCK_HUMAN_PLAYER_DAVE)).thenReturn(false);
        Mockito.when(MOCK_BOT_PLAYER_V_GER.isEnemyOf(MOCK_HUMAN_PLAYER_KIRK)).thenReturn(true);

        PLAYER_VECTOR.add(MOCK_HUMAN_PLAYER_DAVE);
        PLAYER_VECTOR.add(MOCK_BOT_PLAYER_HAL);
        PLAYER_VECTOR.add(MOCK_HUMAN_PLAYER_KIRK);
        PLAYER_VECTOR.add(MOCK_BOT_PLAYER_V_GER);
        Mockito.when(mockGame.getPlayersVector()).thenReturn(PLAYER_VECTOR);
    }

    @Test
    public void testShouldBotAcknowledgeDefeat() {
        ChatProcessor chatProcessor = new ChatProcessor();

        // Test individual human victory.
        String cmd = VictoryCommand.getDeclareIndividual(MOCK_HUMAN_PLAYER_DAVE.getName());
        String msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test team human victory.
        cmd = VictoryCommand.getDeclareTeam(MOCK_HUMAN_PLAYER_DAVE.getName());
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
        cmd = VictoryCommand.getDeclareIndividual(MOCK_HUMAN_PLAYER_KIRK.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by self.
        cmd = VictoryCommand.getDeclareIndividual(MOCK_BOT_PLAYER_HAL.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));

        // Test victory by opposing bot.
        cmd = VictoryCommand.getDeclareIndividual(MOCK_BOT_PLAYER_V_GER.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeDefeat(msg, mockBotHal));
    }

    @Test
    public void testShouldBotAcknowledgeVictory() {
        ChatProcessor chatProcessor = new ChatProcessor();

        // Test enemy wants defeat.
        String cmd = DefeatCommand.getWantsDefeat(MOCK_HUMAN_PLAYER_DAVE.getName());
        String msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test enemy admits defeat.
        cmd = DefeatCommand.getAdmitsDefeat(MOCK_HUMAN_PLAYER_DAVE.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertTrue(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test ally wants defeat.
        cmd = DefeatCommand.getWantsDefeat(MOCK_HUMAN_PLAYER_KIRK.getName());
        msg = Server.formatChatMessage(Server.ORIGIN, cmd);
        TestCase.assertFalse(chatProcessor.shouldBotAcknowledgeVictory(msg, mockBotHal));

        // Test ally admits defeat.
        cmd = DefeatCommand.getAdmitsDefeat(MOCK_HUMAN_PLAYER_KIRK.getName());
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