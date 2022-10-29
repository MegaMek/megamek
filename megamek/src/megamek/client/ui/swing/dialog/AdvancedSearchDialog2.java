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
package megamek.client.ui.swing.dialog;

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.unitSelector.ASAdvancedSearchPanel;
import megamek.client.ui.swing.unitSelector.TWAdvancedSearchPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * This is a (new) dialog for advanced unit filtering, mostly for the unit selector. It contains the old
 * TW advanced search in one tab and the new AlphaStrike search in another tab. Both searches can be used
 * simultaneously.
 */
public class AdvancedSearchDialog2 extends AbstractButtonDialog {

    final int year;
    private final TWAdvancedSearchPanel totalWarTab;
    private final ASAdvancedSearchPanel alphaStrikeTab = new ASAdvancedSearchPanel();
    private final JTabbedPane advancedSearchPane = new JTabbedPane();

    public AdvancedSearchDialog2(JFrame parent, int allowedYear) {
        super(parent, true, "AdvancedSearchDialog", "AdvancedSearchDialog.title");
        year = allowedYear;
        totalWarTab = new TWAdvancedSearchPanel(year);
        advancedSearchPane.addTab("Total Warfare", totalWarTab);
        advancedSearchPane.addTab("Alpha Strike", alphaStrikeTab);
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
        JPanel buttonPanel = super.createButtonPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
        return buttonPanel;
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
