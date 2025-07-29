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
package megamek.client.ui.entityreadout;

import megamek.client.ui.util.DiscordFormat;

import java.util.*;

/**
 * Data laid out in a table with named columns. The columns are left-justified by default, but justification can be set
 * for columns individually. Plain text output requires a monospace font to line up correctly. For HTML and discord
 * output the background color of an individual row can be set.
 */
class TableElement implements MultiRowViewElement {

    private static final String HTML_ROW = "<TR>%s</TR>\n";
    private static final String HTML_HEADER_CELL = "<TH %s>%s</TH>";
    private static final String HTML_DATA_CELL = "<TD %s>%s</TD>";
    private static final String HTML_TABLE = "<table cellspacing=0 cellpadding=2 border=0>%s</TABLE>\n";
    private static final String HTML_NBSP = "&nbsp;";
    private static final String PLAIN_COL_PADDING = "  ";

    static final int JUSTIFIED_LEFT = 0;
    static final int JUSTIFIED_CENTER = 1;
    static final int JUSTIFIED_RIGHT = 2;

    private final int[] justification;
    private final String[] colNames;
    private final Map<Integer, Integer> colWidth = new HashMap<>();
    private final List<ViewElement[]> data = new ArrayList<>();

    TableElement(int colCount) {
        justification = new int[colCount];
        colNames = new String[colCount];
        Arrays.fill(colNames, "");
    }

    void setColNames(String... colNames) {
        Arrays.fill(this.colNames, "");
        System.arraycopy(colNames, 0, this.colNames, 0,
              Math.min(colNames.length, this.colNames.length));
        colWidth.clear();
        for (int i = 0; i < colNames.length; i++) {
            colWidth.put(i, colNames[i].length());
        }
    }

    void setJustification(int... justification) {
        Arrays.fill(this.justification, JUSTIFIED_LEFT);
        System.arraycopy(justification, 0, this.justification, 0,
              Math.min(justification.length, this.justification.length));
    }

    public void addRow(String... row) {
        ViewElement[] convertedRow = new ViewElement[row.length];
        for (int i = 0; i < row.length; i++) {
            convertedRow[i] = (row[i] == null) ? new EmptyElement() : new PlainElement(row[i]);
        }
        addRow(convertedRow);
    }

    public void addRow(ViewElement... row) {
        for (int i = 0; i < row.length; i++) {
            if (row[i] == null) {
                row[i] = new EmptyElement();
            }
        }
        data.add(row);
        for (int i = 0; i < row.length; i++) {
            colWidth.merge(i, row[i].toPlainText().length(), Math::max);
        }
    }

    private String leftPad(String s, int fieldSize) {
        if (fieldSize > 0) {
            return String.format("%" + fieldSize + "s", s);
        } else {
            return "";
        }
    }

    private String rightPad(String s, int fieldSize) {
        if (fieldSize > 0) {
            return String.format("%-" + fieldSize + "s", s);
        } else {
            return "";
        }
    }

    private String center(String s, int fieldSize) {
        int rightPadding = Math.max(fieldSize - s.length(), 0) / 2;
        return rightPad(leftPad(s, fieldSize - rightPadding), fieldSize);
    }

    private String justify(int justification, String s, int fieldSize) {
        if (justification == JUSTIFIED_CENTER) {
            return center(s, fieldSize);
        } else if (justification == JUSTIFIED_LEFT) {
            return rightPad(s, fieldSize);
        } else {
            return leftPad(s, fieldSize);
        }
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        for (int col = 0; col < colNames.length; col++) {
            sb.append(justify(justification[col], colNames[col], colWidth.get(col)));
            sb.append(PLAIN_COL_PADDING);
        }
        sb.append("\n");
        if (colNames.length > 0) {
            int w = sb.length() - 1 - PLAIN_COL_PADDING.length();
            sb.append("-".repeat(Math.max(0, w))).append("\n");
        }
        for (ViewElement[] row : data) {
            for (int col = 0; col < row.length; col++) {
                sb.append(justify(justification[col], row[col].toPlainText(), colWidth.get(col)));
                sb.append(PLAIN_COL_PADDING);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toHTML() {

        StringBuilder tableRows = new StringBuilder();
        if (colNames.length > 0) {
            StringBuilder rowBuilder = new StringBuilder();
            for (int col = 0; col < colNames.length; col++) {
                rowBuilder.append(HTML_HEADER_CELL.formatted(htmlJustifyCell(col), colNames[col]));
                rowBuilder.append(HTML_HEADER_CELL.formatted("", HTML_NBSP.repeat(4)));
            }
            tableRows.append(HTML_ROW.formatted(rowBuilder.toString()));
        }
        for (ViewElement[] row : data) {
            StringBuilder rowBuilder = new StringBuilder();
            for (int col = 0; col < row.length; col++) {
                rowBuilder.append(HTML_DATA_CELL.formatted(htmlJustifyCell(col), row[col].toHTML()));
                rowBuilder.append(HTML_DATA_CELL.formatted("", HTML_NBSP.repeat(4)));
            }
            tableRows.append(HTML_ROW.formatted(rowBuilder.toString()));
        }
        return HTML_TABLE.formatted(tableRows.toString());
    }

    private String htmlJustifyCell(int col) {
        return " align=" + switch (justification[col]) {
            case JUSTIFIED_RIGHT -> "right";
            case JUSTIFIED_CENTER -> "center";
            default -> "left";
        };
    }

    @Override
    public String toDiscord() {
        StringBuilder sb = new StringBuilder();
        sb.append(DiscordFormat.UNDERLINE).append(DiscordFormat.ROW_SHADING);
        for (int col = 0; col < colNames.length; col++) {
            sb.append(justify(justification[col], colNames[col], colWidth.get(col)));
            if (col < colNames.length - 1) {
                sb.append(PLAIN_COL_PADDING);
            }
        }
        sb.append(DiscordFormat.RESET) .append("\n");
        for (int r = 0; r < data.size(); r++) {
            final ViewElement[] row = data.get(r);
            if (r % 2 == 1) {
                sb.append(DiscordFormat.ROW_SHADING);
            }
            for (int col = 0; col < row.length; col++) {
                sb.append(DiscordFormat.highlightNumbersForDiscord(justify(justification[col],
                      row[col].toPlainText(), colWidth.get(col))));
                if (col < row.length - 1) {
                    sb.append(PLAIN_COL_PADDING);
                }
            }
            sb.append(DiscordFormat.RESET).append("\n");
        }
        return sb.toString();
    }
}
