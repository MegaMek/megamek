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

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Roll;

import java.util.List;

public class RollReportEntry extends PublicReportEntry {

    private static final String ONE = "⚀";
    private static final String TWO = "⚁";
    private static final String THREE = "⚂";
    private static final String FOUR = "⚃";
    private static final String FIVE = "⚄";
    private static final String SIX = "⚅";

    private static final List<String> DICE = List.of(ONE, TWO, THREE, FOUR, FIVE, SIX);

    private final String rollText;
    private final int[] dices;

    public RollReportEntry(Roll roll) {
        super(0);
        rollText = roll.toString();
        var size = roll.getIntValues().length;
        dices = new int[size];

        System.arraycopy(roll.getIntValues(), 0, dices, 0, size);
    }

    @Override
    protected String reportText() {
        var dicesText = new StringBuilder();
        for (int i = 0; i < dices.length; i++) {
            var dv = dices[i] - 1;
            if (dv < 0 || dv >= DICE.size()) {
                dicesText.append(dv);
            } else {
                dicesText.append(DICE.get(dv));
            }
            if (i < dices.length - 1) {
                dicesText.append(" + ");
            }
        }
        return UIUtil.spanCSS("dice", dicesText.toString()) + " " + UIUtil.spanCSS("roll", rollText);
    }
}
