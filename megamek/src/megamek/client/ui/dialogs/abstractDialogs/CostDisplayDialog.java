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
package megamek.client.ui.dialogs.abstractDialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.clientGUI.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.util.UIUtil;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

/** A dialog to display the cost calculation for a given entity. */
public class CostDisplayDialog extends AbstractDialog {

    private final Entity entity;
    private final JToggleButton dryCostToggle = new JToggleButton("Include Ammo");
    private FlexibleCalculationReport costReport;
    private final JScrollPane reportScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    /**
     * Creates a non-modal dialog to display the cost breakdown for the given entity. A null entity can safely be
     * passed.
     *
     * @param frame  The parent frame of this dialog
     * @param entity The unit to display the cost calculation for
     */
    public CostDisplayDialog(final JFrame frame, final @Nullable Entity entity) {
        this(frame, false, entity);
    }

    /**
     * Creates a dialog to display the cost breakdown for the given entity. A null entity can safely be passed.
     *
     * @param frame  The parent frame of this dialog
     * @param modal  When true, will make this dialog modal
     * @param entity The unit to display the cost calculation for
     */
    public CostDisplayDialog(final JFrame frame, final boolean modal, final @Nullable Entity entity) {
        super(frame, modal, "CostDisplayDialog", "CostDisplayDialog.title");
        this.entity = entity;
        initialize();
    }

    @Override
    protected void finalizeInitialization() throws Exception {
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
        centerPanel.add(new JSeparator());
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
        pack();
        Dimension screenSize = UIUtil.getScaledScreenSize(this);
        setSize(new Dimension(getSize().width, Math.min(getHeight(), (int) (screenSize.getHeight() * 0.8))));
    }

    private void copyToClipboard(String reportString) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(reportString), null);
    }

    @Override
    protected void cancelAction() {
        dispose();
    }
}
