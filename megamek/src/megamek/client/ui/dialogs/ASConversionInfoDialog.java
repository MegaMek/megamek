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
import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.util.UIUtil;
import megamek.codeUtilities.StringUtility;
import megamek.common.Entity;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

public class ASConversionInfoDialog extends AbstractDialog {

    private final CalculationReport report;
    private final String unitName;

    //TODO: Commenting

    public ASConversionInfoDialog(final JFrame frame, CalculationReport report, @Nullable ASCardDisplayable element) {
        this(frame, report, element.getChassis() + " " + element.getModel(), false);
    }

    public ASConversionInfoDialog(final JFrame frame, CalculationReport report, @Nullable AlphaStrikeElement element) {
        this(frame, report, element.getName(), false);
    }

    public ASConversionInfoDialog(final JFrame frame, CalculationReport report, @Nullable AlphaStrikeElement element, boolean modal) {
        this(frame, report, element.getName(), modal);
    }

    public ASConversionInfoDialog(final JFrame frame, CalculationReport report) {
        this(frame, report, "", false);
    }

    public ASConversionInfoDialog(final JFrame frame, CalculationReport report, @Nullable Entity entity) {
        this(frame, report, entity.getShortName(), false);
    }

    public ASConversionInfoDialog(final JFrame frame, CalculationReport report, @Nullable Entity entity, boolean modal) {
        this(frame, report, entity.getShortName(), modal);
    }

    private ASConversionInfoDialog(final JFrame frame, CalculationReport report, @Nullable String unitName, boolean modal) {
        super(frame, modal, "BVDisplayDialog", "BVDisplayDialog.title");
        this.report = Objects.requireNonNull(report);
        this.unitName = unitName;
        initialize();
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        adaptToGUIScale();
        if (!StringUtility.isNullOrBlank(unitName)) {
            setTitle(getTitle() + " (" + unitName + ")");
        }
        pack();
        Dimension screenSize = UIUtil.getScaledScreenSize(this);
        setSize(new Dimension(getSize().width, Math.min(getHeight(), (int) (screenSize.getHeight() * 0.8))));
    }

    @Override
    protected Container createCenterPane() {
        JButton exportText = new JButton("Copy as Text");
        JButton exportHTML = new JButton("Copy as HTML");

        if (report instanceof FlexibleCalculationReport) {
            FlexibleCalculationReport flexReport = (FlexibleCalculationReport) report;
            exportText.addActionListener(evt -> copyToClipboard(flexReport.getTextReport().toString()));
            exportHTML.addActionListener(evt -> copyToClipboard(flexReport.getHtmlReport().toString()));
        } else {
            exportText.setEnabled(false);
            exportHTML.setEnabled(false);
        }

        var scrollPane = new JScrollPane(report.toJComponent(), ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));

        Box centerPanel = Box.createVerticalBox();
        centerPanel.setBorder(new EmptyBorder(25, 15, 25, 15));
        JPanel buttonPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(exportText);
        buttonPanel.add(exportHTML);
        centerPanel.add(buttonPanel);
        centerPanel.add(scrollPane);
        return centerPanel;
    }

    private void copyToClipboard(String reportString) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(reportString), null);
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}

