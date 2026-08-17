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
 * A compact, vertically stacked form panel for settings pages. All add methods share one row counter, so rows and
 * grids can be interleaved while preserving call order.
 *
 * <p>Choose an add method according to what one visual cell represents:</p>
 * <ul>
 *   <li>{@link #addCheckBox(JCheckBox)} for one standalone checkbox.</li>
 *   <li>{@link #addCheckBoxGrid(int, JCheckBox...)} for several checkboxes arranged across each row.</li>
 *   <li>{@link #addRow(JComponent, JComponent)} for one {@code label | control} setting.</li>
 *   <li>{@link #addRowGrid(int, JComponent...)} for several {@code label | control} settings per row.</li>
 *   <li>{@link #addComponentGrid(int, JComponent...)} for arbitrary cells that keep their natural widths.</li>
 *   <li>{@link #addEqualWidthComponentGrid(int, JComponent...)} when every cell is a complete setting or composite
 *       panel and all cells must have the same width.</li>
 *   <li>{@link #addFullWidthComponent(JComponent)} for content that must span the complete form.</li>
 * </ul>
 *
 * <p>The common two-column layouts are:</p>
 * <pre>{@code
 * addCheckBoxGrid(2):            [checkbox + text] | [checkbox + text]
 * addRow:                        label             | control
 * addRowGrid(2):                 label | control   | label | control
 * addEqualWidthComponentGrid(2): [complete option] | [complete option]
 * addFullWidthComponent:         [             full-width content             ]
 * }</pre>
 *
 * <p>A Swing {@link JCheckBox} already contains its box and text; it is one component. A composite option panel can
 * similarly contain a label and control while remaining one component from this form's perspective. Prefer
 * {@code addRow} or {@code addRowGrid} for separately constructed labels and controls because those methods associate
 * {@link JLabel labels} with their controls for accessibility.</p>
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

    /** Removes all form content and resets row placement for rebuilding the form. */
    public void clear() {
        removeAll();
        row = 0;
    }

    /**
     * Adds one left-aligned checkbox on its own row, spanning the form's two base columns.
     *
     * <p>Use this for an isolated boolean setting. For consecutive boolean settings, prefer
     * {@link #addCheckBoxGrid(int, JCheckBox...)}.</p>
     *
     * @param checkBox checkbox whose text serves as its visible label
     */
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

    /**
     * Adds one component that spans both base columns and stretches horizontally.
     *
     * <p>Use this for controls that genuinely need the section width, such as sliders, lists, warnings, custom panels,
     * or drag-and-drop content. Do not use it merely to place a normal checkbox or label/control setting.</p>
     *
     * @param component content that should occupy the full form width
     */
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

    /**
     * Adds arbitrary components in row-major order while retaining each component's natural preferred width.
     *
     * <p>Use this for mixed controls whose cells should not be forced to the same width, such as a checkbox paired with
     * a color button. With one column, each component becomes a full-width row. When a label width is configured, the
     * first column receives that minimum width, but the remaining columns retain their natural widths.</p>
     *
     * @param columnCount number of components per row
     * @param components  components to place in row-major order
     *
     * @see #addEqualWidthComponentGrid(int, JComponent...)
     */
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

    /**
     * Adds arbitrary components in row-major order after assigning every component the same preferred width.
     *
     * <p>Use this when each component is one complete setting or composite option panel and the visual columns must be
     * equal. A configured label width becomes the shared preferred cell width, allowing grids in separate sections to
     * align without a long option widening only its own section. Without a configured label width, the widest component
     * determines the shared width. Every column receives equal layout weight and divides any extra available width.
     * This method changes the supplied components' preferred widths. Callers must wrap or otherwise adapt content that
     * is naturally wider than one configured cell. For separate label/control pairs, use
     * {@link #addRowGrid(int, JComponent...)}.</p>
     *
     * @param columnCount number of equal-width components per row
     * @param components  complete settings or composite panels to place in row-major order
     */
    public void addEqualWidthComponentGrid(int columnCount, JComponent... components) {
        if (columnCount <= 1) {
            addComponentGrid(columnCount, components);
            return;
        }

        int cellWidth = setUniformPreferredWidth(components);
        int rowCount = (components.length + columnCount - 1) / columnCount;
        int firstRow = row;
        for (int index = 0; index < rowCount * columnCount; index++) {
            int column = index % columnCount;
            JComponent component = index < components.length
                  ? components[index]
                  : equalWidthFiller(cellWidth);
            GridBagConstraints layout = gridCellConstraints(firstRow + index / columnCount, column, columnCount);
            layout.weightx = 1.0;
            layout.fill = GridBagConstraints.HORIZONTAL;
            add(component, layout);
        }
        row += rowCount;
    }

    private int setUniformPreferredWidth(JComponent... components) {
        int width = UIUtil.scaleForGUI(labelWidth);
        if (width == 0) {
            for (JComponent component : components) {
                width = Math.max(width, component.getPreferredSize().width);
            }
        }
        for (JComponent component : components) {
            Dimension preferredSize = component.getPreferredSize();
            component.setPreferredSize(new Dimension(width, preferredSize.height));
        }
        return width;
    }

    private JPanel equalWidthFiller(int width) {
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        filler.setPreferredSize(new Dimension(width, 0));
        return filler;
    }

    /**
     * Adds left-aligned checkboxes in row-major order.
     *
     * <p>Use this for a homogeneous group of boolean settings. Each checkbox, including its text, is one cell. This
     * method applies checkbox-specific alignment but does not force every checkbox to the same preferred width; use
     * {@link #addEqualWidthComponentGrid(int, JComponent...)} for equal-width composite option cells.</p>
     *
     * @param columnCount number of checkboxes per row
     * @param checkBoxes  checkboxes to place in row-major order
     */
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

    /**
     * Adds one setting as a label in the first base column and a horizontally stretched control in the second.
     *
     * <pre>{@code
     * label | control
     * }</pre>
     *
     * <p>Use this for one conventional form field. If {@code label} is a {@link JLabel}, it is associated with the
     * control through {@link JLabel#setLabelFor(java.awt.Component)}. Use {@link #addRowGrid(int, JComponent...)} when
     * several label/control settings should share one visual row.</p>
     *
     * @param label   visible label or other label component
     * @param control component edited or selected by the user
     */
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
     * Adds alternating label/control components with several complete pairs per visual row.
     *
     * <pre>{@code
     * addRowGrid(2, label1, control1, label2, control2):
     * label1 | control1 | label2 | control2
     * }</pre>
     *
     * <p>Use this for manually constructed fields that need aligned labels, separate control widths, and label/control
     * accessibility associations. This differs from {@link #addEqualWidthComponentGrid(int, JComponent...)}: one
     * setting here consumes two internal grid columns, while one equal-width component-grid cell is already a complete
     * setting. An unmatched final component is ignored.</p>
     *
     * @param pairsPerRow     number of complete label/control settings per visual row
     * @param labelsAndControls alternating label, control, label, control components
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
