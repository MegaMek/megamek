/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.Objects;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import megamek.client.ui.clientGUI.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.util.UIUtil;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestInfantry;

public class WeightDisplayDialog extends AbstractDialog {

    private final Entity entity;

    public WeightDisplayDialog(final JFrame frame, final Entity entity) {
        this(frame, false, entity);
    }

    public WeightDisplayDialog(final JFrame frame, final boolean modal, final Entity entity) {
        super(frame, modal, "BVDisplayDialog", "BVDisplayDialog.title");
        this.entity = Objects.requireNonNull(entity);
        initialize();
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        setTitle(getTitle() + " (" + entity.getShortName() + ")");
        pack();
        Dimension screenSize = UIUtil.getScaledScreenSize(this);
        setSize(new Dimension(getSize().width, Math.min(getHeight(), (int) (screenSize.getHeight() * 0.8))));
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    protected Container createCenterPane() {
        var scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        String textReport;
        if (entity.isConventionalInfantry()) {
            FlexibleCalculationReport weightReport = new FlexibleCalculationReport();
            TestInfantry.getWeightExact((Infantry) entity, weightReport);
            scrollPane.setViewportView(weightReport.toJComponent());
            textReport = weightReport.getTextReport().toString();
        } else {
            TestEntity testEntity = TestEntity.getEntityVerifier(entity);
            if (testEntity != null) {
                textReport = testEntity.printEntity().toString();
            } else {
                textReport = "This report is currently not implemented.";
            }
            JTextPane textPane = new JTextPane();
            textPane.setText(textReport);
            textPane.setEditable(false);
            textPane.setCaret(new DefaultCaret());
            scrollPane.setViewportView(textPane);
        }

        JButton exportText = new JButton("Copy as Text");
        exportText.addActionListener(evt -> copyToClipboard(textReport));

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));

        Box centerPanel = Box.createVerticalBox();
        centerPanel.setBorder(new EmptyBorder(25, 15, 25, 15));
        JPanel buttonPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(exportText);
        centerPanel.add(buttonPanel);
        centerPanel.add(scrollPane);
        return centerPanel;
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
