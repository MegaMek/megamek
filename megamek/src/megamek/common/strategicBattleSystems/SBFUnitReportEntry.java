/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.strategicBattleSystems;

import megamek.client.ui.swing.util.UIUtil;

import java.awt.*;

public class SBFUnitReportEntry extends SBFReportEntry {

    private final String formationName;
    private final String unitName;
    private final String playerColorHex;

    public SBFUnitReportEntry(String formationName, String unitName, String playerColorHex) {
        super(0);
        this.formationName = formationName;
        this.unitName = unitName;
        this.playerColorHex = playerColorHex;
        noNL();
    }

    public SBFUnitReportEntry(SBFFormation formation, int unitIndex, Color color) {
        this(formation.generalName(), unitName(formation, unitIndex), UIUtil.hexColor(color));
    }

    @Override
    protected String reportText() {
        return "<span style='color:" + playerColorHex + ";'>" + formationName + "</span>" + ", "
                + "<span style='color:" + playerColorHex + ";'>" + unitName + "</span>";
    }

    private static String unitName(SBFFormation formation, int unitIndex) {
        if ((unitIndex < 0) || (unitIndex > formation.getUnits().size())) {
            throw new IllegalArgumentException("Invalid unit index");
        } else {
            return formation.getUnits().get(unitIndex).getName();
        }
    }
}
