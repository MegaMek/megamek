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
package megamek.client.ui.dialogs.advancedsearch;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.formdev.flatlaf.FlatClientProperties;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ButtonEsc;
import megamek.client.ui.buttons.DialogButton;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;

/**
 * This is the dialog for advanced unit filtering, mostly for the unit selector. It contains the TW advanced search in
 * one tab and the AlphaStrike search in another tab. Both searches can be used simultaneously.
 */
public class AdvancedSearchDialog extends AbstractButtonDialog {

    private final int year;
    private final TWAdvancedSearchPanel totalWarTab;
    private final ASAdvancedSearchPanel alphaStrikeTab = new ASAdvancedSearchPanel();
    private final JTabbedPane advancedSearchPane = new JTabbedPane();

    public AdvancedSearchDialog(JFrame parent, int allowedYear) {
        super(parent, true, "AdvancedSearchDialog", "AdvancedSearchDialog.title");
        year = allowedYear;
        totalWarTab = new TWAdvancedSearchPanel(year);
        advancedSearchPane.addTab("Total Warfare", totalWarTab);
        advancedSearchPane.addTab("Alpha Strike", new TWAdvancedSearchPanel.StandardScrollPane(alphaStrikeTab));
        initialize();
    }

    @Override
    public void setVisible(boolean b) {
        alphaStrikeTab.saveValues();
        super.setVisible(b);
    }

    @Override
    protected void okAction() {
        super.okAction();
        totalWarTab.prepareFilter();
    }

    @Override
    protected void cancelAction() {
        alphaStrikeTab.resetValues();
        super.cancelAction();
    }

    @Override
    protected JPanel createButtonPanel() {
        JButton cancelButton = new ButtonEsc(new CloseAction(this));
        JButton okButton = new DialogButton(Messages.getString("Ok.text"));
        okButton.addActionListener(this::okButtonActionPerformed);
        getRootPane().setDefaultButton(okButton);

        JPanel notePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notePanel.add(Box.createHorizontalStrut(20));
        var noteLabel = new JLabel(Messages.getString("MekSelectorDialog.Search.Combine"));
        noteLabel.putClientProperty(FlatClientProperties.STYLE, "foreground: mix($Label.foreground, #afa, 60%)");
        notePanel.add(noteLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel outerPanel = new JPanel(new GridLayout(1, 1));
        outerPanel.setBorder(BorderFactory.createCompoundBorder(
              new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
              new EmptyBorder(10, 0, 10, 0)));
        outerPanel.add(notePanel);
        outerPanel.add(buttonPanel);

        return outerPanel;
    }

    @Override
    protected Container createCenterPane() {
        return advancedSearchPane;
    }

    /** Deactivates the search fields in both search tabs so that no units are filtered out. */
    public void clearSearches() {
        totalWarTab.clearValues();
        alphaStrikeTab.clearValues();
    }

    public ASAdvancedSearchPanel getASAdvancedSearch() {
        return alphaStrikeTab;
    }

    public TWAdvancedSearchPanel getTWAdvancedSearch() {
        return totalWarTab;
    }
}
