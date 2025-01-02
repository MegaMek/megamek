/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.autoresolve.acar.report;

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import mekhq.campaign.autoresolve.component.Formation;

public class FormationReportEntry extends PublicReportEntry {

    private final String formationName;
    private final String playerColorHex;

    public FormationReportEntry(String formationName, String playerColorHex) {
        super(0);
        this.formationName = formationName;
        this.playerColorHex = playerColorHex;
        noNL();
    }

    public FormationReportEntry(Formation formation, IGame game) {
        this(formation.getName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(formation, game)));
    }

    @Override
    protected String reportText() {
        return "<span style='color:" + playerColorHex + "; font-weight: bold;'>" + formationName + "</span>";
    }
}
