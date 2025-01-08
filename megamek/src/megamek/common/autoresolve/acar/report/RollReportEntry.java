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

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Roll;

import java.util.List;

public class RollReportEntry extends PublicReportEntry {

    private enum Die {
        ONE(1, "\u2680"),
        TWO(2, "\u2681"),
        THREE(3, "\u2682"),
        FOUR(4, "\u2683"),
        FIVE(5, "\u2684"),
        SIX(6, "\u2685");

        private final int value;
        private final String symbol;

        Die(int value, String symbol) {
            this.value = value;
            this.symbol = symbol;
        }

        public int getValue() {
            return value;
        }

        public String getSymbol() {
            return symbol;
        }

        public static Die getDie(int value) {
            if (!validDie(value)) {
                throw new IllegalArgumentException("Invalid die value: " + value + ", value must be between 1 and 6");
            }
            return Die.values()[value-1];
        }

        public static boolean validDie(int value) {
            return value >= 1 && value <= 6;
        }
    }


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
            var dv = dices[i];
            if (Die.validDie(dv)) {
                dicesText.append(Die.getDie(dv).getSymbol());
            } else {
                dicesText.append(dv);
            }
            if (i < dices.length - 1) {
                dicesText.append(" + ");
            }
        }

        return UIUtil.spanCSS("dice", dicesText.toString()) + " " + UIUtil.spanCSS("roll", rollText);
    }
}
