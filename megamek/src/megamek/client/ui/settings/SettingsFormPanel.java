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
package megamek.client.ui.settings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;

/**
 * A compact, vertically stacked form panel for settings pages. All add methods share one row counter, allowing
 * labelled rows, full-width controls, and grids to be interleaved while preserving call order.
 */
public class SettingsFormPanel extends JPanel {
    public static final int DEFAULT_LABEL_WIDTH = 300;
    public static final int DEFAULT_CONTROL_WIDTH = 220;
    public static final int GRID_COLUMN_GAP = 32;

    private static final int LABEL_RIGHT_PADDING = 12;
    private static final int ROW_VERTICAL_PADDING = 5;

    private final int labelWidth;
    private final int controlWidth;
    private int row;

    public SettingsFormPanel(String name) {
        this(name, 0, DEFAULT_CONTROL_WIDTH);
    }

    public SettingsFormPanel(String name, int labelWidth) {
        this(name, labelWidth, DEFAULT_CONTROL_WIDTH);
    }

    public SettingsFormPanel(String name, int labelWidth, int controlWidth) {
        this.labelWidth = labelWidth;
        this.controlWidth = controlWidth;
        setName("pnl" + name);
        setOpaque(false);
        setLayout(new GridBagLayout());
    }

    /** Adds one left-aligned checkbox followed by a horizontal filler. */
    public void addCheckBox(JCheckBox checkBox) {
        alignCheckBoxToStart(checkBox);
        int currentRow = row++;

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = currentRow;
        layout.gridwidth = 2;
        layout.anchor = GridBagConstraints.WEST;
        layout.fill = GridBagConstraints.NONE;
        layout.insets = rowInsets(0);
        add(checkBox, layout);
        addTrailingFiller(currentRow, 2);
    }

    /** Adds a component that spans and fills both form columns. */
    public void addFullWidthComponent(JComponent component) {
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = row++;
        layout.gridwidth = 2;
        layout.weightx = 1.0;
        layout.anchor = GridBagConstraints.WEST;
        layout.fill = GridBagConstraints.HORIZONTAL;
        layout.insets = rowInsets(0);
        add(component, layout);
    }

    /** Adds components in row-major order using {@code columnCount} columns. */
    public void addComponentGrid(int columnCount, JComponent... components) {
        if (columnCount <= 1) {
            for (JComponent component : components) {
                addFullWidthComponent(component);
            }
            return;
        }

        int firstRow = row;
        for (int index = 0; index < components.length; index++) {
            int column = index % columnCount;
            GridBagConstraints layout = gridCellConstraints(firstRow + index / columnCount, column, columnCount);
            setMinimumFirstColumnWidth(components[index], column);
            add(components[index], layout);
        }
        finishGrid(firstRow, components.length, columnCount);
    }

    /** Adds left-aligned checkboxes in row-major order using {@code columnCount} columns. */
    public void addCheckBoxGrid(int columnCount, JCheckBox... checkBoxes) {
        if (columnCount <= 1) {
            for (JCheckBox checkBox : checkBoxes) {
                addCheckBox(checkBox);
            }
            return;
        }

        int firstRow = row;
        for (int index = 0; index < checkBoxes.length; index++) {
            int column = index % columnCount;
            alignCheckBoxToStart(checkBoxes[index]);
            GridBagConstraints layout = gridCellConstraints(firstRow + index / columnCount, column, columnCount);
            setMinimumFirstColumnWidth(checkBoxes[index], column);
            add(checkBoxes[index], layout);
        }
        finishGrid(firstRow, checkBoxes.length, columnCount);
    }

    /** Adds a label and a horizontally stretched control. */
    public void addRow(JComponent label, JComponent control) {
        associateLabel(label, control);
        setMinimumLabelWidth(label);
        setMinimumControlWidth(control);
        int currentRow = row++;

        GridBagConstraints labelLayout = new GridBagConstraints();
        labelLayout.gridx = 0;
        labelLayout.gridy = currentRow;
        labelLayout.anchor = GridBagConstraints.WEST;
        labelLayout.fill = GridBagConstraints.NONE;
        labelLayout.insets = rowInsets(LABEL_RIGHT_PADDING);
        add(label, labelLayout);

        GridBagConstraints controlLayout = new GridBagConstraints();
        controlLayout.gridx = 1;
        controlLayout.gridy = currentRow;
        controlLayout.gridwidth = GridBagConstraints.REMAINDER;
        controlLayout.weightx = 1.0;
        controlLayout.anchor = GridBagConstraints.WEST;
        controlLayout.fill = GridBagConstraints.HORIZONTAL;
        controlLayout.insets = rowInsets(0);
        add(control, controlLayout);
    }

