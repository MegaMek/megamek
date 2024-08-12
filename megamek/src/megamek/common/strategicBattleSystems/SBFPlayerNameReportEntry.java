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
package megamek.common.strategicBattleSystems;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Player;

public class SBFPlayerNameReportEntry extends SBFReportEntry {

    private final String playerName;
    private final String playerColorHex;

    public SBFPlayerNameReportEntry(Player player) {
        this(player.getName(), player.getColour());
        noNL();
    }

    public SBFPlayerNameReportEntry(String playerName, String playerColorHex) {
        super(0);
        this.playerName = playerName;
        this.playerColorHex = playerColorHex;
    }

    public SBFPlayerNameReportEntry(String playerName, PlayerColour color) {
        this(playerName, UIUtil.hexColor(color.getColour()));
    }

    @Override
    protected String reportText() {
        return "<span style='color:" + playerColorHex + ";'>" + playerName + "</span>";
    }

}
