/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.autoresolve.acar.report;

import java.awt.Color;

import megamek.client.ui.util.UIUtil;
import megamek.common.autoresolve.component.Formation;
import megamek.common.strategicBattleSystems.SBFUnit;

public class UnitReportEntry extends PublicReportEntry {

    private final String formationName;
    private final String unitName;
    private final String playerColorHex;

    public UnitReportEntry(String formationName, String unitName, String playerColorHex) {
        super(null);
        this.formationName = formationName;
        this.unitName = unitName;
        this.playerColorHex = playerColorHex;
        noNL();
    }

    public UnitReportEntry(Formation formation, int unitIndex, Color color) {
        this((formation.getEntity() == null) ? formation.getName() : null,
              unitName(formation, unitIndex),
              UIUtil.hexColor(color));
    }

    public UnitReportEntry(Formation formation, SBFUnit unit, Color color) {
        this((formation.getEntity() == null) ? formation.getName() : null, unit.getName(), UIUtil.hexColor(color));
    }

    public UnitReportEntry(SBFUnit unit, Color color) {
        this(null, unit.getName(), UIUtil.hexColor(color));
    }


    @Override
    protected String reportText() {
        if (formationName == null) {
            return "<span style='color:" + playerColorHex + "; font-weight: bold;'>" + unitName + "</span>";
        }
        return "[<span style='color:" + playerColorHex + "; font-weight: bold;'>" + formationName + "</span>]" + " "
              + "<span style='color:" + playerColorHex + "; font-weight: bold;'>" + unitName + "</span>";
    }

    private static String unitName(Formation formation, int unitIndex) {
        if ((unitIndex < 0) || (unitIndex > formation.getUnits().size())) {
            throw new IllegalArgumentException("Invalid unit index");
        } else {
            var unit = formation.getUnits().get(unitIndex);

            return unit.getName();
        }
    }
}
