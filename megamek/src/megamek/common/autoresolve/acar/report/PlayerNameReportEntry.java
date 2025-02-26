/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.acar.report;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Player;

public class PlayerNameReportEntry extends PublicReportEntry {

    private final String playerName;
    private final String playerColorHex;

    public PlayerNameReportEntry(Player player) {
        this(player.getName(), player.getColour());
        noNL();
    }

    public PlayerNameReportEntry(String playerName, String playerColorHex) {
        super(null);
        this.playerName = playerName;
        this.playerColorHex = playerColorHex;
    }

    public PlayerNameReportEntry(String playerName, PlayerColour color) {
        this(playerName, UIUtil.hexColor(color.getColour()));
    }

    @Override
    protected String reportText() {
        return "<span style='color:" + playerColorHex + "; font-weight: bold;'>" + playerName + "</span>";
    }
}
