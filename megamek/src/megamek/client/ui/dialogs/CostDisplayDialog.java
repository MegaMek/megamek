/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.dialogs;

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/** A dialog to display the cost calculation for a given entity. */
public class CostDisplayDialog extends AbstractDialog {

    private final Entity entity;
    private final JToggleButton dryCostToggle = new JToggleButton("Include Ammo");
    private FlexibleCalculationReport costReport;
    private final JScrollPane reportScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    /**
     * Creates a non-modal dialog to display the cost breakdown for the given entity. A null entity can
     * safely be passed.
     *
     * @param frame The parent frame of this dialog
     * @param entity The unit to display the cost calculation for
     */
    public CostDisplayDialog(final JFrame frame, final @Nullable Entity entity) {
        this(frame, false, entity);
    }

    /**
     * Creates a dialog to display the cost breakdown for the given entity. A null entity can
     * safely be passed.
     *
     * @param frame The parent frame of this dialog
     * @param modal When true, will make this dialog modal
     * @param entity The unit to display the cost calculation for
     */
    public CostDisplayDialog(final JFrame frame, final boolean modal, final @Nullable Entity entity) {
        super(frame, modal, "CostDisplayDialog", "CostDisplayDialog.title");
        this.entity = entity;
        initialize();
    }

    @Override
    protected void finalizeInitialization() {
        super.finalizeInitialization();
        if (entity != null) {
            setTitle(getTitle() + " (" + entity.getShortName() + ")");
        }
        updateDialogSize();
    }

    @Override
    protected Container createCenterPane() {
        JButton exportText = new JButton("Copy as Text");
        exportText.addActionListener(evt -> copyToClipboard(costReport.getTextReport().toString()));
        JButton exportHTML = new JButton("Copy as HTML");
        exportHTML.addActionListener(evt -> copyToClipboard(costReport.getHtmlReport().toString()));
        dryCostToggle.addActionListener(evt -> updateCalculation());

        reportScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        reportScrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));
        updateCalculation();

        Box centerPanel = Box.createVerticalBox();
        centerPanel.setBorder(new EmptyBorder(25, 15, 25, 15));
        JPanel buttonPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(dryCostToggle);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(exportText);
        buttonPanel.add(exportHTML);
        centerPanel.add(buttonPanel);
        centerPanel.add(reportScrollPane);
        return centerPanel;
    }

    private void updateCalculation() {
        if (entity != null) {
            costReport = new FlexibleCalculationReport();
            entity.getCost(costReport, !dryCostToggle.isSelected());
            // Use an inner panel to make the report top-left-aligned
            JPanel anchorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            anchorPanel.add(costReport.toJComponent());
            reportScrollPane.setViewportView(anchorPanel);
            updateDialogSize();
        } else {
            reportScrollPane.setViewportView(new JLabel("Error: Could not access the unit!"));
        }
    }

    /** Does gui-scaling, packs the dialog and reduces the height if its too big. */
    private void updateDialogSize() {
        UIUtil.adjustDialog(getContentPane());
        pack();
        Dimension screenSize = UIUtil.getScaledScreenSize(this);
        setSize(new Dimension(getSize().width, Math.min(getHeight(), (int) (screenSize.getHeight() * 0.8))));
    }

    private void copyToClipboard(String reportString) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(reportString), null);
    }
}
