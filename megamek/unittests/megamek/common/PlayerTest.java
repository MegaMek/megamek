/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import megamek.client.ui.swing.util.PlayerColour;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(value = JUnit4.class)
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
