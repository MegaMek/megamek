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
package megamek.client.ui.dialogs.advancedsearch;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.Messages;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;

class QuirksSearchTab extends JPanel {

    final JComboBox<String> cQuirkInclude = new JComboBox<>();
    final JComboBox<String> cQuirkExclude = new JComboBox<>();
    final JComboBox<String> cWeaponQuirkInclude = new JComboBox<>();
    final JComboBox<String> cWeaponQuirkExclude = new JComboBox<>();
    TriStateItemList listQuirkType;
    TriStateItemList listWeaponQuirkType;

    QuirksSearchTab() {
        JButton btnQuirksClear = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));
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
