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
package megamek.client.ui.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * A layout manager that arranges components in a vertical grid pattern. Components are placed column by column, filling
 * vertically before moving to the next column. This is in contrast to the standard GridLayout which fills horizontally
 * first.
 *
 * @author MegaMek Team
 */
public class VerticalGridLayout extends GridLayout {

    /**
     * Creates a vertical grid layout with a default of one column per component.
     */
    public VerticalGridLayout() {
        super();
    }

    /**
     * Creates a vertical grid layout with the specified number of rows and columns.
     *
     * @param rows the number of rows in the grid
     * @param cols the number of columns in the grid
     */
    public VerticalGridLayout(int rows, int cols) {
        super(rows, cols);
    }

    /**
     * Creates a vertical grid layout with the specified number of rows and columns, and the specified horizontal and
     * vertical gaps.
     *
     * @param rows          the number of rows in the grid
     * @param cols          the number of columns in the grid
     * @param horizontalGap the horizontal gap between components
     * @param verticalGap   the vertical gap between components
     */
    public VerticalGridLayout(int rows, int cols, int horizontalGap, int verticalGap) {
        super(rows, cols, horizontalGap, verticalGap);
    }

    /**
     * Lays out the specified container using this layout. Components are arranged in a vertical grid pattern, filling
     * columns before rows.
     *
     * @param parent the container to be laid out
     */
    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            final int numComponents = parent.getComponentCount();
            if (numComponents == 0) {
                return;
            }

            final Insets insets = parent.getInsets();
            final GridDimensions gridDims = calculateGridDimensions(numComponents);
            final ComponentDimensions compDims = calculateComponentDimensions(parent, insets, gridDims);

            positionComponents(parent, insets, gridDims, compDims, numComponents);
        }
    }

    /**
     * Calculates the optimal grid dimensions based on the number of components.
     *
     * @param numComponents the number of components to arrange
     *
     * @return the calculated grid dimensions
     */
    private GridDimensions calculateGridDimensions(int numComponents) {
        int numRows = getRows();
        int numCols = getColumns();

        if (numRows > 0) {
            numCols = (numComponents + numRows - 1) / numRows;
        } else {
            numRows = (numComponents + numCols - 1) / numCols;
        }

        return new GridDimensions(numRows, numCols);
    }

    /**
     * Calculates the component dimensions and positioning information.
     *
     * @param parent   the parent container
     * @param insets   the container insets
     * @param gridDims the grid dimensions
     *
     * @return the calculated component dimensions
     */
    private ComponentDimensions calculateComponentDimensions(Container parent, Insets insets, GridDimensions gridDims) {
        final int totalGapsWidth = (gridDims.numCols - 1) * getHgap();
        final int widthWOInsets = parent.getWidth() - insets.left - insets.right;
        final int widthOnComponent = (widthWOInsets - totalGapsWidth) / gridDims.numCols;
        final int extraWidthAvailable = (widthWOInsets - (widthOnComponent * gridDims.numCols + totalGapsWidth)) / 2;

        final int totalGapsHeight = (gridDims.numRows - 1) * getVgap();
        final int heightWOInsets = parent.getHeight() - insets.top - insets.bottom;
        final int heightOnComponent = (heightWOInsets - totalGapsHeight) / gridDims.numRows;
        final int extraHeightAvailable = (heightWOInsets - (heightOnComponent * gridDims.numRows + totalGapsHeight))
              / 2;

        return new ComponentDimensions(widthOnComponent, heightOnComponent, extraWidthAvailable, extraHeightAvailable);
    }

    /**
     * Positions all components in the container according to the grid layout.
     *
     * @param parent        the parent container
     * @param insets        the container insets
     * @param gridDims      the grid dimensions
     * @param compDims      the component dimensions
     * @param numComponents the number of components
     */
    private void positionComponents(Container parent, Insets insets, GridDimensions gridDims,
          ComponentDimensions compDims, int numComponents) {
        final boolean ltr = parent.getComponentOrientation().isLeftToRight();

        if (ltr) {
            positionComponentsLTR(parent, insets, gridDims, compDims, numComponents);
        } else {
            positionComponentsRTL(parent, insets, gridDims, compDims, numComponents);
        }
    }

    /**
     * Positions components for left-to-right orientation.
     */
    private void positionComponentsLTR(Container parent, Insets insets, GridDimensions gridDims,
          ComponentDimensions compDims, int numComponents) {
        int x = insets.left + compDims.extraWidthAvailable;

        for (int c = 0; c < gridDims.numCols; c++) {
            int y = insets.top + compDims.extraHeightAvailable;

            for (int r = 0; r < gridDims.numRows; r++) {
                final int componentIndex = c * gridDims.numRows + r;
                if (componentIndex < numComponents) {
                    final Component component = parent.getComponent(componentIndex);
                    component.setBounds(x, y, compDims.width, compDims.height);
                }
                y += compDims.height + getVgap();
            }
            x += compDims.width + getHgap();
        }
    }

    /**
     * Positions components for right-to-left orientation.
     */
    private void positionComponentsRTL(Container parent, Insets insets, GridDimensions gridDims,
          ComponentDimensions compDims, int numComponents) {
        int x = parent.getWidth() - insets.right - compDims.width - compDims.extraWidthAvailable;

        for (int c = 0; c < gridDims.numCols; c++) {
            int y = insets.top + compDims.extraHeightAvailable;

            for (int r = 0; r < gridDims.numRows; r++) {
                final int componentIndex = c * gridDims.numRows + r;
                if (componentIndex < numComponents) {
                    final Component component = parent.getComponent(componentIndex);
                    component.setBounds(x, y, compDims.width, compDims.height);
                }
                y += compDims.height + getVgap();
            }
            x -= compDims.width + getHgap();
        }
    }

    /**
     * Immutable record to hold grid dimensions.
     */
    private record GridDimensions(int numRows, int numCols) {
    }

    /**
     * Immutable record to hold component dimensions and positioning.
     */
    private record ComponentDimensions(int width, int height, int extraWidthAvailable, int extraHeightAvailable) {
    }
}
