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
package megamek.client.ui.unitreadout;

import megamek.client.ui.util.DiscordFormat;

import java.util.*;

/**
 * Data laid out in a table with named columns. The columns are left-justified by default, but justification can be set
 * for columns individually. Plain text output requires a monospace font to line up correctly. For HTML and discord
 * output the background color of an individual row can be set.
 */
class TableElement implements MultiRowViewElement {

    static final int JUSTIFIED_LEFT = 0;
    static final int JUSTIFIED_CENTER = 1;
    static final int JUSTIFIED_RIGHT = 2;

    private final int[] justification;
    private final String[] colNames;
    private final List<String[]> data = new ArrayList<>();
    private final Map<Integer, Integer> colWidth = new HashMap<>();
    private final Map<Integer, String> colors = new HashMap<>();

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
        data.add(row);
        for (int i = 0; i < row.length; i++) {
            colWidth.merge(i, row[i].length(), Math::max);
        }
    }

    void addRowWithColor(String color, String... row) {
        addRow(row);
        colors.put(data.size() - 1, color);
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
        final String COL_PADDING = "  ";
        StringBuilder sb = new StringBuilder();
        for (int col = 0; col < colNames.length; col++) {
            sb.append(justify(justification[col], colNames[col], colWidth.get(col)));
            if (col < colNames.length - 1) {
                sb.append(COL_PADDING);
            }
        }
        sb.append("\n");
        if (colNames.length > 0) {
            int w = sb.length() - 1;
            sb.append("-".repeat(Math.max(0, w)));
            sb.append("\n");
        }
        for (String[] row : data) {
            for (int col = 0; col < row.length; col++) {
                sb.append(justify(justification[col], row[col], colWidth.get(col)));
                if (col < row.length - 1) {
                    sb.append(COL_PADDING);
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toHTML() {
        StringBuilder sb = new StringBuilder("<table cellspacing=\"0\" cellpadding=\"2\" border=\"0\">");
        if (colNames.length > 0) {
            sb.append("<tr>");
            for (int col = 0; col < colNames.length; col++) {
                if (justification[col] == JUSTIFIED_RIGHT) {
                    sb.append("<th align=\"right\">");
                } else if (justification[col] == JUSTIFIED_CENTER) {
                    sb.append("<th align=\"center\">");
                } else {
                    sb.append("<th align=\"left\">");
                }
                if (justification[col] != JUSTIFIED_LEFT) {
                    sb.append("&nbsp;&nbsp;");
                }
                sb.append(colNames[col]);
                if (justification[col] != JUSTIFIED_RIGHT) {
                    sb.append("&nbsp;&nbsp;");
                }
                sb.append("</th>");
            }
            sb.append("</tr>\n");
        }
        for (int r = 0; r < data.size(); r++) {
            if (colors.containsKey(r)) {
                sb.append("<tr color=\"").append(colors.get(r)).append("\">");
            } else {
                sb.append("<tr>");
            }
            final String[] row = data.get(r);
            for (int col = 0; col < row.length; col++) {
                if (justification[col] == JUSTIFIED_RIGHT) {
                    sb.append("<td align=\"right\">");
                } else if (justification[col] == JUSTIFIED_CENTER) {
                    sb.append("<td align=\"center\">");
                } else {
                    sb.append("<td align=\"left\">");
                }
                if (justification[col] != JUSTIFIED_LEFT) {
                    sb.append("&nbsp;&nbsp;");
                }
                sb.append(row[col]);
                if (justification[col] != JUSTIFIED_RIGHT) {
                    sb.append("&nbsp;&nbsp;");
                }
                sb.append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    @Override
    public String toDiscord() {
        final String COL_PADDING = "  ";
        StringBuilder sb = new StringBuilder();
        sb.append(DiscordFormat.UNDERLINE).append(DiscordFormat.ROW_SHADING);
        for (int col = 0; col < colNames.length; col++) {
            sb.append(justify(justification[col], colNames[col], colWidth.get(col)));
            if (col < colNames.length - 1) {
                sb.append(COL_PADDING);
            }
        }
        sb.append(DiscordFormat.RESET);
        sb.append("\n");
        for (int r = 0; r < data.size(); r++) {
            final String[] row = data.get(r);
            if (r % 2 == 1) {
                sb.append(DiscordFormat.ROW_SHADING);
            }
            for (int col = 0; col < row.length; col++) {
                sb.append(DiscordFormat.highlightNumbersForDiscord(justify(justification[col], row[col], colWidth.get(col))));
                if (col < row.length - 1) {
                    sb.append(COL_PADDING);
                }
            }
            sb.append(DiscordFormat.RESET).append("\n");
        }
        return sb.toString();
    }
}
