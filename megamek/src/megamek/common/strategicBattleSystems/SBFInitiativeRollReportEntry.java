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

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.InitiativeRoll;

import java.util.List;

public class SBFInitiativeRollReportEntry extends SBFReportEntry {

    private static final String ONE = "\u2680";
    private static final String TWO = "\u2681";
    private static final String THREE = "\u2682";
    private static final String FOUR = "\u2683";
    private static final String FIVE = "\u2684";
    private static final String SIX = "\u2685";

    private static final List<String> DICE = List.of(ONE, TWO, THREE, FOUR, FIVE, SIX);

    private final String rollText;


    public SBFInitiativeRollReportEntry(InitiativeRoll roll) {
        super(0);
        rollText = roll.toString();
    }

    @Override
    protected String reportText() {
        return UIUtil.spanCSS("dice", SIX) + " " + UIUtil.spanCSS("roll", rollText);
    }
}
