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
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.CompositeTechLevelReport;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Faction;
import megamek.common.units.Entity;

/**
 * A dialog that displays how a unit's composite tech level is put together: the tech level of every component the unit
 * is built from, both with and without the Variable Tech Level rule, and the running composite the components produce
 * as they are folded in.
 */
public class TechLevelDisplayDialog extends AbstractDialog {

    /**
     * The smallest height the dialog will size itself to. Guards against a zero or near-zero screen height (as can be
     * reported during display changes) collapsing the dialog into a zero-height window.
     */
    private static final int MINIMUM_DIALOG_HEIGHT = 200;

    private final Entity entity;
    private final Faction techFaction;
    private final int evaluationYear;
    private final boolean useVariableTechLevel;

    private final JScrollPane reportScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    /**
     * Creates a non-modal dialog showing the composite tech level breakdown for the given unit, evaluated in the unit's
     * own introduction year with no faction filtering and the Variable Tech Level rule in use. A {@code null} entity
     * can safely be passed.
     *
     * @param frame  The parent frame of this dialog
     * @param entity The unit to display the tech level breakdown for
     */
    public TechLevelDisplayDialog(final JFrame frame, final @Nullable Entity entity) {
        this(frame, entity, Faction.NONE, (entity == null) ? 0 : entity.getYear(), true);
    }

    /**
     * Creates a non-modal dialog showing the composite tech level breakdown for the given unit. A {@code null} entity
     * can safely be passed.
     *
     * @param frame                The parent frame of this dialog
     * @param entity               The unit to display the tech level breakdown for
     * @param techFaction          The faction to evaluate faction-specific dates for
     * @param evaluationYear       The year to evaluate the variable tech level in
     * @param useVariableTechLevel {@code true} when the Variable Tech Level rule is in use, which is then reported as
     *                             the unit's effective tech level; {@code false} to treat the static tech level as the
     *                             effective one
     */
    public TechLevelDisplayDialog(final JFrame frame, final @Nullable Entity entity, final Faction techFaction,
          final int evaluationYear, final boolean useVariableTechLevel) {
        super(frame, false, "TechLevelDisplayDialog", "TechLevelDisplayDialog.title");
        this.entity = entity;
        this.techFaction = techFaction;
        this.evaluationYear = evaluationYear;
        this.useVariableTechLevel = useVariableTechLevel;
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
        JButton exportText = new JButton(Messages.getString("TechLevelDisplayDialog.copyAsText"));
        exportText.addActionListener(event -> copyToClipboard(plainTextReport()));
        JButton exportHTML = new JButton(Messages.getString("TechLevelDisplayDialog.copyAsHTML"));
        exportHTML.addActionListener(event -> copyToClipboard(htmlReport()));

        reportScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        reportScrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));
        buildReport();

        Box centerPanel = Box.createVerticalBox();
        centerPanel.setBorder(new EmptyBorder(25, 15, 25, 15));
        JPanel buttonPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(exportText);
        buttonPanel.add(exportHTML);
        centerPanel.add(buttonPanel);
        centerPanel.add(new JSeparator());
        centerPanel.add(reportScrollPane);
        return centerPanel;
    }

    private void buildReport() {
        if (entity == null) {
            reportScrollPane.setViewportView(new JLabel(Messages.getString("TechLevelDisplayDialog.noUnit")));
            return;
        }

        JTextPane reportPane = new JTextPane();
        // Gives the pane the report font and the themed colour classes used elsewhere in the suite, so the
        // report follows the user's light or dark theme instead of rendering as black text on white.
        Report.setupStylesheet(reportPane);
        reportPane.setEditable(false);
        reportPane.setText(htmlReport());
        reportPane.setCaretPosition(0);

        reportScrollPane.setViewportView(reportPane);
        updateDialogSize();
    }

    private String htmlReport() {
        return CompositeTechLevelReport.toHtml(entity, techFaction, evaluationYear, useVariableTechLevel);
    }

    private String plainTextReport() {
        return CompositeTechLevelReport.toPlainText(entity, techFaction, evaluationYear, useVariableTechLevel);
    }

    /** Does gui-scaling, packs the dialog and reduces the height if it's too big. */
    private void updateDialogSize() {
        pack();
        Dimension screenSize = UIUtil.getScaledScreenSize(this);
        int maximumHeight = Math.max(MINIMUM_DIALOG_HEIGHT, (int) (screenSize.getHeight() * 0.8));
        setSize(new Dimension(getSize().width, Math.min(getHeight(), maximumHeight)));
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
