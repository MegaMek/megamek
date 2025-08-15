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
     * @param rows the number of rows in the grid
     * @param cols the number of columns in the grid
     * @param hgap the horizontal gap between components
     * @param vgap the vertical gap between components
     */
    public VerticalGridLayout(int rows, int cols, int hgap, int vgap) {
        super(rows, cols, hgap, vgap);
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
            final int ncomponents = parent.getComponentCount();
            if (ncomponents == 0) {
                return;
            }

            final Insets insets = parent.getInsets();
            final GridDimensions gridDims = calculateGridDimensions(ncomponents);
            final ComponentDimensions compDims = calculateComponentDimensions(parent, insets, gridDims);

            positionComponents(parent, insets, gridDims, compDims, ncomponents);
        }
    }

    /**
     * Calculates the optimal grid dimensions based on the number of components.
     *
     * @param ncomponents the number of components to arrange
     *
     * @return the calculated grid dimensions
     */
    private GridDimensions calculateGridDimensions(int ncomponents) {
        int nrows = getRows();
        int ncols = getColumns();

        if (nrows > 0) {
            ncols = (ncomponents + nrows - 1) / nrows;
        } else {
            nrows = (ncomponents + ncols - 1) / ncols;
        }

        return new GridDimensions(nrows, ncols);
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
        final int totalGapsWidth = (gridDims.ncols - 1) * getHgap();
        final int widthWOInsets = parent.getWidth() - insets.left - insets.right;
        final int widthOnComponent = (widthWOInsets - totalGapsWidth) / gridDims.ncols;
        final int extraWidthAvailable = (widthWOInsets - (widthOnComponent * gridDims.ncols + totalGapsWidth)) / 2;

        final int totalGapsHeight = (gridDims.nrows - 1) * getVgap();
        final int heightWOInsets = parent.getHeight() - insets.top - insets.bottom;
        final int heightOnComponent = (heightWOInsets - totalGapsHeight) / gridDims.nrows;
        final int extraHeightAvailable = (heightWOInsets - (heightOnComponent * gridDims.nrows + totalGapsHeight)) / 2;

        return new ComponentDimensions(widthOnComponent, heightOnComponent, extraWidthAvailable, extraHeightAvailable);
    }

    /**
     * Positions all components in the container according to the grid layout.
     *
     * @param parent      the parent container
     * @param insets      the container insets
     * @param gridDims    the grid dimensions
     * @param compDims    the component dimensions
     * @param ncomponents the number of components
     */
    private void positionComponents(Container parent, Insets insets, GridDimensions gridDims,
          ComponentDimensions compDims, int ncomponents) {
        final boolean ltr = parent.getComponentOrientation().isLeftToRight();

        if (ltr) {
            positionComponentsLTR(parent, insets, gridDims, compDims, ncomponents);
        } else {
            positionComponentsRTL(parent, insets, gridDims, compDims, ncomponents);
        }
    }

    /**
     * Positions components for left-to-right orientation.
     */
    private void positionComponentsLTR(Container parent, Insets insets, GridDimensions gridDims,
          ComponentDimensions compDims, int ncomponents) {
        int x = insets.left + compDims.extraWidthAvailable;

        for (int c = 0; c < gridDims.ncols; c++) {
            int y = insets.top + compDims.extraHeightAvailable;

            for (int r = 0; r < gridDims.nrows; r++) {
                final int componentIndex = c * gridDims.nrows + r;
                if (componentIndex < ncomponents) {
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
          ComponentDimensions compDims, int ncomponents) {
        int x = parent.getWidth() - insets.right - compDims.width - compDims.extraWidthAvailable;

        for (int c = 0; c < gridDims.ncols; c++) {
            int y = insets.top + compDims.extraHeightAvailable;

            for (int r = 0; r < gridDims.nrows; r++) {
                final int componentIndex = c * gridDims.nrows + r;
                if (componentIndex < ncomponents) {
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
    private static final class GridDimensions {
        final int nrows;
        final int ncols;

        GridDimensions(int nrows, int ncols) {
            this.nrows = nrows;
            this.ncols = ncols;
        }
    }

    /**
     * Immutable record to hold component dimensions and positioning.
     */
    private static final class ComponentDimensions {
        final int width;
        final int height;
        final int extraWidthAvailable;
        final int extraHeightAvailable;

        ComponentDimensions(int width, int height, int extraWidthAvailable, int extraHeightAvailable) {
            this.width = width;
            this.height = height;
            this.extraWidthAvailable = extraWidthAvailable;
            this.extraHeightAvailable = extraHeightAvailable;
        }
    }
}
