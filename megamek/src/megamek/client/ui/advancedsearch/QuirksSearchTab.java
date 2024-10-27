/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.common.options.*;

import javax.swing.*;
import java.awt.*;

class QuirksSearchTab extends JPanel {

    private final JButton btnQuirksClear = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));
    final JComboBox<String> cQuirkInclude = new JComboBox<>();
    final JComboBox<String> cQuirkExclude = new JComboBox<>();
    final JComboBox<String> cWeaponQuirkInclude = new JComboBox<>();
    final JComboBox<String> cWeaponQuirkExclude = new JComboBox<>();
    TriStateItemList listQuirkType;
    TriStateItemList listWeaponQuirkType;

    QuirksSearchTab() {
        btnQuirksClear.addActionListener(e -> clear());

        loadAndOr(cQuirkInclude, 0);
        loadAndOr(cQuirkExclude, 1);
        listQuirkType = new TriStateItemList(new Quirks(), 25);

        loadAndOr(cWeaponQuirkInclude, 0);
        loadAndOr(cWeaponQuirkExclude, 1);
        cWeaponQuirkExclude.setSelectedIndex(1);

        listWeaponQuirkType = new TriStateItemList(new WeaponQuirks(), 17);

        JPanel unitQuirksPanel = new JPanel(new BorderLayout());
        JPanel quirkIEPanel = new JPanel();
        quirkIEPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Quirk")));
        quirkIEPanel.add(Box.createHorizontalStrut(15));
        quirkIEPanel.add(new JLabel("\u2611"));
        quirkIEPanel.add(cQuirkInclude);
        quirkIEPanel.add(new JLabel("\u2612"));
        quirkIEPanel.add(cQuirkExclude);
        unitQuirksPanel.add(quirkIEPanel, BorderLayout.NORTH);
        unitQuirksPanel.add(new JScrollPane(listQuirkType.getComponent()), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(btnQuirksClear);
        unitQuirksPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel weaponQuirkPanel = new JPanel(new BorderLayout());
        JPanel weaponQuirkIEPanel = new JPanel();
        weaponQuirkIEPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.WeaponQuirk")));
        weaponQuirkIEPanel.add(Box.createHorizontalStrut(15));
        weaponQuirkIEPanel.add(new JLabel("\u2611"));
        weaponQuirkIEPanel.add(cWeaponQuirkInclude);
        weaponQuirkIEPanel.add(new JLabel("\u2612"));
        weaponQuirkIEPanel.add(cWeaponQuirkExclude);
        weaponQuirkPanel.add(weaponQuirkIEPanel, BorderLayout.NORTH);
        weaponQuirkPanel.add(new JScrollPane(listWeaponQuirkType.getComponent()), BorderLayout.CENTER);

        JPanel innerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        innerPanel.add(unitQuirksPanel);
        innerPanel.add(weaponQuirkPanel);
        add(innerPanel);
    }

    private void loadAndOr(JComboBox<String> cb, int index) {
        cb.addItem(Messages.getString("MekSelectorDialog.Search.and"));
        cb.addItem(Messages.getString("MekSelectorDialog.Search.or"));
        cb.setSelectedIndex(index);
    }

    void clear() {
        cQuirkInclude.setSelectedIndex(0);
        cQuirkExclude.setSelectedIndex(1);
        listQuirkType.clear();

        cWeaponQuirkInclude.setSelectedIndex(0);
        cWeaponQuirkExclude.setSelectedIndex(1);
        listWeaponQuirkType.clear();
    }
}
