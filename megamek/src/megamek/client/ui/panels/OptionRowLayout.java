/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Arranges option rows (special pilot abilities, quirks) into a responsive multi-column grid.
 *
 * <p>The option lists all face the same problem: a variable number of same-height rows that should fill the width
 * they are given, top-aligned, with the column count following the container. This holds that arrangement so the
 * panels can differ in what they list without differing in how it looks.</p>
 *
 * <p>Rows are re-added rather than rebuilt, so a caller may lay out the same components repeatedly - on a resize or
 * a filter keystroke - without losing selection or in-progress edits.</p>
 */
public final class OptionRowLayout {

    /** Horizontal gap between columns, added to the widest row when computing how many columns fit. */
    public static final int COLUMN_GAP = 8;

    /**
     * Component name marking a row's implementation-status label. The highlight applied to a selected row skips it,
     * so the status marker keeps its own muted colour instead of turning yellow with the rest of the row.
     */
    public static final String STATUS_MARKER_NAME = "optionStatusMarker";

    private OptionRowLayout() {
    }

    /**
     * @param availableWidth the usable width inside the panel, in pixels
     * @param maxRowWidth    the widest row that has to fit in a column, in pixels
     *
     * @return how many columns fit, at least one. A non-positive width or row width yields a single column, which is
     *       the right answer before the panel has been laid out and measured.
     */
    public static int calculateColumns(int availableWidth, int maxRowWidth) {
        if ((availableWidth <= 0) || (maxRowWidth <= 0)) {
            return 1;
        }
        return Math.max(1, availableWidth / (maxRowWidth + COLUMN_GAP));
    }

    /**
     * Replaces the panel's contents with the given rows, filling left to right and wrapping every {@code columns}
     * rows. A glue component is added underneath so the rows stay at the top when the panel is stretched.
     *
     * @param panel         the panel to fill; its layout is set to {@link GridBagLayout}
     * @param rowComponents the rows to show, in display order. An empty list leaves the panel empty.
     * @param columns       how many columns to use; a non-positive value leaves the panel empty
     */
    public static void relayout(JPanel panel, List<? extends JComponent> rowComponents, int columns) {
        panel.removeAll();
        if (!rowComponents.isEmpty() && (columns > 0)) {
            panel.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.weightx = 1.0;
            constraints.insets = new Insets(0, 2, 0, 2);

            int currentColumn = 0;
            for (JComponent rowComponent : rowComponents) {
                constraints.gridx = currentColumn;
                panel.add(rowComponent, constraints);
                currentColumn++;
                if (currentColumn >= columns) {
                    currentColumn = 0;
                    constraints.gridy++;
                }
            }

            // Bottom glue keeps the rows at the top when side-by-side panels are given equal heights
            constraints.gridx = 0;
            constraints.gridy++;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.weighty = 1.0;
            constraints.fill = GridBagConstraints.BOTH;
            panel.add(new JPanel(), constraints);
        }
        panel.revalidate();
        panel.repaint();
    }
}
