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
import megamek.common.Entity;
import megamek.common.autoresolve.component.Formation;

public class EntityNameReportEntry extends PublicReportEntry {

    private final String name;
    private final String playerColorHex;
    private final String playerTeam;

    public EntityNameReportEntry(Entity entity) {
        this(entity.getDisplayName(), entity.getOwner().getColour(), entity.getOwner().getTeam());
    }

    public EntityNameReportEntry(String name, PlayerColour color, int playerTeam) {
        super(null);
        this.name = name;
        this.playerColorHex = UIUtil.hexColor(color.getColour());
        this.playerTeam = playerTeam + "";
        noNL();
    }

    @Override
    protected String reportText() {
        return "<span style='color:" + playerColorHex + "; font-weight: bold;'>" + name + "[Team " + playerTeam + "]</span>";
    }

}
