/*
 * Copyright (c) 2022-2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.advancedsearch;

import com.formdev.flatlaf.FlatClientProperties;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.ButtonEsc;
import megamek.client.ui.swing.CloseAction;
import megamek.client.ui.swing.dialog.DialogButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * This is the dialog for advanced unit filtering, mostly for the unit selector. It contains the TW advanced search in one tab and the
 * AlphaStrike search in another tab. Both searches can be used simultaneously.
 */
public class AdvancedSearchDialog2 extends AbstractButtonDialog {

    private final int year;
    private final TWAdvancedSearchPanel totalWarTab;
    private final ASAdvancedSearchPanel alphaStrikeTab = new ASAdvancedSearchPanel();
    private final JTabbedPane advancedSearchPane = new JTabbedPane();

    public AdvancedSearchDialog2(JFrame parent, int allowedYear) {
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

        JPanel outerPanel = new JPanel(new GridLayout(1,1));
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
