package megamek.common;

import junit.framework.TestCase;
import megamek.client.ui.swing.util.PlayerColour;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PlayerTest {

    @Test
    public void testGetColorForPlayer() {
        String playerName = "jefke";
        IPlayer player = new Player(0, playerName);
        TestCase.assertEquals("<B><font color='8080b0'>" + playerName + "</font></B>", player.getColorForPlayer());

        playerName = "Jeanke";
        IPlayer player2 = new Player(1, playerName);
        player2.setColour(PlayerColour.FUCHSIA);
        TestCase.assertEquals("<B><font color='f000f0'>" + playerName + "</font></B>", player2.getColorForPlayer());

    }

}
