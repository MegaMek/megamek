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

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.autoresolve.component.Formation;

public class FormationReportEntry extends PublicReportEntry {

    private final String formationName;
    private final String playerColorHex;
    private final String unitNames;

    public FormationReportEntry(String formationName, String unitNames, String playerColorHex) {
        super(null);
        this.formationName = formationName;
        this.unitNames = unitNames;
        this.playerColorHex = playerColorHex;
        noNL();
    }

    public FormationReportEntry(Formation formation, IGame game) {
        this(formation.getDisplayName(), String.join(", ", formation.getElementNames()), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(formation, game)));
    }

    @Override
    protected String reportText() {
        return "<a data-value='"+ unitNames +"'><span style='color:" + playerColorHex + "; font-weight: bold;'>" + formationName + "</span></a>";
    }
}
