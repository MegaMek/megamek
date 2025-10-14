/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.StringTokenizer;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * This class has been downloaded from a third-party source: Author: Rob Camick Website:
 * https://tips4java.wordpress.com/2008/11/06/wrap-layout/ FlowLayout subclass that fully supports wrapping of
 * components.
 */
public class WrapLayout extends FlowLayout {
    /**
     * Constructs a new <code>WrapLayout</code> with a left alignment and a default 5-unit horizontal and vertical gap.
     */
    public WrapLayout() {
        super();
    }

    /**
     * Constructs a new <code>FlowLayout</code> with the specified alignment and a default 5-unit horizontal and
     * vertical gap. The value of the alignment argument must be one of
     * <code>WrapLayout</code>, <code>WrapLayout</code>,
     * or <code>WrapLayout</code>.
     *
     * @param align the alignment value
     */
    public WrapLayout(int align) {
        super(align);
    }

    /**
     * Creates a new flow layout manager with the indicated alignment and the indicated horizontal and vertical gaps.
     * <p>
     * The value of the alignment argument must be one of
     * <code>WrapLayout</code>, <code>WrapLayout</code>,
     * or <code>WrapLayout</code>.
     *
     * @param align the alignment value
     * @param hGap  the horizontal gap between components
     * @param vGap  the vertical gap between components
     */
    public WrapLayout(int align, int hGap, int vGap) {
        super(align, hGap, vGap);
    }

    /**
     * Returns the preferred dimensions for this layout given the
     * <i>visible</i> components in the specified target container.
     *
     * @param target the component which needs to be laid out
     *
     * @return the preferred dimensions to lay out the subcomponents of the specified container
     */
    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    /**
     * Returns the minimum dimensions needed to lay out the <i>visible</i> components contained in the specified target
     * container.
     *
     * @param target the component which needs to be laid out
     *
     * @return the minimum dimensions to lay out the subcomponents of the specified container
     */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    /**
     * Returns the minimum or preferred dimension needed to lay out the target container.
     *
     * @param target    target to get layout size for
     * @param preferred should preferred size be calculated
     *
     * @return the dimension to lay out the target container
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // Each row must fit with the width allocated to the container.
            // When the container width = 0, the preferred width of the container
            // has not yet been calculated so lets ask for the maximum.

            Container container = target;

            while ((container.getSize().width == 0) && (container.getParent() != null)) {
                container = container.getParent();
            }

            int targetWidth = container.getSize().width;

            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hGap = getHgap();
            int vGap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hGap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            // Fit components into the allowed width

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int numMembers = target.getComponentCount();

            for (int i = 0; i < numMembers; i++) {
                Component m = target.getComponent(i);

                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    // Can't add the component to current row. Start a new row.

                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    // Add a horizontal gap for all components after the first

                    if (rowWidth != 0) {
                        rowWidth += hGap;
                    }

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vGap * 2;

            // When using a scroll pane or the DecoratedLookAndFeel we need to
            // make sure the preferred size is less than the size of the
            // target container so shrinking the container size works
            // correctly. Removing the horizontal gap is an easy way to do this.

            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

            if ((scrollPane != null) && target.isValid()) {
                dim.width -= (hGap + 1);
            }

            return dim;
        }
    }

    /**
     * A new row has been completed. Use the dimensions of this row to update the preferred size for the container.
     *
     * @param dim       update the width and height when appropriate
     * @param rowWidth  the width of the row to add
     * @param rowHeight the height of the row to add
     */
    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) {
            dim.height += getVgap();
        }

        dim.height += rowHeight;
    }

    /**
     * Inserts line breaks into a given input string to ensure that no line exceeds a maximum length of 100.
     *
     * @param input The input string to be wrapped.
     *
     * @return The string with line breaks inserted.
     */
    public static String wordWrap(String input) {
        return wordWrap(input, 100);
    }

    /**
     * Inserts line breaks into a given input string to ensure that no line exceeds a maximum length.
     *
     * @param input             The input string to be wrapped.
     * @param maximumCharacters The maximum number of characters (including whitespaces) on each line.
     *
     * @return The string with line breaks inserted.
     */
    public static String wordWrap(String input, int maximumCharacters) {
        String[] lines = input.split("<br>");

        StringBuilder output = new StringBuilder(input.length());
        output.append("<html>");

        // Process each line
        for (String line : lines) {
            StringTokenizer token = new StringTokenizer(line, " ");

            int lineLen = 0;
            while (token.hasMoreTokens()) {
                String word = token.nextToken();

                if (lineLen + word.length() > maximumCharacters) {
                    output.append("<br>");
                    lineLen = 0;
                }
                output.append(word).append(' ');
                lineLen += word.length();
            }
            output.append("<br>");
        }
        output.append("</html>");
        return output.toString();
    }
}