    /**
     * Adds alternating label/control components as a grid of {@code pairsPerRow} pairs per row. An unmatched final
     * component is ignored.
     */
    public void addRowGrid(int pairsPerRow, JComponent... labelsAndControls) {
        if (pairsPerRow <= 1) {
            for (int index = 0; index + 1 < labelsAndControls.length; index += 2) {
                addRow(labelsAndControls[index], labelsAndControls[index + 1]);
            }
            return;
        }

        int pairCount = labelsAndControls.length / 2;
        int firstRow = row;
        for (int pairIndex = 0; pairIndex < pairCount; pairIndex++) {
            int columnPair = pairIndex % pairsPerRow;
            int gridRow = firstRow + pairIndex / pairsPerRow;
            JComponent label = labelsAndControls[pairIndex * 2];
            JComponent control = labelsAndControls[pairIndex * 2 + 1];
            associateLabel(label, control);
            setMinimumLabelWidth(label);
            setMinimumControlWidth(control);

            GridBagConstraints labelLayout = new GridBagConstraints();
            labelLayout.gridx = columnPair * 2;
            labelLayout.gridy = gridRow;
            labelLayout.anchor = GridBagConstraints.WEST;
            labelLayout.fill = GridBagConstraints.NONE;
            labelLayout.insets = rowInsets(LABEL_RIGHT_PADDING);
            add(label, labelLayout);

            boolean lastColumn = columnPair == pairsPerRow - 1;
            GridBagConstraints controlLayout = new GridBagConstraints();
            controlLayout.gridx = columnPair * 2 + 1;
            controlLayout.gridy = gridRow;
            controlLayout.anchor = GridBagConstraints.WEST;
            controlLayout.insets = rowInsets(lastColumn ? 0 : GRID_COLUMN_GAP);
            if (lastColumn) {
                controlLayout.gridwidth = GridBagConstraints.REMAINDER;
                controlLayout.weightx = 1.0;
                controlLayout.fill = GridBagConstraints.HORIZONTAL;
            } else {
                controlLayout.fill = GridBagConstraints.NONE;
            }
            add(control, controlLayout);
        }
        row += (pairCount + pairsPerRow - 1) / pairsPerRow;
    }

    private GridBagConstraints gridCellConstraints(int gridRow, int column, int columnCount) {
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = column;
        layout.gridy = gridRow;
        layout.anchor = GridBagConstraints.WEST;
        layout.fill = GridBagConstraints.NONE;
        layout.insets = rowInsets(getGridColumnRightPadding(column, columnCount));
        return layout;
    }

    private void finishGrid(int firstRow, int componentCount, int columnCount) {
        int rowCount = (componentCount + columnCount - 1) / columnCount;
        for (int rowOffset = 0; rowOffset < rowCount; rowOffset++) {
            addTrailingFiller(firstRow + rowOffset, columnCount);
        }
        row += rowCount;
    }

    private int getGridColumnRightPadding(int column, int columnCount) {
        if (column == columnCount - 1) {
            return 0;
        }
        return labelWidth > 0 ? LABEL_RIGHT_PADDING : GRID_COLUMN_GAP;
    }

    private void setMinimumFirstColumnWidth(JComponent component, int column) {
        if (labelWidth > 0 && column == 0) {
            setMinimumWidth(component, labelWidth);
        }
    }

    private void alignCheckBoxToStart(JCheckBox checkBox) {
        checkBox.setHorizontalAlignment(SwingConstants.LEADING);
    }

    private void addTrailingFiller(int rowIndex, int columnIndex) {
        JPanel filler = new JPanel();
        filler.setOpaque(false);

        GridBagConstraints fillerLayout = new GridBagConstraints();
        fillerLayout.gridx = columnIndex;
        fillerLayout.gridy = rowIndex;
        fillerLayout.weightx = 1.0;
        fillerLayout.fill = GridBagConstraints.HORIZONTAL;
        add(filler, fillerLayout);
    }

    private void setMinimumControlWidth(JComponent control) {
        setMinimumWidth(control, controlWidth);
    }

    private static void associateLabel(JComponent label, JComponent control) {
        if (label instanceof JLabel swingLabel) {
            swingLabel.setLabelFor(control);
        }
    }

    private void setMinimumLabelWidth(JComponent label) {
        if (labelWidth > 0) {
            setMinimumWidth(label, labelWidth);
        }
    }

    private void setMinimumWidth(JComponent component, int minimumWidth) {
        Dimension preferredSize = component.getPreferredSize();
        int width = Math.max(preferredSize.width, UIUtil.scaleForGUI(minimumWidth));
        Dimension adjustedSize = new Dimension(width, preferredSize.height);
        component.setPreferredSize(adjustedSize);
        component.setMinimumSize(adjustedSize);
    }

    private static Insets rowInsets(int right) {
        return new Insets(UIUtil.scaleForGUI(ROW_VERTICAL_PADDING), 0,
              UIUtil.scaleForGUI(ROW_VERTICAL_PADDING), UIUtil.scaleForGUI(right));
    }
}
