package megamek.common;

import megamek.client.ui.swing.util.PlayerColour;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlayerTest {

    @Test
    public void testGetColorForPlayer() {
        String playerName = "jefke";
        Player player = new Player(0, playerName);
        assertEquals("<B><font color='8080b0'>" + playerName + "</font></B>", player.getColorForPlayer());

        playerName = "Jeanke";
        Player player2 = new Player(1, playerName);
        player2.setColour(PlayerColour.FUCHSIA);
        assertEquals("<B><font color='f000f0'>" + playerName + "</font></B>", player2.getColorForPlayer());
    }
}
